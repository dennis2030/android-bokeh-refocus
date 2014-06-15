package fi.harism.campaper;

import java.util.Arrays;

import android.graphics.Bitmap;
import android.util.Log;

public class BokehFilter {
	private String TAG = "BokehFilter";
	
	private int PATCH_RADIUS = 20;
	
	private Bitmap mImage, mDepth;
	private int mZFocus;
	private double maxCoc;
	private Bitmap mBlur;
	private int case1 = 0;
	private int case2 = 0;
	private int case3 = 0;
	
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
		
		mImage.getPixels(image, 0, width, 0, 0, width, height);
		mDepth.getPixels(depth, 0, width, 0, 0, width, height);
		Log.d(TAG,"case z:" + mZFocus);
		double[] coc = calcCoc(depth, mZFocus);
		
		blurByRow(blur, image, depth, coc, width, height);
		
		Log.d(TAG,"case1 = " + case1 + ", case2 = " + case2 + ", case3 = " + case3);
		
		case1 = 0;
		case2 = 0;
		case3 = 0;
		
		image = Arrays.copyOf(blur, blur.length);
		transpose(blur, width, height);
		transpose(image, width, height);
		transpose(depth, width, height);
		transpose(coc, width, height);
		
		blurByRow(blur, image, depth, coc, height, width);
		Log.d(TAG,"case1 = " + case1 + ", case2 = " + case2 + ", case3 = " + case3);
		transpose(blur, height, width);
		
		mBlur = mImage.copy(Bitmap.Config.ARGB_8888, true);
		mBlur.setPixels(blur, 0, width, 0, 0, width, height);
		
		return mBlur;
	}

	private void blurByRow(int[] blur, int[] image, int[] depth, double[] coc, int width, int height) {
		double[] haha = new double[PATCH_RADIUS * 2 + 1];
		int hoho = 0;
		
		for(int r = PATCH_RADIUS; r < height - PATCH_RADIUS; ++ r) {
			Log.d(TAG, "bluring " + r + " row");
			for(int c = PATCH_RADIUS; c < width - PATCH_RADIUS; ++ c) {
				int idx = r * width + c;
				double[] weights = calcWeights(coc, depth, idx);

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

	private void transpose(double[] matrix, int width, int height) {
		double[] tmp = Arrays.copyOf(matrix, matrix.length);
		for(int r = 0; r < height; ++ r) {
			for(int c = 0; c < width; ++ c) {
				int from = r * width + c;
				int to = c * height + r;
				matrix[to] = tmp[from];
			}
		}
	}
	
    private int innerProduct(int[] image, double[] weights, int idxCenter) {
        int patchSize = PATCH_RADIUS * 2 + 1;
        int newPixel = 0x00000000;
        
        double allRed = 0.0, allGreen = 0.0, allBlue = 0.0;
        
        double sumWeight = 0.0;
        for(int i = 0; i < patchSize; ++ i) {
            sumWeight += weights[i];
        }
        for(int i = 0; i < patchSize; ++ i) {
            int pixel = image[idxCenter + i - PATCH_RADIUS];
            int oneRed, oneGreen, oneBlue;
            oneRed = (pixel >> 16) & 0xff;
            oneGreen = (pixel >> 8) & 0xff;
            oneBlue = pixel & 0xff;
            allRed += weights[i] * oneRed / sumWeight;
            allGreen += weights[i] * oneGreen / sumWeight;
            allBlue += weights[i] * oneBlue / sumWeight;
        }
        
        newPixel += 0xff000000 |
                (((int) allRed) << 16) |
                (((int) allGreen) << 8) |
                (int) allBlue;
        
        return newPixel;
    }
	
	private double[] calcCoc(int[] inputPixels, int z_focus) {
		double[] CoC = new double[inputPixels.length];
		
		// magic constant
    	double s = 20.0;
    	
    	double sumCoc = 0.0;
    	
    	for(int i=0;i<inputPixels.length;i++)
    	{    		
    		// use b channel as depth (since it is grey level, it's okay.)
    		int depth = inputPixels[i]  & 0xff;   
    		CoC[i] = s * Math.abs(1-(double)z_focus/ depth);
   // 		Log.d(TAG, "case4 depth: " + depth + ", coc: " + CoC[i]);
    		maxCoc = Math.max(CoC[i], maxCoc);
    		sumCoc += CoC[i];
    	}
    	
    	Log.d(TAG, "case0: average coc:" + (sumCoc / inputPixels.length));
    	
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
				overlap = 1.0;		
				//Log.d(TAG, "case 3");
			}
			
			// calculate intensity part
			double intensity = 0.0; 
			double INTENSITY_CONST = 1;
			intensity = INTENSITY_CONST / (coc[pixelNow]);
			
			// calculate for leakage part
			double leakage = 1.0;
			double LEAKAGE_CONST = 1.0/maxCoc;
			int z = depth[pixelNow] & 0xff;
			if(z > mZFocus)
			{
				leakage = coc[pixelNow] * LEAKAGE_CONST;
			}
			//leakage = 1.0;
			weights[i] = overlap * intensity * leakage;
		//	weights[i] = 1.0;
		}
		
		return weights;
	}
}
