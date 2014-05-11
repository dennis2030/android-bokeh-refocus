package com.drew.imaging.png;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

/**
 * @author Drew Noakes http://drewnoakes.com
 */
public enum PngColorType
{
    /**
     * Each pixel is a greyscale sample.
     */
    Greyscale(0, "Greyscale", 1,2,4,8,16),

    /**
     * Each pixel is an R,G,B triple.
     */
    TrueColor(2, "True Color", 8,16),

    /**
     * Each pixel is a palette index. Seeing this value indicates that a <code>PLTE</code> chunk shall appear.
     */
    IndexedColor(3, "Indexed Color", 1,2,4,8),

    /**
     * Each pixel is a greyscale sample followed by an alpha sample.
     */
    GreyscaleWithAlpha(4, "Greyscale with Alpha", 8,16),

    /**
     * Each pixel is an R,G,B triple followed by an alpha sample.
     */
    TrueColorWithAlpha(6, "True Color with Alpha", 8,16);

    @Nullable
    public static PngColorType fromNumericValue(int numericValue)
    {
        PngColorType[] colorTypes = PngColorType.class.getEnumConstants();
        for (PngColorType colorType : colorTypes) {
            if (colorType.getNumericValue() == numericValue) {
                return colorType;
            }
        }
        return null;
    }

    private final int _numericValue;
    @NotNull private final String _description;
    @NotNull private final int[] _allowedBitDepths;

    private PngColorType(int numericValue, @NotNull String description, @NotNull int... allowedBitDepths)
    {
        _numericValue = numericValue;
        _description = description;
        _allowedBitDepths = allowedBitDepths;
    }

    public int getNumericValue()
    {
        return _numericValue;
    }

    @NotNull
    public String getDescription()
    {
        return _description;
    }

    @NotNull
    public int[] getAllowedBitDepths()
    {
        return _allowedBitDepths;
    }
}
