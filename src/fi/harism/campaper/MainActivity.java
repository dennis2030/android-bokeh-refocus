package fi.harism.campaper;

import java.io.IOException;
import java.io.InputStream;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.widget.ImageView;

public class MainActivity extends Activity {

	private double drawLeft = 0;
	private double drawTop = 0;
	private double drawWidth = 0;
	private double drawHeight = 0;
	
	private Bitmap depth_bitmap = null;
	private Bitmap image_bitmap = null;
	
	int[] depthPixels = null;
	
	BokehFilter mBokeh;
	
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
    
    protected double[] calcCoC(int[] inputPixels, int z_focus ,int width, int height)
    {
    	double[] CoC = new double[width*height];
    	double s = 3.0;
    	
    	for(int i=0;i<inputPixels.length;i++)
    	{
    		int depth = convertToRGB(inputPixels[i])[0];
    		CoC[i] = s * Math.abs(1-z_focus/ depth);
    		
    	}
    	
    	return CoC;
    }
    protected int[] convertToRGB(int input)
    {
    	int[] rgb = new int[3];
		int R = (input >> 16) & 0xff;     //bitwise shifting
        int G = (input >> 8) & 0xff;
        int B = input & 0xff;
        rgb[0] = R;
        rgb[1] = G;
        rgb[2] = B;
        return rgb;

    }
    /*
    protected Color[][] getRGBValues(int[] pixels, int width, int height)
    {
    	Color[][] rgb = new Color[width][height];
    	for(int y=0;y<height;y++)
    	{
    		for(int x=0;x<width;x++)
    		{
    			int index = y*width + x;
    			int p = pixels[index];
    			int R = (p >> 16) & 0xff;     //bitwise shifting
                int G = (p>> 8) & 0xff;
                int B = p & 0xff;
                rgb[x][y] = new Color(R,G,B);
    		}
    	}
    	return rgb;
    }*/
          
    
    @Override
    protected void onStart() {
        super.onStart();
        
        InputStream is = null;
        try {
            is = openFileInput("depthmap");
            depth_bitmap = BitmapFactory.decodeStream(is);
            is = openFileInput("image");
            image_bitmap = BitmapFactory.decodeStream(is);                       
         
            // copy pixels of depth image into depthPixels
            depthPixels = new int[depth_bitmap.getWidth() * depth_bitmap.getHeight()];
            depth_bitmap.getPixels(depthPixels, 0, depth_bitmap.getWidth(), 0, 0, depth_bitmap.getWidth(), depth_bitmap.getHeight());
            
            setContentView(R.layout.layout_main);
            ImageView iv = (ImageView)findViewById(R.id.imageview);
            iv.setImageBitmap(depth_bitmap);                                                
         
            
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
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        	if(depth_bitmap == null) {
        		return;
        	}
        	
            ImageView iv = (ImageView) findViewById(R.id.imageview);
            
            // calculate the paddings of image
            double bitmap_ratio = ((double)depth_bitmap.getWidth())/depth_bitmap.getHeight();
            double imageview_ratio = ((double)iv.getWidth())/iv.getHeight();                        
            if(bitmap_ratio > imageview_ratio)
            {
            	drawLeft = 0;
            	drawHeight = (imageview_ratio/bitmap_ratio) * iv.getHeight();
            	drawWidth = iv.getWidth();
            	drawTop = ((double)iv.getHeight() - drawHeight)/2;            	
            }
            else
            {
            	drawTop = 0;
            	drawWidth = (bitmap_ratio/imageview_ratio) * iv.getWidth();
            	drawHeight = iv.getHeight();
            	drawLeft = ((double)iv.getWidth() - drawWidth)/2;            	 
            }
            
            // add touch listener            
            iv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                	double x = event.getX() - drawLeft;
                	double y = event.getY() - drawTop;
                	
                	// boundary handling
                	if(x < 0 || y < 0 || x > drawWidth || y > drawHeight)
                		return false;
                	
                	int depthWidth = depth_bitmap.getWidth();
                	
                	// scale back to size of depth bitmap
                	double depthRatio = (double)depthWidth / drawWidth;
                	int scaledX = (int)(x*depthRatio);
                	int scaledY = (int)(y*depthRatio);
                	int index = scaledX + scaledY * depthWidth;
                	int zFocus = convertToRGB( depthPixels[index] )[0]; 
                	
                	Log.d("touch","x = " + (event.getX()- drawLeft) + ", y = " + (event.getY()-drawTop));
                	Log.d("touch", "selected depth = " + zFocus);          
                	
                	if(mBokeh == null) {
                		mBokeh = new BokehFilter(image_bitmap, depth_bitmap, zFocus);
                        Bitmap blur = mBokeh.generate();
                        ImageView iv = (ImageView)findViewById(R.id.imageview);
                        iv.setImageBitmap(blur);
                	}
                    

                    return true;
                }
            });

        }
    
    
    
}
