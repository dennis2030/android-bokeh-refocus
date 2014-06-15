#include <stdio.h>
#include "main.h"

#include <android/bitmap.h>
#include <jni.h>

#include "refNR.h"
#include "openCLNR.h"

extern "C" jint
Java_edu_ntu_android2014_MainActivity_runOpenCL(JNIEnv* env, jclass clazz, jobject image, jobject depth, jobject blur, jdoubleArray coc, jintArray tmp_int, jdoubleArray tmp_double, jint zFocus, jint width, jint height)
{
    void* imageBuffer;
    void* depthBuffer;
    void* blurBuffer;

    AndroidBitmap_lockPixels(env, image, &imageBuffer);
    AndroidBitmap_lockPixels(env, depth, &depthBuffer);
    AndroidBitmap_lockPixels(env, blur, &blurBuffer);

    jdouble* cocBuffer = env->GetDoubleArrayElements(coc, NULL);
    jint* tmpIntBuffer = env->GetIntArrayElements(tmp_int, NULL);
    jdouble* tmpDoubleBuffer = env->GetDoubleArrayElements(tmp_double, NULL);

    openCLNR((unsigned int *) imageBuffer, (unsigned int *) depthBuffer, (unsigned int *) blurBuffer, cocBuffer, (unsigned int*)tmpIntBuffer, tmpDoubleBuffer, width, height, zFocus);

    AndroidBitmap_unlockPixels(env, image);
    AndroidBitmap_unlockPixels(env, depth);
    AndroidBitmap_unlockPixels(env, blur);
    env->ReleaseDoubleArrayElements(coc, cocBuffer, 0);
    env->ReleaseIntArrayElements(tmp_int, tmpIntBuffer, 0);
    env->ReleaseDoubleArrayElements(tmp_double, tmpDoubleBuffer, 0);

	return 0;
}

extern "C" jint
Java_edu_ntu_android2014_MainActivity_runNativeC(JNIEnv* env, jclass clazz, jobject image, jobject depth, jobject blur, jdoubleArray coc, jintArray tmp_int, jdoubleArray tmp_double, jint zFocus, jint width, jint height)
{
    void* imageBuffer;
    void* depthBuffer;
    void* blurBuffer;

    AndroidBitmap_lockPixels(env, image, &imageBuffer);
    AndroidBitmap_lockPixels(env, depth, &depthBuffer);
    AndroidBitmap_lockPixels(env, blur, &blurBuffer);

    jdouble* cocBuffer = env->GetDoubleArrayElements(coc, NULL);
    jint* tmpIntBuffer = env->GetIntArrayElements(tmp_int, NULL);
    jdouble* tmpDoubleBuffer = env->GetDoubleArrayElements(tmp_double, NULL);

	refNR((unsigned int *) imageBuffer, (unsigned int *) depthBuffer, (unsigned int *) blurBuffer, cocBuffer, (unsigned int*)tmpIntBuffer, tmpDoubleBuffer, width, height, zFocus);

    AndroidBitmap_unlockPixels(env, image);
    AndroidBitmap_unlockPixels(env, depth);
    AndroidBitmap_unlockPixels(env, blur);
    env->ReleaseDoubleArrayElements(coc, cocBuffer, 0);
    env->ReleaseIntArrayElements(tmp_int, tmpIntBuffer, 0);
    env->ReleaseDoubleArrayElements(tmp_double, tmpDoubleBuffer, 0);

	return 0;
}
