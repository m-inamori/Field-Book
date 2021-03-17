package com.fieldbook.tracker.traits;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.activities.CollectActivity;

import java.util.Timer;
import java.util.TimerTask;

import static androidx.core.content.ContextCompat.getSystemService;

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
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("onTouchEvent", event.toString());
        // hide keyboard
        InputMethodManager imm = parent.getImm();   // object for controlling keyboard display
        imm.hideSoftInputFromWindow(getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        // move focus into this
        requestFocus();

        return true;
    }

    @Override
    public void loadLayout() {
        // Current value display
        etCurVal = findViewById(R.id.etCurVal);

        etCurVal.setHint("");
        etCurVal.setVisibility(EditText.VISIBLE);
        etCurVal.setSelection(etCurVal.getText().length());
        etCurVal.setEnabled(true);

        if (getNewTraits().containsKey(traitObject.getTrait())) {
            etCurVal.setText(getNewTraits().get(traitObject.getTrait()).toString());
            etCurVal.setTextColor(Color.parseColor(getDisplayColor()));
            etCurVal.setSelection(etCurVal.getText().length());
        } else {
            etCurVal.setText("");
            etCurVal.setTextColor(Color.BLACK);

            if (traitObject.getDefaultValue() != null && traitObject.getDefaultValue().length() > 0) {
                etCurVal.setText(traitObject.getDefaultValue());
                updateTrait(traitObject.getTrait(), traitObject.getFormat(), etCurVal.getText().toString());
            }

            etCurVal.setSelection(etCurVal.getText().length());
        }

        etCurVal.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    private Timer timer = new Timer();
                    private final long DELAY = 300;

                    @Override
                    public void afterTextChanged(Editable s) {
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        updateTrait(traitObject.getTrait(), traitObject.getFormat(),
                                                etCurVal.getText().toString());
                                        Log.d("afterTextChanged", etCurVal.getText().toString());
                                    }
                                },
                                DELAY
                        );
                    }
                }
        );

        // This is needed to fix a keyboard bug
        mHandler.postDelayed(new Runnable() {
            public void run() {
                etCurVal.dispatchTouchEvent(MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN, 0, 0, 0));
                etCurVal.dispatchTouchEvent(MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP, 0, 0, 0));
                etCurVal.setSelection(getEtCurVal().getText().length());
            }
        }, 300);
    }

    @Override
    public void deleteTraitListener() {
        ((CollectActivity) getContext()).removeTrait();
    }

    @Override
    public boolean isEntered() {
        return true;
    }

    @Override
    public boolean setValue(String value) {
        etCurVal.setText(value);
        updateTrait(traitObject.getTrait(), traitObject.getFormat(), value);
        return true;
    }
}