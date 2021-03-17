package com.fieldbook.tracker.activities;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.PostProcessor;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.os.strictmode.CleartextNetworkViolation;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;

import com.fieldbook.tracker.preferences.GeneralKeys;
import com.fieldbook.tracker.traits.LayoutCollection;
import com.fieldbook.tracker.R;
import com.fieldbook.tracker.brapi.Observation;
import com.fieldbook.tracker.adapters.InfoBarAdapter;
import com.fieldbook.tracker.database.DataHelper;
import com.fieldbook.tracker.objects.TraitObject;
import com.fieldbook.tracker.traits.PhotoTraitLayout;
import com.fieldbook.tracker.traits.BaseTraitLayout;
import com.fieldbook.tracker.utilities.Constants;
import com.fieldbook.tracker.objects.RangeObject;
import com.fieldbook.tracker.utilities.DialogUtils;
import com.fieldbook.tracker.utilities.Utils;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mikepenz.iconics.utils.IconicsUtils;

import org.threeten.bp.OffsetDateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.NonWritableChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.fieldbook.tracker.activities.ConfigActivity.dt;
import static java.lang.Math.min;

/**
 * All main screen logic resides here
 */

@SuppressLint("ClickableViewAccessibility")
public class TabletCollectActivity extends AppCompatActivity {

    public static boolean searchReload;
    public static String searchRange;
    public static String searchPlot;
    public static boolean reloadData;
    public static boolean partialReload;
    public static Activity thisActivity;
    public static String TAG = "Field Book";
    private static String displayColor = "#d50000";
    ImageButton deleteValue;
    ImageButton missingValue;
    /**
     * Trait layouts
     */
    ArrayList<LayoutCollection> traitLayoutTable = new ArrayList<LayoutCollection>();
    private SharedPreferences ep;
    private String inputPlotId = "";
    private AlertDialog goToId;
    private Object lock;
    private Map newTraits = new HashMap();  // { trait name: value }
    private LayoutCollection currentLayout;
    private TraitObject currentTrait;
    private String[] prefixTraits;
    private BarcodeKeyParser barcodeKeyParser = new BarcodeKeyParser();
    /**
     * Main screen elements
     */
    private Menu systemMenu;
    private InfoBarAdapter infoBarAdapter;
    private RangeBox rangeBox;
    /**
     * Trait-related elements
     */
    private TextView tvCurVal;

    /**
     * Traits
     */
    private String[] all_traits;
    private String[] traits;

    /**
     * Sound
     */
    SoundPlayer soundPlayer;

    /**
     * we have to distinguish from where we are using barcode
     */
    protected enum BarcodeTarget {
        PlotID, Value
    };
    private BarcodeTarget barcodeTarget;
    public void setBarcodeTargetValue() { barcodeTarget = BarcodeTarget.Value; }
    public void setBarcodeTargetPlotID() { barcodeTarget = BarcodeTarget.PlotID; }
    private boolean isBarcodeTargetValue() { return barcodeTarget == BarcodeTarget.Value; }
    private boolean isBarcodeTargetPlotID() { return barcodeTarget == BarcodeTarget.PlotID; }

