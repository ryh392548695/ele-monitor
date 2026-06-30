package com.elemonitor;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EleMonitor";

    private MonitorService monitorService;
    private boolean serviceBound = false;

    private EditText etInterval;
    private EditText etColumnName;
    private Spinner spComparison;
    private EditText etThreshold;
    private EditText etCooldown;
    private Button btnStart;
    private Button btnStop;
    private Button btnTestSound;
    private TextView tvStatus;
    private LinearLayout logContainer;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorService.LocalBinder binder = (MonitorService.LocalBinder) service;
            monitorService = binder.getService();
            serviceBound = true;
            monitorService.setLogCallback(MainActivity.this::addLog);
            updateStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        requestPermissions();
        ignoreBatteryOptimization();

        Intent intent = new Intent(this, MonitorService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        etInterval = findViewById(R.id.et_interval);
        etColumnName = findViewById(R.id.et_column_name);
        spComparison = findViewById(R.id.sp_comparison);
        etThreshold = findViewById(R.id.et_threshold);
        etCooldown = findViewById(R.id.et_cooldown);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnTestSound = findViewById(R.id.btn_test_sound);
        tvStatus = findViewById(R.id.tv_status);
        logContainer = findViewById(R.id.log_container);

        btnStart.setOnClickListener(v -> startMonitoring());
        btnStop.setOnClickListener(v -> stopMonitoring());
        btnTestSound.setOnClickListener(v -> testSound());
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    private void ignoreBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            new AlertDialog.Builder(this)
                .setTitle("电池优化设置")
                .setMessage("为了确保后台监控不被系统杀死，请允许应用后台运行")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("稍后", null)
                .show();
        }
    }

    private void startMonitoring() {
        if (!serviceBound || monitorService == null) {
            Toast.makeText(this, "服务未就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        MonitorConfig config = new MonitorConfig();
        config.intervalMinutes = parseDouble(etInterval.getText().toString(), 2);
        config.columnName = etColumnName.getText().toString().trim();
        config.comparison = spComparison.getSelectedItem().toString();
        config.threshold = parseDouble(etThreshold.getText().toString(), 0);
        config.cooldownSeconds = parseInt(etCooldown.getText().toString(), 60);
        config.targetUrl = "https://r.ele.me/dm-area-data-board/#/agency/data/monitor";

        monitorService.startMonitoring(config);
        updateStatus();
        Toast.makeText(this, "监控已启动", Toast.LENGTH_SHORT).show();
    }

    private void stopMonitoring() {
        if (monitorService != null) {
            monitorService.stopMonitoring();
        }
        updateStatus();
        Toast.makeText(this, "监控已停止", Toast.LENGTH_SHORT).show();
    }

    private void testSound() {
        if (monitorService != null) {
            monitorService.playAlarmSound();
        }
    }

    public void addLog(String message, String type) {
        runOnUiThread(() -> {
            TextView logItem = new TextView(this);
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            logItem.setText("[" + time + "] " + message);
            logItem.setTextSize(11);
            logItem.setPadding(0, 3, 0, 3);

            if ("alert".equals(type)) {
                logItem.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            } else if ("success".equals(type)) {
                logItem.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                logItem.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }

            logContainer.addView(logItem, 0);

            while (logContainer.getChildCount() > 30) {
                logContainer.removeViewAt(logContainer.getChildCount() - 1);
            }
        });
    }

    private void updateStatus() {
        if (monitorService != null && monitorService.isRunning()) {
            tvStatus.setText("状态：运行中");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            tvStatus.setBackgroundColor(0xFFF6FFED);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        } else {
            tvStatus.setText("状态：已停止");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            tvStatus.setBackgroundColor(0xFFFFF2F0);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    private double parseDouble(String s, double defaultVal) {
        try { return Double.parseDouble(s); } catch (Exception e) { return defaultVal; }
    }

    private int parseInt(String s, int defaultVal) {
        try { return Integer.parseInt(s); } catch (Exception e) { return defaultVal; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
