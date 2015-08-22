package com.drivetogethercompany.androidsimple;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidSimple as = new AndroidSimple(getApplicationContext(), getFragmentManager(), null);
        as.download("http://ptt.drivetogether.fr/_preset/triso", Environment.getExternalStorageDirectory().getPath())
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
