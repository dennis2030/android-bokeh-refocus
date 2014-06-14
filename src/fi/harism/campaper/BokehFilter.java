package fi.harism.campaper;

import android.graphics.Bitmap;
import android.util.Log;

public class BokehFilter {
	private String TAG = "BokehFilter";
	
	private int PATCH_RADIUS = 4;
	
	private Bitmap mImage, mDepth;
	private int mZFocus;
	
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
		
		int[] coc = calcCoc(depth);
		
		mImage.getPixels(image, 0, width, 0, 0, width, height);
		mDepth.getPixels(depth, 0, width, 0, 0, width, height);
		
		for(int r = PATCH_RADIUS; r < height - PATCH_RADIUS; ++ r) {
			for(int c = PATCH_RADIUS; c < width - PATCH_RADIUS; ++ c) {
				int idx = r * width + c;
				double[] weights = calcWeights(coc, depth, r, c);
				for(int i = 0; i < PATCH_RADIUS * 2 + 1; ++ i) {
					blur[idx] += weights[i] * image[idx + i - PATCH_RADIUS];
				}
			}
		}
		
		Bitmap res = mImage.copy(Bitmap.Config.ARGB_8888, true);
		res.setPixels(blur, 0, mImage.getWidth(), 0, 0, mImage.getWidth(), mImage.getHeight());
		
		return res;
	}
	
	private int[] calcCoc(int[] depth) {
		int[] coc = new int[depth.length];
		return coc;
	}
	
	private double[] calcWeights(int[] coc, int[] depth, int r, int c) {
		double[] weights = new double[PATCH_RADIUS * 2 + 1];
		return weights;
	}
}
