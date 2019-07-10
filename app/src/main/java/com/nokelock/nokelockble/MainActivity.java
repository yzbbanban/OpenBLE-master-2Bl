package com.nokelock.nokelockble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.nokelock.adapter.DeviceAdapter;
import com.nokelock.bean.BleDevice;
import com.nokelock.constant.ExtraConstant;
import com.nokelock.service.BluetoothLeService;
import com.nokelock.utils.MPermissionsActivity;
import com.nokelock.utils.ParseLeAdvData;
import com.nokelock.utils.SampleGattAttributes;
import com.nokelock.utils.SortComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class MainActivity extends MPermissionsActivity {

    private static final String TAG = "MainActivity";

    private TextView tvRefresh;
    private boolean isRefreshing = false;

    private List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();
    private List<BleDevice> bleDeviceList = new ArrayList<>();
    private List<BleDevice> adapterList = new ArrayList<>();
    private BleDevice bleDevice;
    private DeviceAdapter adapter;
    private Comparator comp;

    private String exName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBLE();
        initWidget();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshDevice();
            }
        }, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (connection != null) {
            unbindService(connection);
            connection = null;
        }
    }

    private void initBLE() {
        exName = getIntent().getStringExtra(ExtraConstant.NAME);
        Log.i(TAG, "initBLE: "+exName);
        boolean bindService = bindService(new Intent(this, BluetoothLeService.class), connection, Context.BIND_AUTO_CREATE);
        if (bindService) {
            Log.w(MainActivity.class.getSimpleName(), "蓝牙初始化成功");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                System.out.println("蓝牙已打开");
                refreshDevice();
            } else if (resultCode == RESULT_CANCELED) {
                System.out.println("取消打开");
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothLeService bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothLeService.initBluetooth()) {
                App.getInstance().setBluetoothLeService(bluetoothLeService);
                BluetoothAdapter bluetoothAdapter = bluetoothLeService.getmBluetoothAdapter();
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBT, 1);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            App.getInstance().setBluetoothLeService(null);
        }
    };

    private void initWidget() {
        comp = new SortComparator();
        ListView listView = (ListView) findViewById(R.id.recycler_view);
        tvRefresh = (TextView) findViewById(R.id.tv_refresh);
        Button btRefresh = (Button) findViewById(R.id.bt_refresh);
        adapter = new DeviceAdapter(this, adapterList);
        Toast.makeText(this, "==>" + new Gson().toJson(adapterList), Toast.LENGTH_LONG).show();
        listView.setAdapter(adapter);
        new Thread(new DeviceThread()).start();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App.getInstance().getBluetoothLeService().getmBluetoothAdapter().stopLeScan(leScanCallback);
                isRefreshing = false;
                tvRefresh.setText("扫描结束");
                BleDevice bluetoothDevice = adapterList.get(position);
                String name = bluetoothDevice.getDevice().getName();
                if (TextUtils.isEmpty(name)) {
                    name = "null";
                }
                String address = bluetoothDevice.getDevice().getAddress();
                Intent intent = new Intent(MainActivity.this, LockManageActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("address", address);
                startActivity(intent);

            }
        });
        btRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshDevice();
            }
        });

    }

    private void refreshDevice() {
        requestPermission(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
    }

    @Override
    public void permissionSuccess(int requestCode) {
        super.permissionSuccess(requestCode);
        if (requestCode == 101) {
            startScanDevice();
        }
    }

    private void startScanDevice() {
        if (isRefreshing) return;
        isRefreshing = true;
        BluetoothLeService bluetoothLeService = App.getInstance().getBluetoothLeService();
        if (bluetoothLeService != null) {
            final BluetoothAdapter bluetoothAdapter = bluetoothLeService.getmBluetoothAdapter();
            if (bluetoothAdapter == null) {
                isRefreshing = false;
                return;
            }
            tvRefresh.setText("正在扫描...");
            bluetoothDeviceList.clear();
            adapterList.clear();
            bleDeviceList.clear();
            bluetoothAdapter.startLeScan(new UUID[]{SampleGattAttributes.bltServerUUID}, leScanCallback);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tvRefresh.setText("扫描结束");
                    isRefreshing = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, 5000);

        } else {
            isRefreshing = false;
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (!bluetoothDeviceList.contains(device)) {
                bluetoothDeviceList.add(device);
                bleDevice = new BleDevice(device, scanRecord, rssi);
                bleDeviceList.add(bleDevice);
            }
        }
    };


    private boolean parseAdvData(int rssi, byte[] scanRecord) {
        if (rssi < -75) return false;
        byte[] bytes = ParseLeAdvData.adv_report_parse(ParseLeAdvData.BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA, scanRecord);
        if (null == bytes || bytes.length < 2) {
            return false;
        }
        if (bytes[0] == 0x01 && bytes[1] == 0x02) {
            return true;
        }
        return false;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0://更新设备
                    Collections.sort(adapterList, comp);
                    if (adapter != null) {
                        Toast.makeText(MainActivity.this,
                                "==>" + new Gson().toJson(adapterList), Toast.LENGTH_LONG).show();
                        adapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    class DeviceThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (bleDeviceList.size() > 0) {
                    BleDevice bleDevice = bleDeviceList.get(0);
                    if (null != bleDevice && parseAdvData(bleDevice.getRiss(), bleDevice.getScanBytes())) {
                        adapterList.add(bleDevice);
                        handler.sendEmptyMessage(0);
                    }
                    bleDeviceList.remove(0);
                }
            }
        }
    }
}
