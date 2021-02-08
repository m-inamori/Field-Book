package com.fieldbook.tracker.traits;

import android.content.Context;
import android.net.UrlQuerySanitizer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import com.fieldbook.tracker.R;

public class BooleanTraitLayout extends BaseTraitLayout {

    private ImageView eImg;

    public BooleanTraitLayout(Context context) {
        super(context);
    }

    public BooleanTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BooleanTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ImageView getEImg() {
        return eImg;
    }

    @Override
    public void setNaTraitsText() {
    }

    @Override
    public String type() {
        return "boolean";
    }

    @Override
    public void init() {

        eImg = findViewById(R.id.eImg);

        // Boolean
        eImg.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                final String value = getNewTraits().get(traitObject.getTrait()).toString();
                Log.d("onClick", value);

                String  new_value;
                if (value.equalsIgnoreCase("false")) {
                    new_value = "true";
                    eImg.setImageResource(R.drawable.trait_boolean_true);
                } else {
                    new_value = "false";
                    eImg.setImageResource(R.drawable.trait_boolean_false);
                }

                updateTrait(traitObject.getTrait(), "boolean", new_value);
                getNewTraits().put(traitObject.getTrait(), new_value);
            }
        });
    }

    @Override
    public void loadLayout() {

        getEtCurVal().setVisibility(EditText.GONE);
        getEtCurVal().setEnabled(false);

        if (!getNewTraits().containsKey(traitObject.getTrait())) {
            if (traitObject.getDefaultValue().trim().equalsIgnoreCase("true")) {
                updateTrait(traitObject.getTrait(), "boolean", "true");
                eImg.setImageResource(R.drawable.trait_boolean_true);
            } else {
                updateTrait(traitObject.getTrait(), "boolean", "false");
                eImg.setImageResource(R.drawable.trait_boolean_false);
            }
        } else {
            String bval = getNewTraits().get(traitObject.getTrait()).toString();

            if (bval.equalsIgnoreCase("false")) {
                eImg.setImageResource(R.drawable.trait_boolean_false);
            } else {
                eImg.setImageResource(R.drawable.trait_boolean_true);
            }

        }

    }

    @Override
    public void deleteTraitListener() {
        if (traitObject.getDefaultValue().trim().toLowerCase().equals("true")) {
            updateTrait(traitObject.getTrait(), "boolean", "true");
            eImg.setImageResource(R.drawable.trait_boolean_true);
        } else {
            updateTrait(traitObject.getTrait(), "boolean", "false");
            eImg.setImageResource(R.drawable.trait_boolean_false);
        }
    }

    @Override
    public boolean isEntered() {
        return true;
    }

    @Override
    public boolean setValue(String value) {
        if (value.equalsIgnoreCase("false")) {
            eImg.setImageResource(R.drawable.trait_boolean_false);
        } else if(value.equalsIgnoreCase("true")) {
            eImg.setImageResource(R.drawable.trait_boolean_true);
        }
        else {
            return false;
        }
        updateTrait(traitObject.getTrait(), "boolean", value.toLowerCase());
        getNewTraits().put(traitObject.getTrait(), value.toLowerCase());
        return true;
    }
}