package fi.harism.campaper;

import java.io.IOException;
import java.io.InputStream;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setTitle(R.string.app_name);
        actionBar.setBackgroundDrawable(new ColorDrawable(
                android.R.color.transparent));
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        InputStream is = null;
        try {
            is = openFileInput("depthmap");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            setContentView(R.layout.layout_main);
            ImageView iv = (ImageView)findViewById(R.id.imageview);
            iv.setImageBitmap(bitmap);
        } catch (Exception ex) {
            setContentView(R.layout.layout_main_empty);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_choose:
            startActivity(new Intent(this, ChooseActivity.class));
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
}
