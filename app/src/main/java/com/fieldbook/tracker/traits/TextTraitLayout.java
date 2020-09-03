package com.fieldbook.tracker.traits;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.activities.CollectActivity;

public class TextTraitLayout extends BaseTraitLayout {

    private Handler mHandler = new Handler();

    private EditText    etCurVal;

    public TextTraitLayout(Context context) {
        super(context);
    }

    public TextTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setNaTraitsText() {
    }

    @Override
    public String type() {
        return "text";
    }

    @Override
    public void init() {

    }

    @Override
    public void loadLayout() {
        // Current value display
        etCurVal = findViewById(R.id.etCurVal);

        etCurVal.setHint("");
        etCurVal.setVisibility(EditText.VISIBLE);
        etCurVal.setSelection(etCurVal.getText().length());
        etCurVal.setEnabled(true);

        // あとで復活させる
        /*
        if (getNewTraits().containsKey(traitObject.getTrait())) {
            etCurVal.removeTextChangedListener(getCvText());
            etCurVal.setText(getNewTraits().get(traitObject.getTrait()).toString());
            etCurVal.setTextColor(Color.parseColor(getDisplayColor()));
            etCurVal.addTextChangedListener(getCvText());
            etCurVal.setSelection(etCurVal.getText().length());
        } else {
            etCurVal.removeTextChangedListener(getCvText());
            etCurVal.setText("");
            etCurVal.setTextColor(Color.BLACK);

            if (traitObject.getDefaultValue() != null && traitObject.getDefaultValue().length() > 0) {
                etCurVal.setText(traitObject.getDefaultValue());
                updateTrait(traitObject.getTrait(), traitObject.getFormat(), etCurVal.getText().toString());
            }

            etCurVal.addTextChangedListener(getCvText());
            etCurVal.setSelection(etCurVal.getText().length());
        }
         */

        // This is needed to fix a keyboard bug
        mHandler.postDelayed(new Runnable() {
            public void run() {
                Log.d("TextTraitLayout", traitObject.getTrait());
                Log.d("TextTraitLayout", String.valueOf(etCurVal == null));
                getEtCurVal().dispatchTouchEvent(MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN, 0, 0, 0));
                getEtCurVal().dispatchTouchEvent(MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP, 0, 0, 0));
                getEtCurVal().setSelection(getEtCurVal().getText().length());
            }
        }, 300);
    }

    @Override
    public void deleteTraitListener() {
        ((CollectActivity) getContext()).removeTrait();
    }
}