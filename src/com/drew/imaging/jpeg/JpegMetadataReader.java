/*
 * Copyright 2002-2013 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    http://drewnoakes.com/code/exif/
 *    http://code.google.com/p/metadata-extractor/
 */
package com.drew.imaging.jpeg;

import com.drew.lang.StreamReader;
import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import com.drew.metadata.Metadata;
import com.drew.metadata.adobe.AdobeJpegReader;
import com.drew.metadata.exif.ExifReader;
import com.drew.metadata.icc.IccReader;
import com.drew.metadata.iptc.IptcReader;
import com.drew.metadata.jfif.JfifReader;
import com.drew.metadata.jpeg.JpegCommentReader;
import com.drew.metadata.jpeg.JpegReader;
import com.drew.metadata.photoshop.PhotoshopReader;
import com.drew.metadata.xmp.XmpReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Obtains all available metadata from JPEG formatted files.
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public class JpegMetadataReader
{
    public static final Iterable<JpegSegmentMetadataReader> ALL_READERS = Arrays.asList(
            new JpegReader(),
            new JpegCommentReader(),
            new JfifReader(),
            new ExifReader(),
            new XmpReader(),
            new IccReader(),
            new PhotoshopReader(),
            new IptcReader(),
            new AdobeJpegReader()
    );

    @NotNull
    public static Metadata readMetadata(@NotNull InputStream inputStream, @Nullable Iterable<JpegSegmentMetadataReader> readers) throws JpegProcessingException, IOException
    {
        Metadata metadata = new Metadata();
        process(metadata, inputStream, readers);
        return metadata;
    }

    @NotNull
    public static Metadata readMetadata(@NotNull InputStream inputStream) throws JpegProcessingException, IOException
    {
        return readMetadata(inputStream, null);
    }

    @NotNull
    public static Metadata readMetadata(@NotNull File file, @Nullable Iterable<JpegSegmentMetadataReader> readers) throws JpegProcessingException, IOException
    {
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream(file);
            return readMetadata(inputStream, readers);
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }

    @NotNull
    public static Metadata readMetadata(@NotNull File file) throws JpegProcessingException, IOException
    {
        return readMetadata(file, null);
    }

    public static void process(@NotNull Metadata metadata, @NotNull InputStream inputStream) throws JpegProcessingException, IOException
    {
        process(metadata, inputStream, null);
    }

    public static void process(@NotNull Metadata metadata, @NotNull InputStream inputStream, @Nullable Iterable<JpegSegmentMetadataReader> readers) throws JpegProcessingException, IOException
    {
        if (readers == null)
            readers = ALL_READERS;

        Set<JpegSegmentType> segmentTypes = new HashSet<JpegSegmentType>();
        for (JpegSegmentMetadataReader reader : readers) {
            for (JpegSegmentType type : reader.getSegmentTypes()) {
                segmentTypes.add(type);
            }
        }

        JpegSegmentData segmentData = JpegSegmentReader.readSegments(new StreamReader(inputStream), segmentTypes);

        processJpegSegmentData(metadata, readers, segmentData);
    }

    public static void processJpegSegmentData(Metadata metadata, Iterable<JpegSegmentMetadataReader> readers, JpegSegmentData segmentData)
    {
        // Pass the appropriate byte arrays to each reader.
        for (JpegSegmentMetadataReader reader : readers) {
            for (JpegSegmentType segmentType : reader.getSegmentTypes()) {
                for (byte[] segmentBytes : segmentData.getSegments(segmentType)) {
                    if (reader.canProcess(segmentBytes, segmentType)) {
                        reader.extract(segmentBytes, metadata, segmentType);
                    }
                }
            }
        }
    }

    private JpegMetadataReader() throws Exception
    {
        throw new Exception("Not intended for instantiation");
    }
}