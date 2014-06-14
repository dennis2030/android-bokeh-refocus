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
	
	private enum Direction {
		ROW,
		COLUMN
	};
	
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
		int width = mImage.getWidth();
		int height = mImage.getHeight();
		
		int[] image = new int[width * height];
		int[] depth = new int[width * height];
		int[] blur = new int[width * height];
		
		double[] coc = calcCoc(depth, mZFocus);
		
		mImage.getPixels(image, 0, width, 0, 0, width, height);
		mDepth.getPixels(depth, 0, width, 0, 0, width, height);
		
		blurByRow(blur, image, depth, coc);
		
		image = Arrays.copyOf(blur, blur.length);
		
		blurByCol(blur, image, depth, coc);
		
		Bitmap res = mImage.copy(Bitmap.Config.ARGB_8888, true);
		res.setPixels(blur, 0, mImage.getWidth(), 0, 0, mImage.getWidth(), mImage.getHeight());
		
		return res;
	}

	private void blurByRow(int[] blur, int[] image, int[] depth, double[] coc) {
		int width = mImage.getWidth();
		int height = mImage.getHeight();

		for(int r = PATCH_RADIUS; r < height - PATCH_RADIUS; ++ r) {
			Log.d(TAG, "bluring " + r + " row");
			for(int c = PATCH_RADIUS; c < width - PATCH_RADIUS; ++ c) {
				double[] partCoc = (double[]) getPatch(coc, r, c, Direction.ROW);
				int[] partDepth = (int[]) getPatch(depth, r, c, Direction.ROW);
				double[] weights = calcWeights(partCoc, partDepth);
				
				int[] partImage = (int[]) getPatch(image, r, c, Direction.ROW);
				int newPixel = innerProduct(partImage, weights);
				updatePixel(r, c, blur, newPixel);
			}
		}
	}

	private void blurByCol(int[] blur, int[] image, int[] depth, double[] coc) {
		int width = mImage.getWidth();
		int height = mImage.getHeight();

		for(int c = PATCH_RADIUS; c < width - PATCH_RADIUS; ++ c) {
			Log.d(TAG, "bluring " + c + " column");
			for(int r = PATCH_RADIUS; r < height - PATCH_RADIUS; ++ r) {
				double[] partCoc = (double[]) getPatch(coc, r, c, Direction.COLUMN);
				int[] partDepth = (int[]) getPatch(depth, r, c, Direction.COLUMN);
				double[] weights = calcWeights(partCoc, partDepth);
				
				int[] partImage = (int[]) getPatch(image, r, c, Direction.COLUMN);
				int newPixel = innerProduct(partImage, weights);
				updatePixel(r, c, blur, newPixel);
			}
		}
	}
	
	private Object getPatch(Object src, int rCenter, int cCenter, Direction direction) {
		int patchSize = PATCH_RADIUS * 2 + 1;
		Object patch;
		if(src instanceof double[]) {
			patch = new double[patchSize];
		} else if(src instanceof int[]) {
			patch = new int[patchSize];
		} else {
			patch = null;
			Log.d(TAG, "currently supported type are: double[], int[]");
		}
		for(int i = 0; i < patchSize; ++ i) {
			int curIdx = -1;
			if(direction == Direction.ROW) {
				curIdx = rCenter * mImage.getWidth() + (cCenter + i - PATCH_RADIUS);
			} else if(direction == Direction.COLUMN) {
				curIdx = (rCenter + i - PATCH_RADIUS) * mImage.getWidth() + cCenter;
			}
			
			if(src instanceof double[]) {
				((double[]) patch)[i] = ((double[]) src)[curIdx];
			} else if(src instanceof int[]) {
				((int[]) patch)[i] = ((int[]) src)[curIdx];
			}
		}
		return patch;
	}
	
	private int innerProduct(int[] partImage, double[] weights) {
		int patchSize = PATCH_RADIUS * 2 + 1;
		int newPixel = 0x00000000;
		
		double allRed = 0.0, allGreen = 0.0, allBlue = 0.0;
		
		double sumWeight = 0.0;
		for(int i = 0; i < patchSize; ++ i) {
			sumWeight += weights[i];
		}
		for(int i = 0; i < patchSize; ++ i) {
			int pixel = partImage[i];
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
	
	private void updatePixel(int rTarget, int cTarget, int[] blur, int newPixel) {
		int idx = rTarget * mImage.getWidth() + cTarget;
		blur[idx] = newPixel;
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
	
	private double[] calcWeights(double[] partCoc, int[] partDepth) {
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
			int dist = Math.abs(PATCH_RADIUS - i);
			
			// calculate overlap part
			double overlap = 0.0;
			if(partCoc[i] <= dist)
			{
				weights[i] = 0.0;
				continue;
			}
			else if(partCoc[i] < dist +1)
			{
				overlap = partCoc[i]-dist;
			}
			else
			{
				overlap = 1.0;		
			}
			
			// calculate intensity part
			double intensity = 0.0; 
			double INTENSITY_CONST = 1.0;
			intensity = INTENSITY_CONST / (dist*dist);
			
			// calculate for leakage part
			double leakage = 1.0;
			double LEAKAGE_CONST = 1.0/maxCoc;
			int z = partDepth[i] & 0xff;
			if(z > mZFocus)
			{
				leakage = partCoc[i] * LEAKAGE_CONST;
			}
			leakage = 1.0;
			weights[i] = overlap * intensity * leakage;
			weights[i] = 1.0;
		}
		
		return weights;
	}
}
