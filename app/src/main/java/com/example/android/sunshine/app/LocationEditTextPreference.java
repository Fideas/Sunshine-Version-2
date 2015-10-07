package com.example.android.sunshine.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Custom view for the location setting EditText
 */
public class LocationEditTextPreference extends EditTextPreference{

    private static final int DEFAULT_MINIMUM_LOCATION_LENGTH = 3;
    private int mMinLength;

    public LocationEditTextPreference(Context context, AttributeSet attributes){
        super(context, attributes);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attributes,
                R.styleable.LocationEditTextPreference,
                0, 0);

        try {
                mMinLength = typedArray.getInteger(R.styleable.LocationEditTextPreference_minLength,
                        DEFAULT_MINIMUM_LOCATION_LENGTH);
        } finally {
            typedArray.recycle();
        }
    }

    public int getMinLength(){
        return this.mMinLength;
    }
}
