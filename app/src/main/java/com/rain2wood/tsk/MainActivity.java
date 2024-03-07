package com.rain2wood.tsk;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private TextView triStateTextView;
    private String tskNode = "/proc/tristatekey/tri_state";
    private Process process;
    private BufferedReader bufferedReader;
    private Thread monitoringThread;

    public String lastTskValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startMonitor(tskNode);

        lastTskValue = readTskValue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopMonitor();
    }

    private void startMonitor(String filePath) {
        try {
            process = Runtime.getRuntime().exec("su");
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            process.getOutputStream().write(("inotifyd - /proc/tristatekey/tri_state\n").getBytes());
            process.getOutputStream().flush();

            monitoringThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while ((bufferedReader.readLine()) != null) {
                            handleTskChange();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            monitoringThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleTskChange() {
        String tskValue = readTskValue();
        if (!tskValue.equals(lastTskValue)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Current tsk value is " + tskValue, Toast.LENGTH_SHORT).show();
                }
            });

            switch (tskValue) {
                case "1": // silent mode
                    launchApplication("com.oplus.camera");
                case "2": // vibrate mode
                    break;
                case "3": // ring mode
                    break;
                default:
                    break;
            }

        }
        lastTskValue = tskValue;
        return;
    }

    private String readTskValue() {
        StringBuilder stringBuilder = new StringBuilder();
        Process process = null;
        BufferedReader bufferedReader = null;

        try {
            process = Runtime.getRuntime().exec("su -c cat /proc/tristatekey/tri_state");
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (process != null) {
                process.destroy();
            }
        }

        return stringBuilder.toString();
    }

    private void launchApplication(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        startActivity(intent);
    }
    private void stopMonitor() {
        if (process != null) {
            process.destroy();
        }
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (monitoringThread != null && monitoringThread.isAlive()) {
            monitoringThread.interrupt();
        }
    }
}
