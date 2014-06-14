package fi.harism.campaper;

import android.graphics.Bitmap;
import android.util.Log;

public class BokehFilter {
	private String TAG = "BokehFilter";
	
	private int PATCH_RADIUS = 4;
	
	private Bitmap mImage, mDepth;
	private int mZFocus;
	private double maxCoc;
	private Bitmap mBlur;
	
	public BokehFilter(Bitmap image, Bitmap depth, int zFocus) {
		mImage = image;
		mDepth = depth;
		mZFocus = zFocus;
		if(image.getWidth() != depth.getWidth() || 
				image.getHeight() != depth.getHeight()) {
			Log.d(TAG, "image and depth size not compatible");
		}
	}
	
	public Bitmap generate() {
		if(mBlur != null) {
			return mBlur;
		}
		int width = mImage.getWidth();
		int height = mImage.getHeight();
		
		int[] image = new int[width * height];
		int[] depth = new int[width * height];
		int[] blur = new int[width * height];
		
		double[] coc = calcCoc(depth, mZFocus);
		
		mImage.getPixels(image, 0, width, 0, 0, width, height);
		mDepth.getPixels(depth, 0, width, 0, 0, width, height);
		
		for(int r = PATCH_RADIUS; r < height - PATCH_RADIUS; ++ r) {
			Log.d(TAG, "bluring " + r + " row");
			for(int c = PATCH_RADIUS; c < width - PATCH_RADIUS; ++ c) {
				int idx = r * width + c;
				double[] weights = calcWeights(coc, depth, idx);
				double sumWeight = 0.0;
				for(int i = 0; i < PATCH_RADIUS * 2 + 1; ++ i) {
					sumWeight += weights[i];
					//Log.d(TAG, "weights" + weights[i]);
				}
				for(int i = 0; i < PATCH_RADIUS * 2 + 1; ++ i) {
					int pixel = image[idx + i - PATCH_RADIUS];
					int red = (pixel >> 16) & 0xff;
					int green = (pixel >> 8) & 0xff;
					int blue = pixel & 0xff;
					red = (int) (weights[i] * red / sumWeight);
					green = (int) (weights[i] * green / sumWeight);
					blue = (int) (weights[i] * blue / sumWeight);
					blur[idx] += 0xff000000 | (red << 16) | (green << 8) | blue;
				}
			}
		}
		
		mBlur = mImage.copy(Bitmap.Config.ARGB_8888, true);
		mBlur.setPixels(blur, 0, mImage.getWidth(), 0, 0, mImage.getWidth(), mImage.getHeight());
		
		return mBlur;
	}
	
	private double[] calcCoc(int[] inputPixels, int z_focus) {
		double[] CoC = new double[inputPixels.length];
		
		// magic constant
    	double s = 30.0;
    	
    	for(int i=0;i<inputPixels.length;i++)
    	{    		
    		// use b channel as depth (since it is grey level, it's okay.)
    		int depth = inputPixels[i]  & 0xff;   
    		if(depth == 0)
    		{
    			depth = 1;
    		}
    		CoC[i] = s * Math.abs(1-z_focus/ depth);
    		maxCoc = Math.max(CoC[i], maxCoc);
    	}
    	
    	return CoC;
	}
	
	private double[] calcWeights(double[] coc, int[] depth, int idx) {
		double[] weights = new double[PATCH_RADIUS * 2 + 1];
		
		// initialize weights
		for(int i=0;i<weights.length;i++)
			weights[i] = 1.0;
		
		for(int i=0;i<weights.length;i++)
		{
			// center pixel, weight = 1
			if( i == PATCH_RADIUS )							
				continue;
			// index at original image
			int pixelNow = idx + i - PATCH_RADIUS;
			int dist = Math.abs(idx - pixelNow);
			
			// calculate overlap part
			double overlap = 0.0;
			if(coc[pixelNow] <= dist)
			{
				weights[i] = 0.0;
				//Log.d(TAG, "case 1");
				continue;
			}
			else if(coc[pixelNow] < dist +1)
			{
				overlap = coc[pixelNow]-dist;
				//Log.d(TAG, "case 2");
			}
			else
			{
				overlap = 1.0;		
				//Log.d(TAG, "case 3");
			}
			
			// calculate intensity part
			double intensity = 0.0; 
			double INTENSITY_CONST = 1.0;
			intensity = INTENSITY_CONST / (dist*dist);
			
			// calculate for leakage part
			double leakage = 1.0;
			double LEAKAGE_CONST = 1.0/maxCoc;
			int z = depth[pixelNow] & 0xff;
			if(z > mZFocus)
			{
				leakage = coc[pixelNow] * LEAKAGE_CONST;
			}
			leakage = 1.0;
			weights[i] = overlap * intensity * leakage;
		}
		
		return weights;
	}
}
