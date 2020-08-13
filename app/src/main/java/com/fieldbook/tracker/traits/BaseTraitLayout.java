package com.fieldbook.tracker.traits;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fieldbook.tracker.activities.TabletCollectActivity;
import com.fieldbook.tracker.objects.RangeObject;
import com.fieldbook.tracker.objects.TraitObject;

import java.util.Map;

public abstract class BaseTraitLayout extends LinearLayout {
    public BaseTraitLayout(Context context) {
        super(context);
    }

    public BaseTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract String type();  // return trait type

    public boolean isTraitType(String trait) {
        return trait.equals(type());
    }

    public abstract void init();

    public abstract void loadLayout();

    public abstract void deleteTraitListener();

    public abstract void setNaTraitsText();

    public Map getNewTraits() {
        return ((TabletCollectActivity) getContext()).getNewTraits();
    }

    public TraitObject getCurrentTrait() {
        return ((TabletCollectActivity) getContext()).getCurrentTrait();
    }

    public SharedPreferences getPrefs() {
        return getContext().getSharedPreferences("Settings", 0);
    }

    public RangeObject getCRange() {
        return ((TabletCollectActivity) getContext()).getCRange();
    }

    public EditText getEtCurVal() {
        return ((TabletCollectActivity) getContext()).getEtCurVal();
    }

    public TextWatcher getCvText() {
        return ((TabletCollectActivity) getContext()).getCvText();
    }

    public String getDisplayColor() {
        return ((TabletCollectActivity) getContext()).getDisplayColor();
    }

    public void updateTrait(String parent, String trait, String value) {
        ((TabletCollectActivity) getContext()).updateTrait(parent, trait, value);
    }

    public void removeTrait(String parent) {
        ((TabletCollectActivity) getContext()).removeTrait(parent);
    }
}