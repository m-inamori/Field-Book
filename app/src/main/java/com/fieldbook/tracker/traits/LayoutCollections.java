package com.fieldbook.tracker.traits;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.activities.CollectActivity;
import com.fieldbook.tracker.activities.TabletCollectActivity;
import com.fieldbook.tracker.objects.TraitObject;
import com.fieldbook.tracker.utilities.Utils;

import org.jsoup.Connection;

import java.util.ArrayList;
import java.util.Map;

import static com.fieldbook.tracker.activities.ConfigActivity.dt;

public class LayoutCollections {
    private ArrayList<BaseTraitLayout> traitLayouts;
    private TabletCollectActivity parent;
    private TextView traitName;
    private TextView traitDetails;
    private EditText etCurVal;

    private String[] prefixTraits;

    static int[] traitIDs = {
            R.id.angleLayout, R.id.audioLayout, R.id.barcodeLayout,
            R.id.booleanLayout, R.id.categoricalLayout, R.id.counterLayout,
            R.id.dateLayout, R.id.diseaseLayout, R.id.locationLayout,
            R.id.multicatLayout, R.id.numericLayout, R.id.percentLayout,
            R.id.photoLayout, R.id.textLayout, R.id.labelprintLayout,
            R.id.TraitWithBarcodeLayout
    };

    public LayoutCollections(Activity _activity) {

        parent = (TabletCollectActivity)_activity;
        traitLayouts = new ArrayList<>();
        for (int traitID : traitIDs) {
            BaseTraitLayout layout = parent.findViewById(traitID);
            layout.init();
            layout.setParent(parent, this);
            traitLayouts.add(layout);
        }
    }

    public LayoutCollections(Activity _activity, View _view) {
        parent = (TabletCollectActivity)_activity;
        setWidgets(_view);
        traitLayouts = new ArrayList<>();
        for (int traitID : traitIDs) {
            BaseTraitLayout layout = _view.findViewById(traitID);
            layout.init();
            layout.setParent(parent, this);
            traitLayouts.add(layout);
        }
    }

    public void setWidgets(View view) {
        traitName = view.findViewById(R.id.traitName);
        traitDetails = view.findViewById(R.id.traitDetails);
        etCurVal = view.findViewById(R.id.etCurVal);
    }

    public EditText getEtCurVal() { return etCurVal; }

    TraitObject traitObject = null;

    public TraitObject getTraitObject() {
        return traitObject;
    }

    public void setTraitObject(TraitObject tobj) {
        traitObject = tobj;
        hideLayouts();
        BaseTraitLayout currentLayout = getTraitLayout(traitObject.getFormat());
        for(BaseTraitLayout layout : traitLayouts) {
            Log.d("LayoutCollections", String.valueOf(etCurVal != null));
            if (layout == currentLayout) {
                layout.setTraitObject(traitObject);
                layout.setVisibility(View.VISIBLE);
                layout.loadLayout();
            }
        }
        traitName.setText(tobj.getTrait());
        traitDetails.setText(tobj.getDetails());
    }

    public BaseTraitLayout getTraitLayout(final String format) {
        for (BaseTraitLayout layout : traitLayouts) {
            if (layout.isTraitType(format)) {
                return layout;
            }
        }
        return getTraitLayout("text");
    }

    public BaseTraitLayout getCurrentLayout() {
        Log.d("LayoutCollections", String.valueOf(traitObject != null));
        return getTraitLayout(traitObject.getFormat());
    }

    public PhotoTraitLayout getPhotoTrait() {
        return (PhotoTraitLayout) getTraitLayout("photo");
    }

    public void hideLayouts() {
        for (BaseTraitLayout layout : traitLayouts) {
            layout.setVisibility(View.GONE);
        }
    }

    public void deleteTraitListener(String format) {
        getTraitLayout(format).deleteTraitListener();
    }

    public void setNaTraitsText() {
        getTraitLayout(traitObject.getFormat()).setNaTraitsText();
    }

    public void enableViews() {
        for (LinearLayout traitLayout : traitLayouts) {
            CollectActivity.enableViews(traitLayout);
        }
    }

    public void disableViews() {
        for (LinearLayout traitLayout : traitLayouts) {
            CollectActivity.disableViews(traitLayout);
        }
    }

    public void initCurrentVals() {
        getCurrentLayout().initCurrentVals();
    }

    void initTraitDetails() {
        if (prefixTraits != null) {
            traitDetails.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            traitDetails.setMaxLines(10);
                            break;
                        case MotionEvent.ACTION_UP:
                            traitDetails.setMaxLines(1);
                            break;
                    }
                    return true;
                }
            });
        }
    }

    public void setTrait(String trait) {
        Map newTraits = parent.getNewTraits();
        TraitObject tobj = dt.getDetail(trait);
        setTraitObject(tobj);
    }

    public boolean validateData() {
        if (etCurVal == null)
            return true;

        final String strValue = etCurVal.getText().toString();
        final String trait = traitObject.getTrait();

        if (parent.existsNewTraits()
                && traitObject != null
                && etCurVal.getText().toString().length() > 0
                && !traitObject.isValidValue(strValue)) {

            if (strValue.length() > 0 && traitObject.isOver(strValue)) {
                Utils.makeToast(parent.getApplicationContext(),
                        parent.getString(R.string.trait_error_maximum_value)
                        + ": " + traitObject.getMaximum());
            } else if (strValue.length() > 0 && traitObject.isUnder(strValue)) {
                Utils.makeToast(parent.getApplicationContext(),
                        parent.getString(R.string.trait_error_minimum_value)
                        + ": " + traitObject.getMinimum());
            }

            parent.removeTrait(traitObject);
            etCurVal.getText().clear();

            return false;
        }

        return true;

    }

    void setPrefixTraits() {
        prefixTraits = dt.getRangeColumnNames();
    }
}