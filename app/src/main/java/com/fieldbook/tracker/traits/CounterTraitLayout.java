package com.fieldbook.tracker.traits;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fieldbook.tracker.R;

import static java.lang.Math.min;

public class CounterTraitLayout extends BaseTraitLayout {

    private TextView counterTv;

    public CounterTraitLayout(Context context) {
        super(context);
    }

    public CounterTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CounterTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setNaTraitsText() {
        counterTv.setText("NA");
    }

    @Override
    public String type() {
        return "counter";
    }

    @Override
    public void init() {
        Button addCounterBtn = findViewById(R.id.addBtn);
        Button minusCounterBtn = findViewById(R.id.minusBtn);
        counterTv = findViewById(R.id.curCount);

        // Add counter
        addCounterBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                //TODO NullPointerException
                if (getNewTraits().containsKey(traitObject.getTrait()) && getNewTraits().get(traitObject.getTrait()).toString().equals("NA")) {
                    counterTv.setText("1");
                } else {
                    counterTv.setText(Integer.toString(Integer.parseInt(counterTv.getText().toString()) + 1));
                }
                updateTrait(traitObject.getTrait(), "counter", counterTv.getText().toString());
            }
        });

        // Minus counter
        minusCounterBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                //TODO NullPointerException
                if (getNewTraits().containsKey(traitObject.getTrait()) &&
                    getNewTraits().get(traitObject.getTrait()).toString().equals("NA")) {
                    counterTv.setText("-1");
                } else {
                    counterTv.setText(Integer.toString(Integer.parseInt(counterTv.getText().toString()) - 1));
                }
                updateTrait(traitObject.getTrait(), "counter", counterTv.getText().toString());
            }
        });

    }

    @Override
    public void loadLayout() {
        getEtCurVal().setVisibility(EditText.GONE);
        getEtCurVal().setEnabled(false);

        if (!getNewTraits().containsKey(traitObject.getTrait())) {
            counterTv.setText("0");
        } else {
            counterTv.setText(getNewTraits().get(traitObject.getTrait()).toString());
        }
    }

    @Override
    public boolean setValue(String value) {
        try {
            final int counter = Integer.parseInt(value);
            if (counter < 0)
                return false;

            counterTv.setText(value);
            updateTrait(traitObject.getTrait(), "counter", value);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void deleteTraitListener() {
        removeTrait(traitObject);
        counterTv.setText("0");
    }

    @Override
    public boolean isEntered() {
        return true;
    }
}