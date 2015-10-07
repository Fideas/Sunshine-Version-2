package com.example.android.sunshine.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;

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

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        EditText editText = getEditText();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Dialog d = getDialog();
                if (d instanceof AlertDialog){
                    AlertDialog dialog = (AlertDialog) d;
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    if (editable.length() < mMinLength){
                        positiveButton.setEnabled(false);
                    } else {
                        positiveButton.setEnabled(true);
                    }
                }
            }
        });
    }

    public int getMinLength(){
        return this.mMinLength;
    }
}
