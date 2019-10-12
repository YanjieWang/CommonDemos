package com.example.androidstabilitydemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public void nullPtrCrash(View view) {
        Object obj = null;
        obj.toString();
    }

    public void outIndexPtrCrash(View view) {
        String arry[] = {"test","outOfIndex"};
        for (int i = 0 ; i<=arry.length;i++){
            System.out.println(arry[i]);
        }
    }

    public void sleepAnr(View view) {
        try {
            Thread.currentThread().sleep(999999999);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void endlessLoopAnr(View view) {
        while (true){

        }
    }

    public void deadLockAnr(View view) {

        Thread thread = new Thread(){
            @Override
            public void run() {
                synchronized (lock2){
                    synchronized (lock1){

                    }
                }
            }
        };
        thread.start();
        synchronized (lock1){
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (lock2){

            }
        }

    }

    public void nativeCrash(View view) {
        stringFromJNI();
    }
}
