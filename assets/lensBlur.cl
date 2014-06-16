#define PATCH_RADIUS 10

__kernel void lensBlur(__global int *image_buffer, __global int *depth_buffer, __global int* blur_buffer, __global float *coc_buffer, const int width, const int height, const int z_focus)
{
    int c = get_global_id(0);
    int r = get_global_id(1);
    int idx = r * width + c;
    
	float weights[PATCH_RADIUS*2+1];
	
	int patch_size = PATCH_RADIUS * 2 + 1;
	for(int i=0;i<patch_size;i++)
	{
		// center pixel, weight = 1
		if( i == PATCH_RADIUS )
		{
			weights[i] = 1.0;
			continue;
		}

		// index at original image
		int pixel_now = idx + i - PATCH_RADIUS;
		int dist = abs(idx - pixel_now);

		// calculate overlap part
		float overlap = 0.0;

		if(coc_buffer[pixel_now] <= dist)
		{
			weights[i] = 0.0;
			continue;
		}
		else if(coc_buffer[pixel_now] < dist +1)
		{
			overlap = coc_buffer[pixel_now]-dist;
		}
		else
		{
			overlap = 1.0;
		}

		// calculate intensity part
		float intensity = 0.0;
		float INTENSITY_CONST = 1;
		//intensity = INTENSITY_CONST / (coc_buffer[pixel_now]*coc_buffer[pixel_now]);
		intensity = 1/(coc_buffer[pixel_now]*coc_buffer[pixel_now]);

		// calculate for leakage part
		float leakage = 1.0;
		int z = depth_buffer[pixel_now] & 0xff;
		if(z > z_focus)
		{
			leakage = coc_buffer[pixel_now];
		}
		weights[i] = overlap * intensity * leakage;
	}
	
    int new_pixel = 0x00000000;

    float all_red = 0.0, all_green = 0.0, all_blue = 0.0;

    float sum_weight = 0.0;
    for(int i = 0; i < patch_size; ++ i) {
        sum_weight += weights[i];
    }
    for(int i = 0; i < patch_size; ++ i) {
        int pixel = image_buffer[idx + i - PATCH_RADIUS];
        int one_red, one_green, one_blue;
        one_red = (pixel >> 16) & 0xff;
        one_green = (pixel >> 8) & 0xff;
        one_blue = pixel & 0xff;
        all_red += weights[i] * one_red;
        all_green += weights[i] * one_green;
        all_blue += weights[i] * one_blue;
    }

    all_red /= sum_weight;
    all_green /= sum_weight;
    all_blue /= sum_weight;

    new_pixel += 0xff000000 |
            (((int) all_red) << 16) |
            (((int) all_green) << 8) |
            (int) all_blue;
            
	blur_buffer[idx] = new_pixel;
}
