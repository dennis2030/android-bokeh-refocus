package fi.harism.campaper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.adobe.xmp.XMPMeta;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.xmp.XmpDirectory;

import android.app.ActionBar;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ChooseActivity extends Activity {

    private ProgressBar mProgressBar;
    private View mNoImagesFoundView;
    private ViewPager mViewPager;
    private ChoosePagerAdapter mChoosePagerAdapter;
    private int mImageHashCode = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_choose);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setTitle(R.string.choose_photo);
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setBackgroundDrawable(new ColorDrawable(
                android.R.color.transparent));

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mNoImagesFoundView = findViewById(R.id.textview_empty_list);
        mChoosePagerAdapter = new ChoosePagerAdapter();
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(mChoosePagerAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        scanImages();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_choose, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.action_done:
            int imageIndex = mViewPager.getCurrentItem();
            if (imageIndex >= 0 && imageIndex < mChoosePagerAdapter.getCount()) {
                mProgressBar.setIndeterminate(true);
                new SaveAsyncTask()
                        .execute(mChoosePagerAdapter.get(imageIndex));
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveImage(String name, byte data[]) throws Exception {
        Exception ex = null;
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(name, MODE_PRIVATE);
            fos.write(data);
            fos.flush();
        } catch (Exception e) {
            ex = e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    private void scanImages() {
        final String[] projection = { MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_MODIFIED };
        final String selection = null;
        final String[] selectionArgs = null;
        final String orderBy = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
        final Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                selection, selectionArgs, orderBy);
        String result[] = null;
        int hashCode = 0;
        if (cursor.moveToFirst()) {
            final int dataColumn = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            result = new String[cursor.getCount()];
            int resultIndex = 0;
            do {
                final String data = cursor.getString(dataColumn);
                result[resultIndex++] = data;
                hashCode ^= data.hashCode();
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (result != null && hashCode != mImageHashCode) {
            mImageHashCode = hashCode;
            mViewPager.setVisibility(View.VISIBLE);
            mNoImagesFoundView.setVisibility(View.GONE);
            mProgressBar.setMax(result.length);
            mProgressBar.setProgress(0);
            mChoosePagerAdapter.clear();
            new CheckAsyncTask().execute(result);
        }
    }

    private class CheckAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            for (int index = 0; index < params.length; ++index) {
                if (checkImage(params[index])) {
                    publishProgress(params[index], String.valueOf(index + 1));
                } else {
                    publishProgress(null, String.valueOf(index + 1));
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            if (progress.length > 0 && progress[0] != null) {
                mChoosePagerAdapter.add(progress[0]);
            }
            if (progress.length > 1 && progress[1] != null) {
                mProgressBar.setProgress(Integer.valueOf(progress[1]));
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (mChoosePagerAdapter.getCount() == 0) {
                mViewPager.setVisibility(View.GONE);
                mNoImagesFoundView.setVisibility(View.VISIBLE);
            }
        }

        private boolean checkImage(String path) {
            boolean valid = false;
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(new File(
                        path));
                XmpDirectory xmpDirectory = metadata
                        .getDirectory(XmpDirectory.class);
                if (xmpDirectory != null) {
                    XMPMeta xmpMeta = xmpDirectory.getXMPMeta();
                    xmpMeta.getPropertyBase64(
                            "http://ns.google.com/photos/1.0/image/",
                            "GImage:Data");
                    xmpMeta.getPropertyBase64(
                            "http://ns.google.com/photos/1.0/depthmap/",
                            "GDepth:Data");
                    valid = true;
                }
            } catch (Exception e) {
            }
            return valid;
        }

    }

    private class SaveAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                mProgressBar.setIndeterminate(true);
                Metadata metadata = ImageMetadataReader.readMetadata(new File(
                        params[0]));
                XmpDirectory xmpDirectory = metadata
                        .getDirectory(XmpDirectory.class);
                if (xmpDirectory != null) {
                    XMPMeta xmpMeta = xmpDirectory.getXMPMeta();
                    byte image[] = xmpMeta.getPropertyBase64(
                            "http://ns.google.com/photos/1.0/image/",
                            "GImage:Data");
                    byte depthmap[] = xmpMeta.getPropertyBase64(
                            "http://ns.google.com/photos/1.0/depthmap/",
                            "GDepth:Data");

                    saveImage("image", image);
                    saveImage("depthmap", depthmap);
                }
            } catch (Exception e) {
                return e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(ChooseActivity.this, result, Toast.LENGTH_LONG)
                        .show();
                mProgressBar.setIndeterminate(false);
            } else {
                finish();
            }
        }

    }

    private class ChoosePagerAdapter extends PagerAdapter {

        private final ArrayList<String> mImages = new ArrayList<String>();

        public void clear() {
            mImages.clear();
            notifyDataSetChanged();
        }

        public void add(String path) {
            mImages.add(path);
            notifyDataSetChanged();
        }

        public String get(int index) {
            return mImages.get(index);
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(View owner, int position) {
            return instantiateItem((ViewGroup) owner, position);
        }

        @Override
        public Object instantiateItem(ViewGroup owner, int position) {
            View view = getLayoutInflater().inflate(R.layout.choose_page, null);
            ImageView imageView = (ImageView) view.findViewById(R.id.imageview);
            imageView.setImageBitmap(BitmapFactory.decodeFile(mImages
                    .get(position)));
            owner.addView(view);
            return view;
        }

        @Override
        public void destroyItem(View owner, int position, Object object) {
            destroyItem((ViewGroup) owner, position, object);
        }

        @Override
        public void destroyItem(ViewGroup owner, int position, Object object) {
            owner.removeView((View) object);
        }

    }

}
