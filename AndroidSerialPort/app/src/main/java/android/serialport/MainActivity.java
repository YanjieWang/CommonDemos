package android.serialport;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Set;

public class MainActivity extends Activity {

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    private Spinner spn;
    private Spinner spn_code;
    private CheckBox is_16;
    private ByteArrayOutputStream bb = new ByteArrayOutputStream();
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);

        display = (TextView) findViewById(R.id.textView1);
        editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                    if (usbService != null) { // if UsbService was correctly binded, Send data
                        if (is_16.isChecked()) {
                            usbService.write(NumUtils.toBytes(data));
                        } else {
                            try {
                                usbService.write(data.getBytes(getResources().getStringArray(R.array.codes)[spn_code.getSelectedItemPosition()]));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        spn = findViewById(R.id.spn);
        spn.setSelection(getSharedPreferences("config", MODE_PRIVATE).getInt("baudrate", 12));
        spn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getSharedPreferences("config", MODE_PRIVATE).edit().putInt("baudrate", position).commit();
                if (usbService != null) {
                    usbService.resSetBaudRate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spn_code = findViewById(R.id.spn_code);
        spn_code.setSelection(getSharedPreferences("config", MODE_PRIVATE).getInt("spn_code", 0));
        spn_code.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getSharedPreferences("config", MODE_PRIVATE).edit().putInt("spn_code", position).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        is_16 = findViewById(R.id.is_16);
        is_16.setChecked(getSharedPreferences("config", MODE_PRIVATE).getBoolean("is_16", true));
        is_16.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private KeyListener kl_default;

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSharedPreferences("config", MODE_PRIVATE).edit().putBoolean("is_16", isChecked).commit();
                byte[] data = bb.toByteArray();
                String res = "";
                String code = getResources().getStringArray(R.array.codes)[spn_code.getSelectedItemPosition()];
                if (is_16.isChecked()) {
                    res = NumUtils.bytesToHexFun1(data);
                    try {
                        editText.setText(NumUtils.bytesToHexFun2(editText.getText().toString().getBytes(code)));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (kl_default == null) {
                        kl_default = editText.getKeyListener();
                    }
                    editText.setKeyListener(DigitsKeyListener.getInstance("0123456789ABCDEFabcdef"));
                } else {
                    try {
                        res = new String(data, getResources().getStringArray(R.array.codes)[spn_code.getSelectedItemPosition()]);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    try {
                        editText.setText(new String(NumUtils.toBytes(editText.getText().toString()), code));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    editText.setKeyListener(kl_default);
                }
                display.setText(res);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    public void clean(View view) {
        bb.reset();
        display.setText("");
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    byte[] data = (byte[]) msg.obj;
                    try {
                        bb.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    data = bb.toByteArray();
                    String res = "";
                    if (is_16.isChecked()) {
                        res = NumUtils.bytesToHexFun1(data);
                    } else {
                        try {
                            res = new String(data, getResources().getStringArray(R.array.codes)[spn_code.getSelectedItemPosition()]);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    //Log.e("TEST",res);

                    mActivity.get().display.setText(res);
                    //Log.e("TEST",NumUtils.bytesToHexFun1(display.getText().toString().getBytes()));
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}