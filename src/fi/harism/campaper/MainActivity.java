package fi.harism.campaper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.xmp.XmpDirectory;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action)
                && (type != null && type.startsWith("image/"))) {
            Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            ImageView iv = (ImageView) findViewById(R.id.image);
            //iv.setImageURI(uri);

            InputStream inputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(uri);

                Metadata metadata = ImageMetadataReader
                        .readMetadata(inputStream);

                Log.d("metadata", metadata.toString());

                for (Directory directory : metadata.getDirectories()) {
                    Log.d("directory", directory.getName());
                    for (Tag tag : directory.getTags()) {
                        Log.d(directory.getName(), tag.toString());
                    }
                }

                XmpDirectory xmpDirectory = metadata
                        .getDirectory(XmpDirectory.class);
                if (xmpDirectory != null) {
                    for (String error : xmpDirectory.getErrors()) {
                        Log.d("XmpError", error);
                    }
                    Map<String, String> xmpProperties = xmpDirectory
                            .getXmpProperties();
                    for (String key : xmpProperties.keySet()) {
                        Log.d("XmpProperty",
                                key + " = " + xmpProperties.get(key));
                    }
                    
                    XMPMeta xmpMeta = xmpDirectory.getXMPMeta();
                    byte image[] = xmpMeta.getPropertyBase64("http://ns.google.com/photos/1.0/image/", "GImage:Data");
                    iv.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
                    byte depth[] = xmpMeta.getPropertyBase64("http://ns.google.com/photos/1.0/depthmap/", "GDepth:Data");
                    iv.setImageBitmap(BitmapFactory.decodeByteArray(depth, 0, depth.length));
                }

                // XMPMeta xmpMeta = XMPMetaFactory.parse(inputStream);
                // Log.d("xmpMeta", xmpMeta.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ImageProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XMPException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
