package com.fieldbook.tracker.traits;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.util.Log;

import com.google.zxing.integration.android.IntentIntegrator;

import com.fieldbook.tracker.activities.CollectActivity;
import com.fieldbook.tracker.R;
import com.fieldbook.tracker.utilities.Utils;
import static com.fieldbook.tracker.activities.CollectActivity.thisActivity;

public class TraitWithBarcodeLayout extends BaseTraitLayout {

    public TraitWithBarcodeLayout(Context context) {
        super(context);
    }

    public TraitWithBarcodeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TraitWithBarcodeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setNaTraitsText() {
    }

    @Override
    public String type() {
        return "with_barcode";
    }

    @Override
    public void init() {
        ImageButton getBarcode = findViewById(R.id.inputWithBarcode);
        // Get Barcode
        getBarcode.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                ((CollectActivity)thisActivity).setBarcodeTargetValue();
                IntentIntegrator integrator = new IntentIntegrator(thisActivity);
                integrator.initiateScan();
            }
        });
    }

    @Override
    public void loadLayout() {

        getEtCurVal().setVisibility(EditText.VISIBLE);

        if (getNewTraits().containsKey(traitObject.getTrait())) {
            getEtCurVal().setText(getNewTraits().get(traitObject.getTrait()).toString());
            getEtCurVal().setTextColor(Color.parseColor(getDisplayColor()));
        } else {
            getEtCurVal().setText("");
            getEtCurVal().setTextColor(Color.BLACK);

            if (traitObject.getDefaultValue() != null
                    && traitObject.getDefaultValue().length() > 0)
                getEtCurVal().setText(traitObject.getDefaultValue());
        }
    }

    @Override
    public void deleteTraitListener() {
        ((CollectActivity) getContext()).removeTrait();
    }

    @Override
    public boolean isEntered() {
        return !TextUtils.isEmpty(getEtCurVal().getText().toString());
    }
}