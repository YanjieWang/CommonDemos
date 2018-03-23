package android.serialport;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class MyserialActivity extends Activity {
    public static final String TAG = "MyserialActivity";
	EditText mReception,chuankou,botelv;
    TextView mReceive;
	OutputStream mOutputStream;
	InputStream mInputStream;
    SerialPort sp;
    ReadThread  mReadThread;
    String tty = "/dev/ttySC1";
    int boundryRate = 9600;
    File ttyFile = new File(tty);
    boolean isDockExist = ttyFile.exists();
    private Toast mToast;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch(action) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    //Name of extra for ACTION_USB_DEVICE_ATTACHED and ACTION_USB_DEVICE_DETACHED broadcasts containing the UsbDevice object for the device.
                    if(!isDockExist&&ttyFile.exists()){
                        isDockExist = true;
                        handleDockConnect();
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    //Name of extra for ACTION_USB_DEVICE_ATTACHED and ACTION_USB_DEVICE_DETACHED broadcasts containing the UsbDevice object for the device.
                    if(isDockExist&&!ttyFile.exists()){
                        isDockExist = false;
                        handleDockDisConnect();
                    }
                    break;
            }
        }
    };



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mReception = (EditText) findViewById(R.id.EditTextEmission);
        chuankou = (EditText) findViewById(R.id.chuankou);
        botelv = (EditText) findViewById(R.id.botelv);
        String str_chuankou = getSharedPreferences("setting",Context.MODE_PRIVATE).
                getString("chuankou","/dev/ttySC1");
        String str_botelv = getSharedPreferences("setting",Context.MODE_PRIVATE).
                getString("botelv","9600");
        tty = str_chuankou;
        try {
            boundryRate = Integer.parseInt(str_botelv);
        }catch (Exception e){
            showToast("波特率设置有误");
        }
        chuankou.setText(str_chuankou);
        botelv.setText(str_botelv);
        mReceive = (TextView) findViewById(R.id.EditTextReceive);
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(receiver, intentFilter);
        if (ttyFile.exists()) {
            handleDockConnect();
        }

        findViewById(R.id.bt_serial_send).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    mOutputStream = sp.getOutputStream();
                    mOutputStream.write(NumUtils.bigBytes(NumUtils.toBytes("02 30 30 30 34 31 38 30 30 30 30 30 30 31 3c 03")));
                    showToast("发送数据 :\n" + mReception.getText().toString() + "\n成功");
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("发送数据失败");
                }
            }
        });

    }


    private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[64];
					if (mInputStream == null) return;
					size = mInputStream.read(buffer);
					if (size > 0) {
						onDataReceived(buffer, size);
						
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

    void onDataReceived(final byte[] buffer, final int size) {
		runOnUiThread(new Runnable() {
			public void run() {
				if (mReceive != null) {
                    mReceive.append(new String(NumUtils.bytesToHexFun1(buffer)));
				}
			}
		});
	}
    private void showToast(String str){
        if(mToast!=null){
            mToast.cancel();
        }
        mToast=Toast.makeText(MyserialActivity.this, str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    @Override
    protected void onDestroy() {
        mToast = null;
        unregisterReceiver(receiver);
        handleDockDisConnect();

        getSharedPreferences("setting",Context.MODE_PRIVATE).edit()
                .putString("chuankou",chuankou.getText().toString())
                .putString("botelv",botelv.getText().toString()).commit();
        super.onDestroy();
    }

    void handleDockConnect(){
        findViewById(R.id.bt_serial_send).setEnabled(true);

        try {
            if(ttyFile.exists()){
                sp=new SerialPort(ttyFile, boundryRate, 0);
                showToast( "串口打开成功");
                if(mReadThread!=null){
                    mReadThread.interrupt();
                    mReadThread = null;
                }
                mReadThread = new ReadThread();
                mReadThread.start();
                mInputStream = sp.getInputStream();
            }else{
                showToast( "串口不存在，打开失败");
                return;
            }

        } catch (SecurityException | IOException e) {
            e.printStackTrace();
            showToast( "串口打开失败");
            return;
        }

    };
    void handleDockDisConnect(){

        findViewById(R.id.bt_serial_send).setEnabled(false);
        if(sp!=null){
            sp.close();
            if(mReadThread!=null){
                mReadThread.interrupt();
                mReadThread = null;
            }
            sp=null;
            if(mInputStream != null){
                try {
                    mInputStream.close();
                    mInputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            showToast( "关闭串口成功");
        }
    };
}