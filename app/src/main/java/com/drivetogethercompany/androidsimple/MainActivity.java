package com.drivetogethercompany.androidsimple;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidSimple as = new AndroidSimple(getApplicationContext(), getFragmentManager(), null);
        as.download("http://ptt.drivetogether.fr/_preset/triso", Environment.getExternalStorageDirectory().getPath())
                .setOnPreExecuteCallback(new AndroidSimple.Callback()
                {
                    @Override
                    public void onEvent(String p, Context con, FragmentManager fm, View v)
                    {
                        System.out.println("Download starting !");
                    }
                })
                .setOnPostExecuteCallback(new AndroidSimple.Callback() {
                    @Override
                    public void onEvent(String ret, Context con, FragmentManager fm, View v) {
                        System.out.println("Overrided the onPostExecute function !");
                    }
                })
                .setOnProgressUpdateCallback(new AndroidSimple.Callback() {
                    @Override
                    public void onEvent(String progress, Context con, FragmentManager fm, View v) {
                        System.out.println("upload progress: " + progress + "%");
                    }
                })
                .setOnCancelledCallback(new AndroidSimple.Callback() {
                    @Override
                    public void onEvent(String t, Context con, FragmentManager fm, View v) {
                        Log.i("Info", "Download stoped !");
                    }
                })
                .setOnFailedCallback(new AndroidSimple.Callback() {
                    @Override
                    public void onEvent(String t, Context con, FragmentManager fm, View v) {
                        Log.i("Info", "Download failed !");
                    }
                })
                .setTimeOut(1000, TimeUnit.MILLISECONDS)
                .execute();

        final AndroidSimple.interval i = as.setInterval(new AndroidSimple.Callback() {
            @Override
            public void toExecute() {
                System.out.println("spam every second !");
            }
        }, 1000).start();


        as.setTimeout(new AndroidSimple.Callback() {
            @Override
            public void toExecute() {
                i.stop();
                System.out.println("10 seconds pass !");
            }
        }, 10000);



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
