#include <stdio.h>
#include "main.h"

#include <android/bitmap.h>
#include <jni.h>

#include "refNR.h"
#include "openCLNR.h"

extern "C" jint
Java_edu_ntu_android2014_MainActivity_runOpenCL()
{
}



extern "C" jint
Java_edu_ntu_android2014_MainActivity_runNativeC(JNIEnv* env, jclass clazz, jobject image, jobject depth, jobject blur, jdoubleArray coc, jint zFocus, jint width, jint height)
{
	/*
    void* imageBuffer;
    void* depthBuffer;
    void* cocBuffer;
    void* blurBuffer;

    AndroidBitmap_lockPixels(env, image, &imageBuffer);
    AndroidBitmap_lockPixels(env, depth, &depthBuffer);
    AndroidBitmap_lockPixels(env, blur, &blurBuffer);

    jdouble* cocBuffer = env->GetIntArrayElements(coc, NULL);

	//refNR((unsigned char *) imageBuffer, (unsigned char *) depthBuffer, (unsigned char *) blurBuffer, cocBuffer, width, height, zFocus);

    AndroidBitmap_unlockPixels(env, imageBuffer);
    AndroidBitmap_unlockPixels(env, depthBuffer);
    AndroidBitmap_unlockPixels(env, blurBuffer);
    env->ReleaseIntArrayElements(coc, i, 0);
    */

	return 0;
}
