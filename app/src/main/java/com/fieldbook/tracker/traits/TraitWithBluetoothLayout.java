package com.fieldbook.tracker.traits;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.activities.CollectActivity;
import com.google.zxing.integration.android.IntentIntegrator;

import java.util.ArrayList;

import static com.fieldbook.tracker.activities.CollectActivity.thisActivity;

public class TraitWithBluetoothLayout extends BaseTraitLayout {

    private ListView listDevice;

    private ArrayList<BluetoothDevice> devices;
    private BluetoothDevice currentDevice;

    private BluetoothAdapter bluetoothAdapter;
    private IntentFilter intentFilter;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> deviceNames;

    public TraitWithBluetoothLayout(Context context) {
        super(context);
    }

    public TraitWithBluetoothLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TraitWithBluetoothLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setNaTraitsText() {
    }

    @Override
    public String type() {
        return "with_bluetooth";
    }

    @Override
    public void init() {
        currentDevice = null;
        ImageButton getBarcode = findViewById(R.id.inputWithBluetooth);
        listDevice = findViewById(R.id.device_list);
        // Get Barcode
        getBarcode.setOnClickListener(new OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View arg0) {
                Log.d("TraitWithBluetoothLayout", "onClick 1");
                if (currentDevice == null) {
                    Log.d("TraitWithBluetoothLayout", "onClick 2");
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    devices = new ArrayList<BluetoothDevice>();
                    deviceNames = new ArrayList<String>();
                    setAdapters();
                    setListLister();

                    findBluetoothDevices();
                }
                /*
                ((CollectActivity)thisActivity).setBarcodeTarget(CollectActivity.BarcodeTarget.Value);
                IntentIntegrator integrator = new IntentIntegrator(thisActivity);
                integrator.initiateScan();
                 */
            }
        });
    }

    protected void setAdapters() {
        adapter = new ArrayAdapter<String>(thisActivity, android.R.layout.simple_list_item_1, deviceNames);
        listDevice.setAdapter(adapter);
    }

    protected void setListLister() {
        listDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentDevice = devices.get(position);
                String msg = currentDevice.getName();
                Toast.makeText(thisActivity.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void loadLayout() {

        getEtCurVal().setVisibility(EditText.VISIBLE);

        if (getNewTraits().containsKey(getCurrentTrait().getTrait())) {
            getEtCurVal().setText(getNewTraits().get(getCurrentTrait().getTrait()).toString());
            getEtCurVal().setTextColor(Color.parseColor(getDisplayColor()));
        } else {
            getEtCurVal().setText("");
            getEtCurVal().setTextColor(Color.BLACK);

            if (getCurrentTrait().getDefaultValue() != null
                    && getCurrentTrait().getDefaultValue().length() > 0)
                getEtCurVal().setText(getCurrentTrait().getDefaultValue());
        }

        ListView listView = findViewById(R.id.device_list);
        listView.setVisibility(INVISIBLE);
    }

    @Override
    public void deleteTraitListener() {
        ((CollectActivity) getContext()).removeTrait();
    }

    protected void findBluetoothDevices() {
        BtBroadcastReceiver btBroadcastReceiver = new BtBroadcastReceiver();

        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        thisActivity.registerReceiver(btBroadcastReceiver, intentFilter);

        if (bluetoothAdapter.startDiscovery()) {
            ;
        }
        else {
            Toast toast = Toast.makeText(thisActivity.getApplicationContext(), "can't find.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }
    }

    class BtBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity", intent.getAction());
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (name == null)
                    return;
                Log.d("MainActivity", name);
                if (!deviceNames.contains(name)) {
                    devices.add(device);
                    adapter.add(name);
                    Log.d("MainActivity", String.valueOf(devices.size()));
                }
            }
        }
    }
}
