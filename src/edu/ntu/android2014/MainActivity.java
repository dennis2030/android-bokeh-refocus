package edu.ntu.android2014;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
import android.widget.RadioGroup;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	public static native int runOpenCL(Bitmap image, Bitmap depth, Bitmap blur, float[] coc, int[] tmpInt, float tmpDouble[], int zFocus, int width, int height);
	public static native int runNativeC(Bitmap image, Bitmap depth, Bitmap blur, float[] coc, int[] tmpInt, float tmpDouble[], int zFocus, int width, int height);

	private double drawLeft = 0;
	private double drawTop = 0;
	private double drawWidth = 0;
	private double drawHeight = 0;
	
	private Bitmap depth_bitmap = null;
	private Bitmap image_bitmap = null;
	
	int[] depthPixels = null;
	
	boolean isRunningBokeh;
	
	BokehFilter mBokeh;
	ComputeMethod method;
	
	private enum ComputeMethod {
		JAVA,
		NATIVE_C,
		OPENCL
	}
	
	static {
		try {
			System.loadLibrary("main");
			Log.d(TAG, "successfully loaded native library");
		} catch(UnsatisfiedLinkError e) {
			Log.e(TAG, "load native library failed");
			Log.e(TAG, e.getMessage());
		}
	}
	
	private void copyFile(final String f) {
	    InputStream in;
	    try {
	        in = getAssets().open(f);
	        final File of = new File(getDir("execdir",MODE_PRIVATE), f);

	        final OutputStream out = new FileOutputStream(of);

	        final byte b[] = new byte[65535];
	        int sz = 0;
	        while ((sz = in.read(b)) > 0) {
	            out.write(b, 0, sz);
	        }
	        in.close();
	        out.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
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
        

        copyFile("lensBlur.cl");
        copyFile("transposeFloat.cl");
        copyFile("transposeInt.cl");
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        InputStream is = null;
        try {
            is = openFileInput("depthmap");
            depth_bitmap = BitmapFactory.decodeStream(is);
            is = openFileInput("image");
            image_bitmap = BitmapFactory.decodeStream(is);
            
            // resize bitmaps
            image_bitmap = Bitmap.createScaledBitmap(image_bitmap, (int)(image_bitmap.getWidth()/3), (int)(image_bitmap.getHeight()/3), false);            
            depth_bitmap = Bitmap.createScaledBitmap(depth_bitmap, (int)(depth_bitmap.getWidth()/3), (int)(depth_bitmap.getHeight()/3), false);
            
            Log.d("touch", "width = " + image_bitmap.getWidth() + ", height = " + image_bitmap.getHeight());
            // copy pixels of depth image into depthPixels
            depthPixels = new int[depth_bitmap.getWidth() * depth_bitmap.getHeight()];
            depth_bitmap.getPixels(depthPixels, 0, depth_bitmap.getWidth(), 0, 0, depth_bitmap.getWidth(), depth_bitmap.getHeight());                       
            
            setContentView(R.layout.layout_main);
            
            // set default compute method
            method = ComputeMethod.JAVA;
            
            // set RadioGroup listener
            RadioGroup rg = (RadioGroup)findViewById(R.id.radioGroup_SelectMethod);
            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					switch(checkedId)
					{
						case -1:
							Log.d("touch","Choices cleared.");
							break;
						case R.id.radio_JAVA:
							Log.d("touch","Select JAVA");
							method = ComputeMethod.JAVA;
							break;
						case R.id.radio_C:
							Log.d("touch","Select Native C.");
							method = ComputeMethod.NATIVE_C;
							break;
						case R.id.radio_OpenCL:
							Log.d("touch","Select OpenCL");
							method = ComputeMethod.OPENCL;
							break;
					}
					
				}
			});
            
            ImageView iv = (ImageView)findViewById(R.id.imageview);
            iv.setImageBitmap(image_bitmap);                                                
         
            
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
                	// skip the 2nd event on one touch to avoid it do things twice
                	if(event.getAction() == MotionEvent.ACTION_UP)
                		return false;
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
                	int zFocus = depthPixels[index] & 0xff; 
                	
                	Log.d("touch","x = " + (event.getX()- drawLeft) + ", y = " + (event.getY()-drawTop));
                	Log.d("touch", "selected depth = " + zFocus);
                	
                	Bitmap blur_bitmap = image_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                	
                	// set the running method
                	
                	
                	if(!isRunningBokeh) {
                		isRunningBokeh = true;
                		// calc coc
                		mBokeh = new BokehFilter(image_bitmap, depth_bitmap, zFocus);
                		float[] coc = mBokeh.getCoc();
                		
                		// start timer
                		long startTime = System.currentTimeMillis();
                		
                		if(method == ComputeMethod.JAVA) {
                			mBokeh.generate(blur_bitmap);
                		} else {
                			int[] tmpInt = new int[image_bitmap.getWidth() * image_bitmap.getHeight()];
                			float[] tmpFloat = new float[image_bitmap.getWidth() * image_bitmap.getHeight()];
                    		
                			if(method == ComputeMethod.NATIVE_C) {
                				runNativeC(image_bitmap, depth_bitmap, blur_bitmap, coc, tmpInt, tmpFloat, zFocus, image_bitmap.getWidth(), image_bitmap.getHeight());
                			} else if(method == ComputeMethod.OPENCL) {
                    			runOpenCL(image_bitmap, depth_bitmap, blur_bitmap, coc, tmpInt, tmpFloat, zFocus, image_bitmap.getWidth(), image_bitmap.getHeight());
                    		}
                		}
                		
                    	long endTime   = System.currentTimeMillis();
                    	long totalTime = endTime - startTime;
                    	
                    	Log.d("touch", "Total time of bokeh is " + totalTime + "ms.");
                    	                    	                    	                    
                        ImageView iv = (ImageView)findViewById(R.id.imageview);
                        iv.setImageBitmap(blur_bitmap);
                        isRunningBokeh = false;
                	}
                	
                	

                    return true;
                }
            });

        }
    
    
    
}
