package fi.harism.campaper;

import android.graphics.Bitmap;
import android.util.Log;

public class BokehFilter {
	private String TAG = "BokehFilter";
	
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
		
		mImage.getPixels(image, 0, width, 0, 0, width, height);
		mDepth.getPixels(depth, 0, width, 0, 0, width, height);
		
		for(int r = 0; r < height; ++ r) {
			for(int c = 0; c < width; ++ c) {
				int idx = r * width + c;
				blur[idx] = 0x000000;
			}
		}
		
		Bitmap res = mImage.copy(Bitmap.Config.ARGB_8888, true);
		res.setPixels(blur, 0, mImage.getWidth(), 0, 0, mImage.getWidth(), mImage.getHeight());
		
		return res;
	}
}
