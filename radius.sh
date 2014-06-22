#!/bin/bash

patch_radius=$1

if [ "$1" == "" ]
then
    echo please give me a number
    exit
fi

sed -i -r "s/PATCH_RADIUS = .+;/PATCH_RADIUS = $patch_radius;/" src/edu/ntu/android2014/BokehFilter.java
sed -i -r "s/define PATCH_RADIUS .+/define PATCH_RADIUS $patch_radius/" jni/refNR.cpp
sed -i -r "s/define PATCH_RADIUS .+/define PATCH_RADIUS $patch_radius/" assets/lensBlur.cl

echo replace radius to $patch_radius