    public final Handler myGuiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            synchronized (lock) {
                switch (msg.what) {
                    case 1:
                        ImageView btn = findViewById(msg.arg1);
                        if (btn.getTag() != null) {  // button is still pressed
                            // schedule next btn pressed check
                            Message msg1 = new Message();
                            msg1.copyFrom(msg);
                            if (msg.arg1 == R.id.rangeLeft) {
                                rangeBox.repeatKeyPress("left");
                            } else {
                                rangeBox.repeatKeyPress("right");
                            }
                            myGuiHandler.removeMessages(1);
                            myGuiHandler.sendMessageDelayed(msg1, msg1.arg2);
                        }
                        break;
                }
            }
        }
    };

    private TextWatcher cvText;
    private InputMethodManager imm;
    private Boolean dataLocked = false;

    public InputMethodManager getImm() { return imm; }

    public static void disableViews(ViewGroup layout) {
        layout.setEnabled(false);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                disableViews((ViewGroup) child);
            } else {
                child.setEnabled(false);
            }
        }
    }

    public static void enableViews(ViewGroup layout) {
        layout.setEnabled(false);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                enableViews((ViewGroup) child);
            } else {
                child.setEnabled(true);
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ep = getSharedPreferences("Settings", 0);
        if (ConfigActivity.dt == null) {    // when resume
            ConfigActivity.dt = new DataHelper(this);
        }

        ConfigActivity.dt.open();

        loadScreen();

        soundPlayer = new SoundPlayer();
    }

    private void initCurrentVals() {
        // Current value display
        tvCurVal = findViewById(R.id.tvCurVal);
        barcodeKeyParser.clear();
        Log.d("TabletCollectActivity", tvCurVal.getText().toString());
        
        tvCurVal.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                Log.d("onEditorAction", tvCurVal.getText().toString() + " : xxx");
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    rangeBox.rightClick();
                    return true;
                }

                return false;
            }
        });

        // Validates the text entered for text format
        cvText = new TextWatcher() {
            public void afterTextChanged(Editable en) {
                Log.d("TextWatcher", en.toString());
            }

            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
            }
        };

        tvCurVal.setRawInputType(InputType.TYPE_CLASS_TEXT);
        tvCurVal.setTextIsSelectable(true);

        // Validates the text entered for text format
        for (LayoutCollection layout : traitLayoutTable) {
            layout.initCurrentVals();
        }
    }

    private void loadScreen() {
        setContentView(R.layout.tablet_activity_collect);

        initToolbars();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // If the app is just starting up, we must always allow refreshing of data onscreen
        reloadData = true;

        lock = new Object();

        thisActivity = this;

        // Keyboard service manager
         setIMM();

        infoBarAdapter = new InfoBarAdapter(this, ep.getInt(GeneralKeys.INFOBAR_NUMBER, 2), (RecyclerView) findViewById(R.id.selectorList));

        // prepare TraitLayout in the cells of the TableLayout
        int[] holderIDs = {
                R.id.traitHolder1, R.id.traitHolder2, R.id.traitHolder3,
                R.id.traitHolder4, R.id.traitHolder5, R.id.traitHolder6,
                R.id.traitHolder7, R.id.traitHolder8, R.id.traitHolder9,
                R.id.traitHolder10, R.id.traitHolder11, R.id.traitHolder12
        };
        traits = dt.getVisibleTrait();
        all_traits = dt.getAllTraits();
        for (int i = 0; i < min(holderIDs.length, traits.length); ++i) {
            LinearLayout layout = findViewById(holderIDs[i]);
            LayoutCollection collection = new LayoutCollection(this, layout);
            traitLayoutTable.add(collection);
        }
        // set TraitObject into TraitLayout
        for (int i = 0; i < traitLayoutTable.size(); ++i) {
            if (i == traits.length)
                break;
            TraitObject traitObject = dt.getDetail(traits[i]);
            traitLayoutTable.get(i).setTraitObject(traitObject);
        }
        // hide residual layout
        for (int i = traitLayoutTable.size(); i < holderIDs.length; ++i) {
            LinearLayout layout = findViewById(holderIDs[i]);
            layout.setVisibility(View.INVISIBLE);
        }

        rangeBox = new RangeBox(this);
        initCurrentVals();
    }

    private void refreshMain() {
        rangeBox.saveLastPlot();
        rangeBox.refresh();
        setNewTraits(rangeBox.getPlotID());

        initWidgets(true);
    }

    public void playSound(String scene) {
        soundPlayer.play(scene);
    }

    private void setNaText() {
        for(LayoutCollection traitLayouts : traitLayoutTable)
            traitLayouts.setNaTraitsText();
    }

    private void setNaTextBrapiEmptyField() {
        for(LayoutCollection traitLayouts : traitLayoutTable)
            traitLayouts.setNaTraitsText();
    }

    private void initToolbars() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Toolbar toolbarBottom = findViewById(R.id.toolbarBottom);

        missingValue = toolbarBottom.findViewById(R.id.missingValue);
        missingValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(LayoutCollection layouts : traitLayoutTable) {
                    TraitObject currentTrait = layouts.getTraitObject();
                    updateTrait(currentTrait.getTrait(), currentTrait.getFormat(), "NA");
                    layouts.setNaTraitsText();
                }
            }
        });

        deleteValue = toolbarBottom.findViewById(R.id.deleteValue);
        deleteValue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // if a brapi observation that has been synced, don't allow deleting
                if (dt.isBrapiSynced(rangeBox.getPlotID(), currentTrait.getTrait())) {
                    if (currentTrait.getFormat().equals("photo")) {
                        // I want to use abstract method
                        for(LayoutCollection traitLayouts : traitLayoutTable) {
                            PhotoTraitLayout traitPhoto = traitLayouts.getPhotoTrait();
                            traitPhoto.brapiDelete(newTraits);
                        }
                    } else {
                        brapiDelete(currentTrait.getTrait(), false);
                    }
                } else {
                    for(int i = 0; i < traitLayoutTable.size(); ++i)
                        traitLayoutTable.get(i).deleteTraitListener(currentTrait.getFormat());
                }
            }
        });

    }

    // This update should only be called after repeating keypress ends
    private void repeatUpdate() {
        if (rangeBox.getRangeID() == null)
            return;

        setNewTraits(rangeBox.getPlotID());

        initWidgets(true);
    }

    // This is central to the application
    // Calling this function resets all the controls for traits, and picks one
    // to show based on the current trait data
    private void initWidgets(final boolean rangeSuppress) {
        // Reset dropdowns

        if (!dt.isTableEmpty(DataHelper.RANGE)) {
            final String plotID = rangeBox.getPlotID();
            infoBarAdapter.configureDropdownArray(plotID);
        }

        all_traits = dt.getAllTraits();
        traits = dt.getVisibleTrait();
        for (int i = 0; i < traitLayoutTable.size(); ++i) {
            traitLayoutTable.get(i).setTrait(traits[i]);
        }
    }

    // Moves to specific plot/range/plot_id
    private void moveToSearch(String type, int[] rangeID, String range, String plot, String data) {

        if (rangeID == null) {
            return;
        }

        boolean haveData = false;

        // search moveto
        if (type.equals("search")) {
            for (int j = 1; j <= rangeID.length; j++) {
                rangeBox.setRangeByIndex(j - 1);
                RangeObject cRange = rangeBox.getCRange();

                if (cRange.range.equals(range) & cRange.plot.equals(plot)) {
                    moveToResultCore(j);
                    haveData = true;
                }
            }
        }

        //move to plot
        else if (type.equals("plot")) {
            for (int j = 1; j <= rangeID.length; j++) {
                rangeBox.setRangeByIndex(j - 1);
                RangeObject cRange = rangeBox.getCRange();

                if (cRange.plot.equals(data)) {
                    moveToResultCore(j);
                    haveData = true;
                }
            }
        }

        //move to range
        else if (type.equals("range")) {
            for (int j = 1; j <= rangeID.length; j++) {
                rangeBox.setRangeByIndex(j - 1);
                RangeObject cRange = rangeBox.getCRange();

                if (cRange.range.equals(data)) {
                    moveToResultCore(j);
                    haveData = true;
                }
            }
        }

        //move to plot id
        else if (type.equals("id")) {
            for (int j = 1; j <= rangeID.length; j++) {
                rangeBox.setRangeByIndex(j - 1);
                RangeObject cRange = rangeBox.getCRange();

                if (cRange.plot_id.equals(data)) {
                    Log.d("moveToSearch", "****");
                    moveToResultCore(j);
                    return;
                }
            }
        }

        if (!haveData) {
            Utils.makeToast(getApplicationContext(), getString(R.string.main_toolbar_moveto_no_match));
        }
    }

    private void moveEntryById(String id) {
        rangeBox.setAllRangeID();
        int[] rangeID = rangeBox.getRangeID();
        moveToSearch("id", rangeID, null, null, id);
    }

    private void moveToResultCore(int j) {
        rangeBox.setPaging(j);

        // Reload traits based on selected plot
        rangeBox.display();
        setNewTraits(rangeBox.getPlotID());

        initWidgets(false);
    }

    @Override
    public void onPause() {
        // Backup database
        try {
            dt.exportDatabase("backup");
            File exportedDb = new File(Constants.BACKUPPATH + "/" + "backup.db");
            File exportedSp = new File(Constants.BACKUPPATH + "/" + "backup.db_sharedpref.xml");
            Utils.scanFile(TabletCollectActivity.this, exportedDb);
            Utils.scanFile(TabletCollectActivity.this, exportedSp);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        //save last plot id
        if (ep.getBoolean("ImportFieldFinished", false)) {
            rangeBox.saveLastPlot();
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update menu item visibility
        if (systemMenu != null) {
            systemMenu.findItem(R.id.help).setVisible(ep.getBoolean("Tips", false));
            systemMenu.findItem(R.id.jumpToPlot).setVisible(ep.getBoolean(GeneralKeys.UNIQUE_TEXT, false));
            systemMenu.findItem(R.id.nextEmptyPlot).setVisible(ep.getBoolean(GeneralKeys.NEXT_ENTRY_NO_DATA, false));
            systemMenu.findItem(R.id.barcodeScan).setVisible(ep.getBoolean(GeneralKeys.UNIQUE_CAMERA, false));
            systemMenu.findItem(R.id.datagrid).setVisible(ep.getBoolean(GeneralKeys.DATAGRID_SETTING, false));
        }

        // If reload data is true, it means there was an import operation, and
        // the screen should refresh
        if (ConfigActivity.dt == null) {
            ConfigActivity.dt = new DataHelper(this);
        }

        ConfigActivity.dt.open();

        if (reloadData) {
            reloadData = false;
            partialReload = false;

            // displayRange moved to RangeBox#display
            // and display in RangeBox#reload is commented out
            rangeBox.reload();
            setPrefixTraits();

            initWidgets(false);

            // try to go to last saved plot
            if (ep.getString("lastplot", null) != null) {
                rangeBox.setAllRangeID();
                int[] rangeID = rangeBox.getRangeID();
                moveToSearch("id", rangeID, null, null, ep.getString("lastplot", null));
            }

        } else if (partialReload) {
            partialReload = false;
            rangeBox.display();
            setPrefixTraits();
            initWidgets(false);

        } else if (searchReload) {
            searchReload = false;
            rangeBox.resetPaging();
            int[] rangeID = rangeBox.getRangeID();

            if (rangeID != null) {
                moveToSearch("search", rangeID, searchRange, searchPlot, null);
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d("onKeyUp", "KeyCode=" + keyCode);
        barcodeKeyParser.setKeyCode(keyCode);
        final String strCurVal = barcodeKeyParser.toString();
        tvCurVal.setText(strCurVal);

        // Wait a moment and see if any other keys have been sent
        // Keys are sent character by character
        // Wait a minute, and if the string hasn't changed, the key won't be sent anymore.
        Timer timer = new Timer();
        final long DELAY = 200; // in ms

        try {
            timer.cancel();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            final String tvVal = tvCurVal.getText().toString();
                            if (!strCurVal.equals(tvVal))   // entering
                                return;

                            parseBarcodeString(tvVal);
                        }
                    });
                }
            }, DELAY);
        } catch (Exception e) {
            Log.e(TAG,"" + e.getMessage());
        }

        Log.d("strCurval", strCurVal);
        if (strCurVal.isEmpty()) {
            return super.onKeyUp(keyCode, event);
        }
        return false;
    }

    private void parseBarcodeString(String s) {
        if (s.isEmpty())
            return;

        if (isValidBarcodeString(s)) {
            Log.d("onKeyUp", s);
            processBarcodeString();
        }
        else {
            Log.d("onKeyUp", "error");
            playSound("error");
            barcodeKeyParser.clear();
        }
    }

    private void processBarcodeString() {
        final String strCurVal = barcodeKeyParser.toString();
        final String[] v = strCurVal.split(":");
        if (isValidIDString(v[0], v[1])) {
            if (isAllTraitsEntered()) {
                moveEntryById(v[1]);
            }
            else {
                playSound("error2");
                Utils.makeToast(getApplicationContext(), getString(R.string.main_lack_values));
            }
        }
        else if (inputTraitValueByBarcode(v[0], v[1])) {
            playSound("success");
        }
        else {
            playSound("error");
        }
        barcodeKeyParser.clear();
    }

    private boolean inputTraitValueByBarcode(String key, String value) {
        final int trait_number =  getTraitNumberByBarcode(key);
        if (trait_number == 0) {
            return false;
        }

        final int traitIndex = allTraitIndexIntoTraitIndex(trait_number - 1);
        Log.d("traitIndex", String.valueOf(traitIndex));
        if (traitIndex == -1)
            return false;

        if (!inputTraitValue(traitIndex, value))
            return false;

        barcodeKeyParser.clear();
        return true;
    }

    private boolean isValidIDString(String key, String value) {
        return key.toLowerCase().equals("id") && !value.isEmpty();
    }

    private int getTraitNumberByBarcode(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isValidBarcodeString(String s) {
        // s must be form of *:*
        final int pos = s.indexOf(":");
        if (pos == -1)
            return false;

        final int pos2 = s.indexOf(":", pos + 1);
        return pos2 == -1 && 1 <= pos && pos < s.length() - 1;
    }

    protected boolean inputTraitValue(int traitIndex, String value) {
        if (traitIndex >= traitLayoutTable.size())
            return false;

        Log.d("traitIndex", String.valueOf(traitIndex));
        Log.d("value", value);
        LayoutCollection layoutCollections = traitLayoutTable.get(traitIndex);
        return layoutCollections.setValue(value);
    }

    private int allTraitIndexIntoTraitIndex(int i) {
        final String trait = all_traits[i];
        for (int j = 0; j < traits.length; ++j) {
            if (traits[j].equals(trait))
                return j;
        }
        return -1;
    }

    /**
     * Helper function update user data in the memory based hashmap as well as
     * the database
     */
    public void updateTrait(String parent, String trait, String value) {

        if (rangeBox == null || rangeBox.isEmpty()) {
            return;
        }

        Log.w(parent, value);

        Observation observation = dt.getObservation(rangeBox.getPlotID(), parent);
        String observationDbId = observation.getDbId();
        OffsetDateTime lastSyncedTime = observation.getLastSyncedTime();

        // Always remove existing trait before inserting again
        // Based on plot_id, prevent duplicates
        dt.deleteTrait(rangeBox.getPlotID(), parent);

        String exp_id = Integer.toString(ep.getInt("SelectedFieldExpId", 0));
        dt.insertUserTraits(rangeBox.getPlotID(), parent, trait, value,
                ep.getString("FirstName", "") + " " + ep.getString("LastName", ""),
                ep.getString("Location", ""), "", exp_id, observationDbId,
                lastSyncedTime);
    }

    private void brapiDelete(String parent, Boolean hint) {
        Toast.makeText(getApplicationContext(), getString(R.string.brapi_delete_message), Toast.LENGTH_LONG).show();
        updateTrait(parent, currentTrait.getFormat(), getString(R.string.brapi_na));
        if (hint) {
            setNaTextBrapiEmptyField();
        } else {
            setNaText();
        }
    }

    // Delete trait, including from database
    public void removeTrait(TraitObject traitObject) {
        if (rangeBox.isEmpty()) {
            return;
        }

        if (dt.isBrapiSynced(rangeBox.getPlotID(), traitObject.getTrait())) {
            brapiDelete(traitObject.getTrait(), true);
        } else {
            // Always remove existing trait before inserting again
            // Based on plot_id, prevent duplicate
            remove(traitObject.getTrait(), rangeBox.getPlotID());
        }
    }

    // for format without specific control
    public void removeTrait() {
        remove(currentTrait, rangeBox.getPlotID());
        setText("");
    }

    private void setText(final String string_value) {
        BaseTraitLayout layout= currentLayout.getCurrentLayout();
        EditText etCurVal = layout.getEtCurVal();
        if (etCurVal != null)
            etCurVal.setText(string_value);
    }

    private void customizeToolbarIcons() {
        Set<String> entries = ep.getStringSet(GeneralKeys.TOOLBAR_CUSTOMIZE, new HashSet<String>());

        if (systemMenu != null) {
            systemMenu.findItem(R.id.search).setVisible(entries.contains("search"));
            systemMenu.findItem(R.id.resources).setVisible(entries.contains("resources"));
            systemMenu.findItem(R.id.summary).setVisible(entries.contains("summary"));
            systemMenu.findItem(R.id.lockData).setVisible(entries.contains("lockData"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(TabletCollectActivity.this).inflate(R.menu.menu_main, menu);

        systemMenu = menu;

        systemMenu.findItem(R.id.help).setVisible(ep.getBoolean("Tips", false));
        systemMenu.findItem(R.id.jumpToPlot).setVisible(ep.getBoolean(GeneralKeys.UNIQUE_TEXT, false));
        systemMenu.findItem(R.id.nextEmptyPlot).setVisible(ep.getBoolean(GeneralKeys.NEXT_ENTRY_NO_DATA, false));
        systemMenu.findItem(R.id.barcodeScan).setVisible(ep.getBoolean(GeneralKeys.UNIQUE_CAMERA, false));
        systemMenu.findItem(R.id.datagrid).setVisible(ep.getBoolean(GeneralKeys.DATAGRID_SETTING, false));

        customizeToolbarIcons();

        lockData(dataLocked);

        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private TapTarget collectDataTapTargetView(int id, String title, String desc, int color, int targetRadius) {
        return TapTarget.forView(findViewById(id), title, desc)
                // All options below are optional
                .outerCircleColor(color)      // Specify a color for the outer circle
                .outerCircleAlpha(0.95f)            // Specify the alpha amount for the outer circle
                .targetCircleColor(R.color.black)   // Specify a color for the target circle
                .titleTextSize(30)                  // Specify the size (in sp) of the title text
                .descriptionTextSize(20)            // Specify the size (in sp) of the description text
                .descriptionTextColor(R.color.black)  // Specify the color of the description text
                .descriptionTypeface(Typeface.DEFAULT_BOLD)
                .textColor(R.color.black)            // Specify a color for both the title and description text
                .dimColor(R.color.black)            // If set, will dim behind the view with 30% opacity of the given color
                .drawShadow(true)                   // Whether to draw a drop shadow or not
                .cancelable(false)                  // Whether tapping outside the outer circle dismisses the view
                .tintTarget(true)                   // Whether to tint the target view's color
                .transparentTarget(true)           // Specify whether the target is transparent (displays the content underneath)
                .targetRadius(targetRadius);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent = new Intent(Intent.ACTION_VIEW);

        switch (item.getItemId()) {
            case R.id.help:
                TapTargetSequence sequence = new TapTargetSequence(this)
                        .targets(collectDataTapTargetView(R.id.selectorList, getString(R.string.tutorial_main_infobars_title), getString(R.string.tutorial_main_infobars_description), R.color.main_primaryDark,200),
                                collectDataTapTargetView(R.id.rangeLeft, getString(R.string.tutorial_main_entries_title), getString(R.string.tutorial_main_entries_description), R.color.main_primaryDark,60),
                                collectDataTapTargetView(R.id.valuesPlotRangeHolder, getString(R.string.tutorial_main_navinfo_title), getString(R.string.tutorial_main_navinfo_description), R.color.main_primaryDark,60),
                                collectDataTapTargetView(R.id.missingValue, getString(R.string.tutorial_main_na_title), getString(R.string.tutorial_main_na_description), R.color.main_primary,60),
                                collectDataTapTargetView(R.id.deleteValue, getString(R.string.tutorial_main_delete_title), getString(R.string.tutorial_main_delete_description), R.color.main_primary,60)
                        );
                if (systemMenu.findItem(R.id.search).isVisible()) {
                    sequence.target(collectDataTapTargetView(R.id.search, getString(R.string.tutorial_main_search_title), getString(R.string.tutorial_main_search_description), R.color.main_primaryDark,60));
                }
                if (systemMenu.findItem(R.id.resources).isVisible()) {
                    sequence.target(collectDataTapTargetView(R.id.resources, getString(R.string.tutorial_main_resources_title), getString(R.string.tutorial_main_resources_description), R.color.main_primaryDark,60));
                }
                if (systemMenu.findItem(R.id.summary).isVisible()) {
                    sequence.target(collectDataTapTargetView(R.id.summary, getString(R.string.tutorial_main_summary_title), getString(R.string.tutorial_main_summary_description), R.color.main_primaryDark,60));
                }
                if (systemMenu.findItem(R.id.lockData).isVisible()) {
                    sequence.target(collectDataTapTargetView(R.id.lockData, getString(R.string.tutorial_main_lockdata_title), getString(R.string.tutorial_main_lockdata_description), R.color.main_primaryDark,60));
                }

                sequence.start();
                break;
            case R.id.search:
                intent.setClassName(TabletCollectActivity.this,
                        SearchActivity.class.getName());
                startActivity(intent);
                break;

            case R.id.resources:
                intent.setClassName(TabletCollectActivity.this,
                        FileExploreActivity.class.getName());
                intent.putExtra("path", Constants.RESOURCEPATH);
                intent.putExtra("exclude", new String[]{"fieldbook"});
                intent.putExtra("title", getString(R.string.main_toolbar_resources));
                startActivityForResult(intent, 1);
                break;
            case R.id.nextEmptyPlot:
                nextEmptyPlot();
                break;
            case R.id.jumpToPlot:
                moveToPlotID();
                break;
            case R.id.barcodeScan:
                setBarcodeTargetPlotID();
                new IntentIntegrator(this)
                        .setPrompt(getString(R.string.main_barcode_text))
                        .setBeepEnabled(true)
                        .initiateScan();
                break;
            case R.id.summary:
                showSummary();
                break;
            case R.id.datagrid:
                intent.setClassName(TabletCollectActivity.this,
                        DatagridActivity.class.getName());
                startActivityForResult(intent, 2);
                break;
            case R.id.lockData:
                dataLocked = !dataLocked;
                lockData(dataLocked);
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void lockData(boolean lock) {
        if (lock) {
            systemMenu.findItem(R.id.lockData).setIcon(R.drawable.ic_tb_lock);
            missingValue.setEnabled(false);
            deleteValue.setEnabled(false);
            // etCurVal.setEnabled(false);
            for(int i = 0; i < traitLayoutTable.size(); ++i)
                traitLayoutTable.get(i).disableViews();
        } else {
            systemMenu.findItem(R.id.lockData).setIcon(R.drawable.ic_tb_unlock);
            missingValue.setEnabled(true);
            deleteValue.setEnabled(true);
            for(int i = 0; i < traitLayoutTable.size(); ++i)
                traitLayoutTable.get(i).enableViews();
        }
    }

    private void moveToPlotID() {
        LayoutInflater inflater = this.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_gotobarcode, null);
        final EditText barcodeId = layout.findViewById(R.id.barcodeid);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppAlertDialog);
        builder.setTitle(R.string.main_toolbar_moveto)
                .setCancelable(true)
                .setView(layout);

        builder.setPositiveButton(getString(R.string.dialog_go), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                inputPlotId = barcodeId.getText().toString();
                rangeBox.setAllRangeID();
                int[] rangeID = rangeBox.getRangeID();
                moveToSearch("id", rangeID, null, null, inputPlotId);
                goToId.dismiss();
            }
        });

        builder.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton(getString(R.string.main_toolbar_moveto_scan), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setBarcodeTargetPlotID();
                IntentIntegrator integrator = new IntentIntegrator(thisActivity);
                integrator.initiateScan();
            }
        });

        goToId = builder.create();
        goToId.show();
        DialogUtils.styleDialogs(goToId);

        android.view.WindowManager.LayoutParams langParams = goToId.getWindow().getAttributes();
        langParams.width = LayoutParams.MATCH_PARENT;
        goToId.getWindow().setAttributes(langParams);
    }

    public void nextEmptyPlot() {
        try {
            final int id = rangeBox.nextEmptyPlot();
            rangeBox.setRange(id);
            rangeBox.display();
            rangeBox.setLastRange();
            setNewTraits(rangeBox.getPlotID());
            initWidgets(true);
        } catch (Exception e) {

        }
    }

    private void showSummary() {
        LayoutInflater inflater = this.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_summary, null);
        TextView summaryText = layout.findViewById(R.id.field_name);
        summaryText.setText(createSummaryText(rangeBox.getPlotID()));

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppAlertDialog);
        builder.setTitle(R.string.preferences_appearance_toolbar_customize_summary)
                .setCancelable(true)
                .setView(layout);

        builder.setNegativeButton(getString(R.string.dialog_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });

        final AlertDialog summaryDialog = builder.create();
        summaryDialog.show();
        DialogUtils.styleDialogs(summaryDialog);

        android.view.WindowManager.LayoutParams params2 = summaryDialog.getWindow().getAttributes();
        params2.width = LayoutParams.MATCH_PARENT;
        summaryDialog.getWindow().setAttributes(params2);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (ep.getBoolean(GeneralKeys.VOLUME_NAVIGATION, false)) {
                    if (action == KeyEvent.ACTION_UP) {
                        rangeBox.moveEntryRight();
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (ep.getBoolean(GeneralKeys.VOLUME_NAVIGATION, false)) {
                    if (action == KeyEvent.ACTION_UP) {
                        rangeBox.moveEntryLeft();
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_ENTER:
                String return_action = ep.getString(GeneralKeys.RETURN_CHARACTER, "0");

                if (return_action.equals("0")) {
                    if (action == KeyEvent.ACTION_UP) {
//                        InputTraitValueByBarcode();
//                        rangeBox.moveEntryRight();
                        return false;
                    }
                }

                if (return_action.equals("1")) {
                    if (action == KeyEvent.ACTION_UP) {
                        // moveTrait("right");
                        return true;
                    }
                }

                if (return_action.equals("2")) {
                    return true;
                }

                return false;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    String mChosenFileString = data.getStringExtra("result");
                    File mChosenFile = new File(mChosenFileString);

                    String suffix = mChosenFileString.substring(mChosenFileString.lastIndexOf('.') + 1).toLowerCase();

                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
                    Intent open = new Intent(Intent.ACTION_VIEW);
                    open.setDataAndType(FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".fileprovider", mChosenFile), mime);

                    startActivity(open);
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    inputPlotId = data.getStringExtra("result");
                    rangeBox.setAllRangeID();
                    int[] rangeID = rangeBox.getRangeID();
                    moveToSearch("id", rangeID, null, null, inputPlotId);
                }
                break;
            case 252:
                if (resultCode == RESULT_OK) {
                    for(int i = 0; i < traitLayoutTable.size(); ++i) {
                        LayoutCollection traitLayouts = traitLayoutTable.get(i);
                        PhotoTraitLayout traitPhoto = traitLayouts.getPhotoTrait();
                        if (traitPhoto.isPhotoTaken()) {
                            Log.d("onActivityResult", String.valueOf(i));
                            traitPhoto.makeImage(traitPhoto.getTraitObject(), newTraits);
                        }
                    }
                }
                break;
        }

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result == null) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        else if (isBarcodeTargetPlotID()) {
            if (!isAllTraitsEntered()) {
                playSound("error2");
                return;
            }

            inputPlotId = result.getContents();
            rangeBox.setAllRangeID();
            int[] rangeID = rangeBox.getRangeID();
            moveToSearch("id", rangeID, null, null, inputPlotId);
        }
        else if (isBarcodeTargetValue()) {
            final TraitObject trait = currentTrait;
            final String value = result.getContents();
            if (trait.isValidValue(value)) {
                setText(value);
            }
            else {
                String message = String.format("%s is invalid data.", value);
                Utils.makeToast(getApplicationContext(), message);
            }
        }
    }

    private boolean isAllTraitsEntered() {
        for (LayoutCollection layout : traitLayoutTable) {
            Log.d("isAllTraitsEntered", String.valueOf(layout.isEntered()));
            if (!layout.isEntered())
                return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public InputMethodManager getIMM() {
        return imm;
    }

    public void setIMM() {
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

    }

    public RangeBox getRangeBox() {
        return rangeBox;
    }

    public RangeObject getCRange() {
        return rangeBox.getCRange();
    }

    public String getDisplayColor() {
        return displayColor;
    }

    public ImageButton getDeleteValue() {
        return deleteValue;
    }

    public ImageView getRangeLeft() {
        return rangeBox.getRangeLeft();
    }

    public ImageView getRangeRight() {
        return rangeBox.getRangeRight();
    }

    public Boolean isDataLocked() {
        return dataLocked;
    }

    public SharedPreferences getPreference() {
        return ep;
    }

    public boolean is_cycling_traits_advances() {
        return ep.getBoolean(GeneralKeys.CYCLING_TRAITS_ADVANCES, false);
    }

    public boolean existsTrait(final int ID) {
        return dt.getTraitExists(ID, currentTrait.getTrait(), currentTrait.getFormat());
    }

    boolean existsTrait() {
        return newTraits.containsKey(currentTrait.getTrait());
    }

    public Map getNewTraits() { return newTraits; }

    public void setNewTraits(final String plotID) {
        newTraits = (HashMap) dt.getUserDetail(plotID).clone();
    }

    void setPrefixTraits() {
        prefixTraits = dt.getRangeColumnNames();
    }

    public boolean existsNewTraits() { return newTraits != null; }

    public void remove(String traitName, String plotID) {
        if (newTraits.containsKey(traitName))
            newTraits.remove(traitName);
        dt.deleteTrait(plotID, traitName);
    }

    public void remove(TraitObject trait, String plotID) {
        remove(trait.getTrait(), plotID);
    }

    final String createSummaryText(final String plotID) {
        StringBuilder data = new StringBuilder();

        //TODO this test crashes app
        if (rangeBox.getCRange() != null) {
            for (String s : prefixTraits) {
                data.append(s).append(": ");
                data.append(dt.getDropDownRange(s, plotID)[0]).append("\n");
            }
        }

        for (String s : all_traits) {
            if (newTraits.containsKey(s)) {
                data.append(s).append(": ");
                data.append(newTraits.get(s).toString()).append("\n");
            }
        }
        return data.toString();
    }

    private boolean validateData() {
        for (LayoutCollection layout : traitLayoutTable) {
            if (!layout.validateData()) {
                playSound("error");
                return false;
            }
        }
        return true;
    }

    ///// class RangeBox /////

    public class RangeBox {
        private TabletCollectActivity parent;
        private int[] rangeID;
        private int paging;

        private RangeObject cRange;
        private String lastRange;

        private TextView rangeName;
        private TextView plotName;

        private EditText range;
        private EditText plot;
        private TextView tvRange;
        private TextView tvPlot;

        private ImageView rangeLeft;
        private ImageView rangeRight;

        private Handler repeatHandler;

        private int delay = 100;
        private int count = 1;

        RangeBox(TabletCollectActivity parent_) {
            parent = parent_;
            rangeID = null;
            cRange = new RangeObject();
            cRange.plot = "";
            cRange.plot_id = "";
            cRange.range = "";
            lastRange = "";

            initAndPlot();
        }

        // getter
        RangeObject getCRange() {
            return cRange;
        }

        int[] getRangeID() {
            return rangeID;
        }

        int getRangeIDByIndex(int j) {
            return rangeID[j];
        }

        ImageView getRangeLeft() {
            return rangeLeft;
        }

        ImageView getRangeRight() {
            return rangeRight;
        }

        final String getPlotID() {
            return cRange.plot_id;
        }

        boolean isEmpty() {
            return cRange == null || cRange.plot_id.length() == 0;
        }

        private void initAndPlot() {
            range = findViewById(R.id.range);
            plot = findViewById(R.id.plot);

            rangeName = findViewById(R.id.rangeName);
            plotName = findViewById(R.id.plotName);

            rangeLeft = findViewById(R.id.rangeLeft);
            rangeRight = findViewById(R.id.rangeRight);

            tvRange = findViewById(R.id.tvRange);
            tvPlot = findViewById(R.id.tvPlot);

            rangeLeft.setOnTouchListener(createOnLeftTouchListener());
            rangeRight.setOnTouchListener(createOnRightTouchListener());

            // Go to previous range
            rangeLeft.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    moveEntryLeft();
                }
            });

            // Go to next range
            rangeRight.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    moveEntryRight();
                }
            });

            range.setOnEditorActionListener(createOnEditorListener(range,"range"));
            plot.setOnEditorActionListener(createOnEditorListener(plot,"plot"));

            range.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    range.setCursorVisible(true);
                    return false;
                }
            });

            plot.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    plot.setCursorVisible(true);
                    return false;
                }
            });

            setName(10);

            rangeName.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Utils.makeToast(getApplicationContext(),ep.getString("ImportFirstName", getString(R.string.search_results_dialog_range)));
                    return false;
                }
            });

            plotName.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Utils.makeToast(getApplicationContext(),ep.getString("ImportSecondName", getString(R.string.search_results_dialog_range)));
                    return false;
                }
            });
        }

        private String truncate(String s, int maxLen) {
            if (s.length() > maxLen)
                return s.substring(0, maxLen - 1) + ":";
            return s;
        }

        private OnEditorActionListener createOnEditorListener(final EditText edit, final String searchType) {
            return new OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // do not do bit check on event, crashes keyboard
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        try {
                            moveToSearch(searchType, rangeID, null, null, view.getText().toString());
                            InputMethodManager imm = parent.getIMM();
                            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
                        } catch (Exception ignore) {
                        }
                        return true;
                    }

                    return false;
                }
            };
        }

        private Runnable createRunnable(final String directionStr) {
            return new Runnable() {
                @Override
                public void run() {
                    repeatKeyPress(directionStr);

                    if ((count % 5) == 0) {
                        if (delay > 20) {
                            delay = delay - 10;
                        }
                    }

                    count++;
                    if (repeatHandler != null) {
                        repeatHandler.postDelayed(this, delay);
                    }
                }
            };
        }

        private OnTouchListener createOnLeftTouchListener() {
            Runnable actionLeft = createRunnable("left");
            return createOnTouchListener(rangeLeft, actionLeft,
                    R.drawable.main_entry_left_pressed,
                    R.drawable.main_entry_left_unpressed);
        }

        private OnTouchListener createOnRightTouchListener() {
            Runnable actionRight = createRunnable("right");
            return createOnTouchListener(rangeRight, actionRight,
                    R.drawable.main_entry_right_pressed,
                    R.drawable.main_entry_right_unpressed);
        }

        private OnTouchListener createOnTouchListener(final ImageView control,
                                                      final Runnable action, final int imageID, final int imageID2) {
            return new OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            control.setImageResource(imageID);
                            control.performClick();

                            if (repeatHandler != null) {
                                return true;
                            }
                            repeatHandler = new Handler();
                            repeatHandler.postDelayed(action, 750);

                            delay = 100;
                            count = 1;

                            break;
                        case MotionEvent.ACTION_MOVE:
                            break;
                        case MotionEvent.ACTION_UP:
                            control.setImageResource(imageID2);

                            if (repeatHandler == null) {
                                return true;
                            }
                            repeatHandler.removeCallbacks(action);
                            repeatHandler = null;

                            repeatUpdate();
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            control.setImageResource(imageID2);

                            repeatHandler.removeCallbacks(action);
                            repeatHandler = null;

                            v.setTag(null); // mark btn as not pressed
                            break;
                    }

                    // return true to prevent calling btn onClick handler
                    return true;
                }
            };
        }

        // Simulate range right key press
        private void repeatKeyPress(final String directionStr) {
            boolean left = directionStr.equalsIgnoreCase("left");

            if (!validateData()) {
                return;
            }

            if (rangeID != null && rangeID.length > 0) {
                final int step = left ? -1 : 1;
                paging = movePaging(paging, step, true);

                // Refresh onscreen controls
                cRange = dt.getRange(rangeID[paging - 1]);
                rangeBox.saveLastPlot();

                if (cRange.plot_id.length() == 0)
                    return;

                final SharedPreferences ep = parent.getPreference();
                if (ep.getBoolean(GeneralKeys.PRIMARY_SOUND, false)) {
                    if (!cRange.range.equals(lastRange) && !lastRange.equals("")) {
                        lastRange = cRange.range;

                        try {
                            int resID = getResources().getIdentifier("plonk", "raw", getPackageName());
                            MediaPlayer chimePlayer = MediaPlayer.create(TabletCollectActivity.this, resID);
                            chimePlayer.start();

                            chimePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                public void onCompletion(MediaPlayer mp) {
                                    mp.release();
                                }
                            });
                        } catch (Exception ignore) {
                        }
                    }
                }

                rangeBox.display();
                setNewTraits(rangeBox.getPlotID());

                initWidgets(true);
            }
        }

        void reload() {
            final SharedPreferences ep = parent.getPreference();
            switchVisibility(ep.getBoolean(GeneralKeys.QUICK_GOTO, false));

            setName(8);

            paging = 1;

            setAllRangeID();
            if (rangeID != null) {
                cRange = dt.getRange(rangeID[0]);

                //TODO NullPointerException
                lastRange = cRange.range;
                display();

                setNewTraits(cRange.plot_id);
            }
        }

        // Refresh onscreen controls
        void refresh() {
            cRange = dt.getRange(rangeID[paging - 1]);

            display();
            final SharedPreferences ep = parent.getPreference();
            if (ep.getBoolean(GeneralKeys.PRIMARY_SOUND, false)) {
                if (!cRange.range.equals(lastRange) && !lastRange.equals("")) {
                    lastRange = cRange.range;
                    playSound("plonk");
                }
            }
        }

        // Updates the data shown in the dropdown
        private void display() {
            if (cRange == null)
                return;

            range.setText(cRange.range);
            plot.setText(cRange.plot);

            range.setCursorVisible(false);
            plot.setCursorVisible(false);

            tvRange.setText(cRange.range);
            tvPlot.setText(cRange.plot);
        }

        public void rightClick() {
            rangeRight.performClick();
        }

        private void saveLastPlot() {
            final SharedPreferences ep = parent.getPreference();
            Editor ed = ep.edit();
            ed.putString("lastplot", cRange.plot_id);
            ed.apply();
        }

        void switchVisibility(boolean textview) {
            if (textview) {
                tvRange.setVisibility(TextView.GONE);
                tvPlot.setVisibility(TextView.GONE);
                range.setVisibility(EditText.VISIBLE);
                plot.setVisibility(EditText.VISIBLE);
            } else {
                tvRange.setVisibility(TextView.VISIBLE);
                tvPlot.setVisibility(TextView.VISIBLE);
                range.setVisibility(EditText.GONE);
                plot.setVisibility(EditText.GONE);
            }
        }

        public void setName(int maxLen) {
            final SharedPreferences ep = parent.getPreference();
            String primaryName = ep.getString("ImportFirstName", getString(R.string.search_results_dialog_range)) + ":";
            String secondaryName = ep.getString("ImportSecondName", getString(R.string.search_results_dialog_plot)) + ":";
            rangeName.setText(truncate(primaryName, maxLen));
            plotName.setText(truncate(secondaryName, maxLen));
        }

        void setAllRangeID() {
            rangeID = dt.getAllRangeID();
        }

        public void setRange(final int id) {
            cRange = dt.getRange(id);
        }

        void setRangeByIndex(final int j) {
            cRange = dt.getRange(rangeID[j]);
        }

        void setLastRange() {
            lastRange = cRange.range;
        }

        ///// paging /////

        private void moveEntryLeft() {
            final SharedPreferences ep = parent.getPreference();

            if (!validateData()) {
                return;
            }

            if (ep.getBoolean(GeneralKeys.ENTRY_NAVIGATION_SOUND, false)
                    && !existsTrait()) {
                playSound("error");
            } else {
                if (rangeID != null && rangeID.length > 0) {
                    //index.setEnabled(true);
                    paging = decrementPaging(paging);
                    parent.refreshMain();
                }
            }
        }

        private void moveEntryRight() {
            final SharedPreferences ep = parent.getPreference();

            if (!validateData()) {
                return;
            }

            if (ep.getBoolean(GeneralKeys.ENTRY_NAVIGATION_SOUND, false)
                    && !existsTrait()) {
                playSound("error");
            } else {
                if (rangeID != null && rangeID.length > 0) {
                    //index.setEnabled(true);
                    paging = incrementPaging(paging);
                    parent.refreshMain();
                }
            }
        }

        private int decrementPaging(int pos) {
            return movePaging(pos, -1, false);
        }

        private int incrementPaging(int pos) {
            return movePaging(pos, 1, false);
        }

        private int movePaging(int pos, int step, boolean cyclic) {
            // If ignore existing data is enabled, then skip accordingly
            final SharedPreferences ep = parent.getPreference();

            if (ep.getBoolean(GeneralKeys.HIDE_ENTRIES_WITH_DATA, false)) {
                if (step == 1 && pos == rangeID.length) {
                    return 1;
                }

                final int prevPos = pos;
                while (true) {
                    pos = moveSimply(pos, step);
                    // absorb the differece
                    // between single click and repeated clicks
                    if (cyclic) {
                        if (pos == prevPos) {
                            return pos;
                        } else if (pos == 1) {
                            pos = rangeID.length;
                        } else if (pos == rangeID.length) {
                            pos = 1;
                        }
                    } else {
                        if (pos == 1 || pos == prevPos) {
                            return pos;
                        }
                    }

                    if (!parent.existsTrait(rangeID[pos - 1])) {
                        return pos;
                    }
                }
            } else {
                return moveSimply(pos, step);
            }
        }

        private int moveSimply(int pos, int step) {
            pos += step;
            if (pos > rangeID.length) {
                return 1;
            } else if (pos < 1) {
                return rangeID.length;
            } else {
                return pos;
            }
        }

        void resetPaging() {
            paging = 1;
        }

        void setPaging(int j) {
            paging = j;
        }

        final int nextEmptyPlot() throws Exception {
            int pos = paging;

            if (pos == rangeID.length) {
                throw new Exception();
            }

            while (pos <= rangeID.length) {
                pos += 1;

                if (pos > rangeID.length) {
                    throw new Exception();
                }

                if (!parent.existsTrait(rangeID[pos - 1])) {
                    paging = pos;
                    return rangeID[pos - 1];
                }
            }
            throw new Exception();      // not come here
        }

        void clickLeft() {
            rangeLeft.performClick();
        }

        void clickRight() {
            rangeRight.performClick();
        }
    }

    private class BarcodeKeyParser {
        private String buffer;
        private String prevModifier;

        public BarcodeKeyParser() {
            clear();
        }

        public void clear() {
            buffer = "";
            prevModifier = "";
        }

        public String toString() {
            return buffer;
        }

        public void setKeyCode(int keyCode) {
            Log.d("setKeyCode", String.valueOf(keyCode));
            if (keyCode == 59) {                   // before capital
                prevModifier = "shift";
                return;
            }
            else if (7 <= keyCode && keyCode <= 16) {   // digit
                buffer += String.valueOf(keyCode - 7);
            }
            else if (keyCode == 74) {
                buffer += ":";
            }
            else if (29 <= keyCode && keyCode <= 54) {  // alphabet
                Log.d("setKeyCode", String.valueOf(keyCode) + " " + prevModifier);
                if (prevModifier.equals("shift")) {
                    final char[] c = Character.toChars(keyCode + 36);
                    buffer += new String(c);
                }
                else {
                    final char[] c = Character.toChars(keyCode + 68);
                    buffer += new String(c);
                }
            }
            prevModifier = "";
        }
    }

    class SoundPlayer {
        private HashMap<String, AudioTrackData> audioTracks;

        public SoundPlayer() {
            audioTracks = new HashMap<>();
            audioTracks.put("success", new AudioTrackData(R.raw.button57));
            audioTracks.put("error", new AudioTrackData(R.raw.button18));
            audioTracks.put("error2", new AudioTrackData(R.raw.button67));
        }

        public void play(String scene) {
            if (!audioTracks.containsKey(scene))
                return;

            AudioTrackData audioTrackData = audioTracks.get(scene);
            audioTrackData.play();
        }
    }

    class AudioTrackData {
        static final int SamplingRate = 32000;

        AudioTrack audioTrack;
        private byte[] wavData;

        public AudioTrackData(int soundResourceID) {
            wavData = readWavData(soundResourceID);
            audioTrack = createAudioTrack();
        }

        private byte[] readWavData(int soundResourceID) {
            byte[] wavData_ = null;
            InputStream input = null;
            try {
                // wavを読み込む
                input = getResources().openRawResource(soundResourceID);
                wavData_ = new byte[input.available()];

                // input.read(wavData)
                String readBytes = String.format(
                        Locale.US, "read bytes = %d", input.read(wavData_));
                // input.read(wavData)のwarning回避のためだけ
                input.close();
            } catch (FileNotFoundException fne) {
                fne.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    if (input != null) input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return wavData_;
        }

        private AudioTrack createAudioTrack() {
            // バッファサイズの計算
            int bufSize = android.media.AudioTrack.getMinBufferSize(
                    SamplingRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            // AudioTrack.Builder API level 26より
            AudioTrack audioTrack_ = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                audioTrack_ = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SamplingRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(bufSize)
                        .build();
            }
            return audioTrack_;
        }

        public AudioTrack getAudioTrack() { return audioTrack; }
        public byte[] getWavData() { return wavData; }

        public void play() {
            if (audioTrack == null || wavData == null)
                return;

            // 再生
            audioTrack.play();

            // ヘッダ44byteをオミット
            audioTrack.write(wavData, 44, wavData.length - 44);
        }
    }
}