package com.fieldbook.tracker.traits;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fieldbook.tracker.activities.TabletCollectActivity;
import com.fieldbook.tracker.objects.RangeObject;
import com.fieldbook.tracker.objects.TraitObject;
import com.google.android.material.tabs.TabLayout;

import java.util.HashMap;
import java.util.Map;

import static com.fieldbook.tracker.activities.ConfigActivity.dt;

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

    protected TraitObject traitObject = null;
    protected TabletCollectActivity parent = null;
    protected LayoutCollection cell = null;

    public void setTraitObject(TraitObject tobj) { traitObject = tobj; }
    public void setParent(TabletCollectActivity _activity, LayoutCollection _cell) {
        parent = _activity;
        cell = _cell;
    }

    public abstract String type();  // return trait type

    public boolean isTraitType(String format) {
        return type().equals(format);
    }

    public abstract void init();

    public abstract void loadLayout();

    public abstract void deleteTraitListener();

    public abstract void setNaTraitsText();

    public abstract boolean isEntered();

    public SharedPreferences getPrefs() {
        return getContext().getSharedPreferences("Settings", 0);
    }

    public RangeObject getCRange() {
        return ((TabletCollectActivity) getContext()).getCRange();
    }

    public TraitObject getTraitObject() {
        return traitObject;
    }

    protected Map getNewTraits() {
        return parent.getNewTraits();
    }

    protected void setNewTraits(final String plotID) {
        parent.setNewTraits(plotID);
    }

    protected boolean existsNewTraits() {
        return parent.existsNewTraits();
    }

    public EditText getEtCurVal() { return cell.getEtCurVal(); }

    public String getDisplayColor() {
        String hexColor = String.format("#%06X", (0xFFFFFF & getPrefs().getInt("SAVED_DATA_COLOR", Color.parseColor("#d50000"))));

        return hexColor;
    }

    public void updateTrait(String _parent, String trait, String value) {
        parent.updateTrait(_parent, trait, value);
    }

    public void removeTrait(TraitObject traitObject) {
        parent.removeTrait(traitObject);
    }

    public boolean validateData() { return true; }

    public void initCurrentVals() { }

    public boolean setValue(String s) { return false; }
}