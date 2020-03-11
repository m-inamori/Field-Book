package com.fieldbook.tracker.traitLayouts;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.barcodes.IntentIntegrator;

import static com.fieldbook.tracker.MainActivity.thisActivity;

public class CounterTraitLayout extends TraitLayout {

    private TraitLayout thisLayout;
    private TextView counterTv;

    public CounterTraitLayout(Context context) {
        super(context);
        thisLayout = this;
    }

    public CounterTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        thisLayout = this;
    }

    public CounterTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        thisLayout = this;
    }
    
    public boolean isValidData(String value) {
        try {
            final int n = Integer.parseInt(value);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
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
                if (getNewTraits().containsKey(getCurrentTrait().getTrait()) && getNewTraits().get(getCurrentTrait().getTrait()).toString().equals("NA")) {
                    counterTv.setText("1");
                } else {
                    counterTv.setText(Integer.toString(Integer.parseInt(counterTv.getText().toString()) + 1));
                }
                updateTrait(getCurrentTrait().getTrait(), "counter", counterTv.getText().toString());
            }
        });

        // Minus counter
        minusCounterBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                //TODO NullPointerException
                if (getNewTraits().containsKey(getCurrentTrait().getTrait()) && getNewTraits().get(getCurrentTrait().getTrait()).toString().equals("NA")) {
                    counterTv.setText("-1");
                } else {
                    counterTv.setText(Integer.toString(Integer.parseInt(counterTv.getText().toString()) - 1));
                }
                updateTrait(getCurrentTrait().getTrait(), "counter", counterTv.getText().toString());
            }
        });
        
        Button button = (Button) findViewById(R.id.barcodeForCounter);
        button.setOnClickListener(new NumberButtonOnClickListener());
    }

    @Override
    public void loadLayout() {
        getEtCurVal().setVisibility(EditText.GONE);
        getEtCurVal().setEnabled(false);

        if (!getNewTraits().containsKey(getCurrentTrait().getTrait())) {
            counterTv.setText("0");
        } else {
            counterTv.setText(getNewTraits().get(getCurrentTrait().getTrait()).toString());
        }
    }

    @Override
    public void deleteTraitListener() {
        removeTrait(getCurrentTrait().getTrait());
        counterTv.setText("0");
    }
    
    private class NumberButtonOnClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            final String curText = getEtCurVal().getText().toString();
            IntentIntegrator integrator = new IntentIntegrator(thisActivity);
            integrator.initiateScan();
            thisLayout.setBarcodeTargetValue();
        }
    }
    
    public void setValue(String value) {
        counterTv.setText(value);
        updateTrait(getCurrentTrait().getTrait(), "counter", value);
    }
}