package com.example.inakov.beaconobserver;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity {

    private TextView foundView;
    private TextView uuidView;
    private TextView majorView;
    private TextView minorView;
    private TextView accuracyView;
    private TextView distanceView;
    private TextView rssiView;

    private boolean scanning = false;

    private Button scanButton;

    private BluetoothLeScannerCompat scanner;

    public final static String mUuid = "01122334-4556-6778-899a-abbccddeeff0";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        foundView = (TextView) findViewById(R.id.found_data);
        uuidView = (TextView) findViewById(R.id.uuid_data);
        majorView = (TextView) findViewById(R.id.major_data);
        minorView = (TextView) findViewById(R.id.minor_data);
        accuracyView = (TextView) findViewById(R.id.accuracy_data);
        distanceView = (TextView) findViewById(R.id.distance_data);
        rssiView = (TextView) findViewById(R.id.rssi_data);
        scanButton = (Button) findViewById(R.id.scan_button);

        uuidView.setText(mUuid);

    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
//            final int rssiPercent = (int) (100.0f * (127.0f + device.rssi) / (127.0f + 20.0f));
            Log.e("ScanCallback", "onScanResult");
            Log.e("ScanCallback", "rssi: " + result.getRssi());
            Log.e("ScanCallback", "name: " + result.getScanRecord().getDeviceName());
            Log.e("ScanCallback", result.getScanRecord().getManufacturerSpecificData().toString());

        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {

            Log.e("ScanCallback", "results: " + results.size());
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    for(ScanResult result : results){
                        byte[] manufacturerData = result.getScanRecord().getManufacturerSpecificData(89);

                        long msb = ParserUtils.decodeHalfUuid(manufacturerData, 2);
                        long lsb = ParserUtils.decodeHalfUuid(manufacturerData, 10);
                        String uuidValue = new ParcelUuid(new UUID(msb, lsb)).toString();

                        String major = String.valueOf(ParserUtils.decodeUint16LittleEndian(manufacturerData, 18));
                        String minor = String.valueOf(ParserUtils.decodeUint16LittleEndian(manufacturerData, 20));
                        int calculatedRssiIn1m = manufacturerData[22];

                        uuidView.setText(uuidValue);
                        majorView.setText(major);
                        minorView.setText(minor);
                        rssiView.setText(String.valueOf(result.getRssi()));

                        double accuracy = getAccuracy((double) result.getRssi(), (double) calculatedRssiIn1m);

                        String distance = getDistance(accuracy);
                        distanceView.setText(distance);
                        accuracyView.setText(String.valueOf(accuracy));




                    }
                }
            });

            super.onBatchScanResults(results);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void scanButtonCallback(View view) {
        if(scanning){
            Log.i("MainActivity", "Stop scanning.");
            scanning = false;
            scanButton.setText(getString(R.string.button_scan));

            scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);
        }else{
            Log.i("MainActivity", "Start scanning.");
            scanning = true;
            scanButton.setText(getString(R.string.button_stop_scan));

            scanner = BluetoothLeScannerCompat.getScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000)
                    .setUseHardwareBatchingIfSupported(false).build();

            ByteBuffer bb = ByteBuffer.wrap(new byte[23]);
            bb.put((byte) 2);
            bb.put((byte) 21);
            UUID uuidFilter = UUID.fromString(mUuid);
            bb.putLong(uuidFilter.getMostSignificantBits());
            bb.putLong(uuidFilter.getLeastSignificantBits());

            // Add data array to filters
            ScanFilter filter = new ScanFilter.Builder().setManufacturerData(89, bb.array(), UUID_BEACON_MASK).build();

            List<ScanFilter> filters = new ArrayList<>();
            filters.add(filter);

            scanner.startScan(filters, settings, scanCallback);
        }
    }

    private String getDistance(Double accuracy) {
        if (accuracy == -1.0) {
            return "Unknown";
        } else if (accuracy < 1) {
            return "Immediate";
        } else if (accuracy < 3) {
            return "Near";
        } else {
            return "Far";
        }
    }

    public double getAccuracy(double rssi, double calculatedRssiIn1m) {
        if(rssi == 0.0F) {
            return -1.0F;
        } else {
            double ratio = rssi / calculatedRssiIn1m;
            return Math.max((float)Math.pow(ratio, 5.5D) - 5.0E-4F, 0.0F);
        }
    }

    private static final byte[] UUID_BEACON_MASK = new byte[]{(byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)-1, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};


}
