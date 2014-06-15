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
    void* imageBuffer;
    void* depthBuffer;
    void* blurBuffer;

    AndroidBitmap_lockPixels(env, image, &imageBuffer);
    AndroidBitmap_lockPixels(env, depth, &depthBuffer);
    AndroidBitmap_lockPixels(env, blur, &blurBuffer);

    jdouble* cocBuffer = env->GetDoubleArrayElements(coc, NULL);

	//refNR((unsigned char *) imageBuffer, (unsigned char *) depthBuffer, (unsigned char *) blurBuffer, cocBuffer, width, height, zFocus);

    AndroidBitmap_unlockPixels(env, image);
    AndroidBitmap_unlockPixels(env, depth);
    AndroidBitmap_unlockPixels(env, blur);
    env->ReleaseDoubleArrayElements(coc, cocBuffer, 0);

	return 0;
}
