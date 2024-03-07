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
        triStateTextView = findViewById(R.id.tsk_status);

        startMonitor(tskNode);

        lastTskValue = readTskValue();
        triStateTextView.setText(tskMode(lastTskValue));
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
            String tskMode = tskMode(tskValue);

            runOnUiThread(new Runnable() {
                public void run() {
                    triStateTextView.setText(tskMode);
                    Toast.makeText(MainActivity.this, "Current ringer mode is is " + tskMode, Toast.LENGTH_SHORT).show();
                }
            });

            switch (tskValue) {
                case "1": // silent mode
                    launchApplication("com.miHoYo.GenshinImpact", "com.miHoYo.GetMobileInfo.MainActivity");
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

    public String readTskValue() {
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

    private void launchApplication(String packageName, String ActivityName) {
        Intent intent;
        if (ActivityName.equals("null")) {
            intent = getPackageManager().getLaunchIntentForPackage(packageName);
        } else {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(packageName, ActivityName));
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
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

    private String tskMode(String tskValue) {
        switch (tskValue) {
            case "1":
                return "Silent";
            case "2":
                return "Vibrate";
            case "3":
                return "Ring";
            default:
                return "Unknown";
        }
    }
}
