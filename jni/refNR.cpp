//
//  refNR.cpp
//  OpenCL Example1
//
//  Created by Rasmusson, Jim on 18/03/13.
//
//  Copyright (c) 2013, Sony Mobile Communications AB
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions are met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//
//     * Redistributions in binary form must reproduce the above copyright
//       notice, this list of conditions and the following disclaimer in the
//       documentation and/or other materials provided with the distribution.
//
//     * Neither the name of Sony Mobile Communications AB nor the
//       names of its contributors may be used to endorse or promote products
//       derived from this software without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
//  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//  DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
//  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
//  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
//  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
//  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#include "refNR.h"
#include <cstdlib>

#define PATCH_RADIUS 10

void calc_weights(float *weights, float *coc_buffer, unsigned int *depth_buffer, int z_focus, int idx)
{
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
		intensity = INTENSITY_CONST / (coc_buffer[pixel_now]);

		// calculate for leakage part
		float leakage = 1.0;
		int z = depth_buffer[pixel_now] & 0xff;
		if(z > z_focus)
		{
			leakage = coc_buffer[pixel_now];
		}
		weights[i] = overlap * intensity * leakage;
	}
}

int inner_product(unsigned int *image_buffer, float *weights, int idx_center)
{
    int patch_size = PATCH_RADIUS * 2 + 1;
    int new_pixel = 0x00000000;

    float all_red = 0.0, all_green = 0.0, all_blue = 0.0;

    float sum_weight = 0.0;
    for(int i = 0; i < patch_size; ++ i) {
        sum_weight += weights[i];
    }
    for(int i = 0; i < patch_size; ++ i) {
        int pixel = image_buffer[idx_center + i - PATCH_RADIUS];
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

    return new_pixel;
}

void transpose(float *matrix, float *tmp_buffer, int width, int height) {
	memcpy(tmp_buffer, matrix, width * height * sizeof(float));

	for(int r = 0; r < height; ++ r) {
		for(int c = 0; c < width; ++ c) {
			int from = r * width + c;
			int to = c * height + r;
			matrix[to] = tmp_buffer[from];
		}
	}
}

void transpose(unsigned int *matrix, unsigned int *tmp_buffer, int width, int height) {
	memcpy(tmp_buffer, matrix, width * height * sizeof(unsigned int));

	for(int r = 0; r < height; ++ r) {
		for(int c = 0; c < width; ++ c) {
			int from = r * width + c;
			int to = c * height + r;
			matrix[to] = tmp_buffer[from];
		}
	}
}

void blur_by_row(unsigned int *image_buffer, unsigned int *depth_buffer, unsigned int *blur_buffer, float *coc_buffer, int width, int height, int z_focus)
{
	for(int r = PATCH_RADIUS;r < height - PATCH_RADIUS ; ++r)
	{
		for(int c = PATCH_RADIUS; c < width - PATCH_RADIUS; ++c)
		{
			int idx = r * width + c;
			float weights[PATCH_RADIUS*2+1];
			calc_weights(weights, coc_buffer, depth_buffer, z_focus, idx);

			int new_pixel = inner_product(image_buffer, weights, idx);
			blur_buffer[idx] = new_pixel;
		}
	}
}

void refNR(unsigned int *image_buffer, unsigned int *depth_buffer, unsigned int *blur_buffer, float *coc_buffer, unsigned int *tmp_int_buffer, float *tmp_float_buffer, int width, int height, int z_focus)
{
	blur_by_row(image_buffer, depth_buffer, blur_buffer, coc_buffer, width, height, z_focus);
	transpose(image_buffer, tmp_int_buffer, width, height);
	transpose(depth_buffer, tmp_int_buffer, width, height);
	transpose(blur_buffer, tmp_int_buffer, width, height);
	transpose(coc_buffer, tmp_float_buffer, width, height);

	memcpy(image_buffer, blur_buffer, width * height * sizeof(unsigned int));

	blur_by_row(image_buffer, depth_buffer, blur_buffer, coc_buffer, height, width, z_focus);

	transpose(blur_buffer, tmp_int_buffer, height, width);
}

