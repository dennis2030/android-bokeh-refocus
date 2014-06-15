//
//  openCLNR.cpp
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

#define __CL_ENABLE_EXCEPTIONS

#include "openCLNR.h"

inline std::string loadProgram(std::string input)
{
	std::ifstream stream(input.c_str());
	if (!stream.is_open()) {
		LOGE("Cannot open input file\n");
		exit(1);
	}
	return std::string( std::istreambuf_iterator<char>(stream),
						(std::istreambuf_iterator<char>()));
}

void openCLNR(unsigned int *image_buffer, unsigned int *depth_buffer, unsigned int *blur_buffer, float *coc_buffer, unsigned int *tmp_int_buffer, float *tmp_float_buffer, int width, int height, int z_focus)
{
	LOGI("Begining of openCLNR\n");
	unsigned int imageSize = width * height * sizeof(int);
	unsigned int cocSize = width * height * sizeof(float);
	cl_int err = CL_SUCCESS;

	std::vector<cl::Platform> platforms;
	cl::Platform::get(&platforms);
	if (platforms.size() == 0) {
		std::cout << "Platform size 0\n";
		return;
	}

	cl_context_properties properties[] =
	{ CL_CONTEXT_PLATFORM, (cl_context_properties)(platforms[0])(), 0};
	cl::Context context(CL_DEVICE_TYPE_GPU, properties);

	std::vector<cl::Device> devices = context.getInfo<CL_CONTEXT_DEVICES>();
	cl::CommandQueue queue(context, devices[0], 0, &err);

	std::string kernelSource = loadProgram("/data/data/edu.ntu.android2014/app_execdir/lensBlur.cl");
	LOGI("After loadProgram\n");
	cl::Program::Sources source(1, std::make_pair(kernelSource.c_str(), kernelSource.length()+1));
	cl::Program program(context, source);
	const char *options = "-cl-fast-relaxed-math";
	LOGI("Before build\n");
	program.build(devices, options);
	LOGI("After build\n");
	cl::Kernel kernel(program, "lensBlur", &err);

	cl::Buffer cl_image_buffer = cl::Buffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, imageSize, (void *) image_buffer, &err);
	cl::Buffer cl_depth_buffer = cl::Buffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, imageSize, (void *) depth_buffer, &err);
	cl::Buffer cl_blur_buffer = cl::Buffer(context, CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR, imageSize, (void *) blur_buffer, &err);
	cl::Buffer cl_coc_buffer = cl::Buffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, cocSize, (void *) coc_buffer, &err);
	kernel.setArg(0,cl_image_buffer);
	kernel.setArg(1,cl_depth_buffer);
	kernel.setArg(2,cl_blur_buffer);
	kernel.setArg(3,cl_coc_buffer);
	kernel.setArg(4,width);
	kernel.setArg(5,height);
	kernel.setArg(6,z_focus);

	cl::Event event;

	queue.enqueueNDRangeKernel(	kernel,
			cl::NullRange,
			cl::NDRange(width,height),
			cl::NullRange,
			NULL,
			&event);




	queue.finish();

	queue.enqueueReadBuffer(cl_blur_buffer, CL_TRUE, 0, imageSize, blur_buffer);
}
