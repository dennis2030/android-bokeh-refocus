package edu.ntu.android2014;

import java.util.Arrays;

import android.graphics.Bitmap;
import android.util.Log;

public class BokehFilter {
	private String TAG = "BokehFilter";
	
	private int PATCH_RADIUS = 15;
	
	private Bitmap mImage, mDepth;
	private int mZFocus;
	private float maxCoc;
	private float[] mCoc;
	
	private int case1 = 0;
	private int case2 = 0;
	private int case3 = 0;
	
	public BokehFilter(Bitmap image, Bitmap depth, int zFocus) {
		if(image.getWidth() != depth.getWidth() || 
				image.getHeight() != depth.getHeight()) {
			Log.d(TAG, "image and depth size not compatible");
		}
		
		mImage = image;
		mDepth = depth;
		mZFocus = zFocus;
		
		int width = mImage.getWidth();
		int height = mImage.getHeight();
		int[] depthPixels = new int[width * height];
		mDepth.getPixels(depthPixels, 0, width, 0, 0, width, height);
		mCoc = calcCoc(depthPixels, mZFocus);
	}
	
	public void generate(Bitmap res) {
		int width = mImage.getWidth();
		int height = mImage.getHeight();
		
		int[] image = new int[width * height];
		int[] depth = new int[width * height];
		int[] blur = new int[width * height];
		
		mImage.getPixels(image, 0, width, 0, 0, width, height);
		mDepth.getPixels(depth, 0, width, 0, 0, width, height);
		Log.d(TAG,"case z:" + mZFocus);
		
		blurByRow(blur, image, depth, mCoc, width, height);
		
		Log.d(TAG,"case1 = " + case1 + ", case2 = " + case2 + ", case3 = " + case3);
		
		case1 = 0;
		case2 = 0;
		case3 = 0;
		
		image = Arrays.copyOf(blur, blur.length);
		transpose(blur, width, height);
		transpose(image, width, height);
		transpose(depth, width, height);
		transpose(mCoc, width, height);
		
		blurByRow(blur, image, depth, mCoc, height, width);
		Log.d(TAG,"case1 = " + case1 + ", case2 = " + case2 + ", case3 = " + case3);
		transpose(blur, height, width);
		
		res.setPixels(blur, 0, width, 0, 0, width, height);
	}
	
	public float[] getCoc() {
		return mCoc;
	}

	private void blurByRow(int[] blur, int[] image, int[] depth, float[] coc, int width, int height) {
		float[] haha = new float[PATCH_RADIUS * 2 + 1];
		int hoho = 0;
		
		for(int r = PATCH_RADIUS; r < height - PATCH_RADIUS; ++ r) {
		//	Log.d(TAG, "bluring " + r + " row");
			for(int c = PATCH_RADIUS; c < width - PATCH_RADIUS; ++ c) {
				int idx = r * width + c;
				float[] weights = calcWeights(coc, depth, idx);

				++ hoho;
				for(int i = 0; i < weights.length; ++ i) {
					haha[i] += weights[i];
				}
				
				int newPixel = innerProduct(image, weights, idx);
				blur[idx] = newPixel;
			}
		}
		
		for(int i = 0; i < haha.length; ++ i) {
			haha[i] /= hoho;
			Log.d(TAG, "weights[" + i + "]: " + haha[i]);
		}
	}
	
	private void transpose(int[] matrix, int width, int height) {
		int[] tmp = Arrays.copyOf(matrix, matrix.length);
		for(int r = 0; r < height; ++ r) {
			for(int c = 0; c < width; ++ c) {
				int from = r * width + c;
				int to = c * height + r;
				matrix[to] = tmp[from];
			}
		}
	}

	private void transpose(float[] matrix, int width, int height) {
		float[] tmp = Arrays.copyOf(matrix, matrix.length);
		for(int r = 0; r < height; ++ r) {
			for(int c = 0; c < width; ++ c) {
				int from = r * width + c;
				int to = c * height + r;
				matrix[to] = tmp[from];
			}
		}
	}
	
    private int innerProduct(int[] image, float[] weights, int idxCenter) {
        int patchSize = PATCH_RADIUS * 2 + 1;
        int newPixel = 0x00000000;
        
        float allRed = 0, allGreen = 0, allBlue = 0;
        
        float sumWeight = 0;
        for(int i = 0; i < patchSize; ++ i) {
            sumWeight += weights[i];
        }
        for(int i = 0; i < patchSize; ++ i) {
            int pixel = image[idxCenter + i - PATCH_RADIUS];
            int oneRed, oneGreen, oneBlue;
            oneRed = (pixel >> 16) & 0xff;
            oneGreen = (pixel >> 8) & 0xff;
            oneBlue = pixel & 0xff;
            allRed += weights[i] * oneRed;
            allGreen += weights[i] * oneGreen;
            allBlue += weights[i] * oneBlue;
        }
        
        allRed /= sumWeight;
        allGreen /= sumWeight;
        allBlue /= sumWeight;
        
        newPixel += 0xff000000 |
                (((int) allRed) << 16) |
                (((int) allGreen) << 8) |
                (int) allBlue;
        
        return newPixel;
    }
	
	private float[] calcCoc(int[] inputPixels, int z_focus) {
		float[] CoC = new float[inputPixels.length];
		
		// magic constant
    	float s = PATCH_RADIUS;
    	
    	float sumCoc = 0;
    	
    	for(int i=0;i<inputPixels.length;i++)
    	{    		
    		// use b channel as depth (since it is grey level, it's okay.)
    		int depth = inputPixels[i]  & 0xff;   
    		CoC[i] = s * Math.abs(1-(float)z_focus/ depth);
   // 		Log.d(TAG, "case4 depth: " + depth + ", coc: " + CoC[i]);
    		maxCoc = Math.max(CoC[i], maxCoc);
    		sumCoc += CoC[i];
    	}
    	
    	Log.d(TAG, "case0: average coc:" + (sumCoc / inputPixels.length));
    	
    	return CoC;
	}
	
	private float[] calcWeights(float[] coc, int[] depth, int idx) {
		float[] weights = new float[PATCH_RADIUS * 2 + 1];			
		
		// initialize weights
		for(int i=0;i<weights.length;i++)
			weights[i] = 1;
		
		for(int i=0;i<weights.length;i++)
		{
			// center pixel, weight = 1
			if( i == PATCH_RADIUS )							
				continue;
			// index at original image
			int pixelNow = idx + i - PATCH_RADIUS;
			int dist = Math.abs(idx - pixelNow);
			
			// calculate overlap part
			float overlap = 0;
			if(coc[pixelNow] <= dist)
			{
				weights[i] = 0;
				case1++;
				//Log.d(TAG, "case 1");
				continue;
			}
			else if(coc[pixelNow] < dist +1)
			{
				overlap = coc[pixelNow]-dist;
				case2++;
				//Log.d(TAG, "case 2");
			}
			else
			{
				case3++;
				overlap = 1;		
				//Log.d(TAG, "case 3");
			}
			
			// calculate intensity part
			float intensity = 0; 
			float INTENSITY_CONST = 1;
			intensity = INTENSITY_CONST / (coc[pixelNow]);
			
			// calculate for leakage part
			float leakage = 1;
			int z = depth[pixelNow] & 0xff;
			if(z > mZFocus)
			{
				leakage = coc[pixelNow];
			}
			//leakage = 1.0;
			weights[i] = overlap * intensity * leakage;
		//	weights[i] = 1.0;
		}
		
		return weights;
	}
}
