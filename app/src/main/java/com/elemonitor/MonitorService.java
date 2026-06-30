package com.elemonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class MonitorService extends Service {

    private static final String TAG = "EleMonitorService";
    private static final String CHANNEL_ID = "ele_monitor_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;
    private static final int ALERT_NOTIFICATION_ID = 1002;

    private final IBinder binder = new LocalBinder();
    private Handler handler;
    private Runnable monitorTask;
    private WebView webView;
    private MonitorConfig config;
    private boolean isRunning = false;
    private long lastAlertTime = 0;
    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private LogCallback logCallback;

    public interface LogCallback {
        void onLog(String message, String type);
    }

    public class LocalBinder extends Binder {
        MonitorService getService() {
            return MonitorService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        initWebView();
        acquireWakeLock();
    }

    private void initWebView() {
        webView = new WebView(getApplicationContext());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new JSInterface(), "EleMonitor");
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EleMonitor::WakeLock");
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setLogCallback(LogCallback callback) {
        this.logCallback = callback;
    }

    private void log(String message, String type) {
        Log.d(TAG, message);
        if (logCallback != null) {
            logCallback.onLog(message, type);
        }
    }

    public void startMonitoring(MonitorConfig config) {
        this.config = config;
        this.isRunning = true;
        this.lastAlertTime = 0;

        log("监控已启动，间隔: " + config.intervalMinutes + " 分钟", "success");
        updateForegroundNotification("监控运行中 - 每" + config.intervalMinutes + "分钟查询");

        // Load target page
        webView.loadUrl(config.targetUrl);

        // Wait for page load then start monitoring
        handler.postDelayed(() -> {
            if (isRunning) {
                performQuery();
            }
        }, 5000);

        // Schedule periodic queries
        long intervalMs = (long) (config.intervalMinutes * 60 * 1000);
        monitorTask = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    performQuery();
                    handler.postDelayed(this, intervalMs);
                }
            }
        };
        handler.postDelayed(monitorTask, intervalMs);
    }

    public void stopMonitoring() {
        isRunning = false;
        if (monitorTask != null) {
            handler.removeCallbacks(monitorTask);
        }
        if (webView != null) {
            webView.stopLoading();
        }
        log("监控已停止", null);
        updateForegroundNotification("监控已停止");
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void performQuery() {
        if (!isRunning || webView == null) return;

        log("正在执行查询...", null);

        try {
            // Step 1: Click "盯商圈" tab
            String clickTabScript =
                "(function() {" +
                "  var all = document.querySelectorAll('*');" +
                "  for (var i = 0; i < all.length; i++) {" +
                "    if (all[i].children.length === 0 && all[i].textContent.trim() === '\u76ef\u5546\u5708') {" +
                "      var p = all[i].parentElement;" +
                "      while (p && p.tagName !== 'BODY') {" +
                "        if (p.click || p.onclick) { p.click(); return 'clicked'; }" +
                "        p = p.parentElement;" +
                "      }" +
                "    }" +
                "  }" +
                "  return 'not_found';" +
                "})();";

            webView.evaluateJavascript(clickTabScript, null);

            // Step 2: Click query button after delay
            handler.postDelayed(() -> {
                if (!isRunning) return;

                String clickQueryScript =
                    "(function() {" +
                    "  var btns = document.querySelectorAll('button, [type=\"button\"]');" +
                    "  for (var i = 0; i < btns.length; i++) {" +
                    "    var t = btns[i].textContent.trim().replace(/\s+/g, '');" +
                    "    if (t === '\u67e5\u8be2') {" +
                    "      btns[i].click();" +
                    "      return 'clicked';" +
                    "    }" +
                    "  }" +
                    "  return 'not_found';" +
                    "})();";

                webView.evaluateJavascript(clickQueryScript, null);

                // Step 3: Extract data after delay
                handler.postDelayed(() -> extractData(), 4000);

            }, 2000);

        } catch (Exception e) {
            log("查询出错: " + e.getMessage(), "alert");
        }
    }

    private void extractData() {
        if (!isRunning || webView == null) return;

        String extractScript =
            "(function() {" +
            "  var headers = document.querySelectorAll('th, .ant-table-thead th');" +
            "  var colIndex = -1;" +
            "  var colName = '" + escapeJsString(config.columnName) + "';" +
            "  for (var i = 0; i < headers.length; i++) {" +
            "    var hText = headers[i].textContent.trim().replace(/\s+/g, '');" +
            "    if (hText.indexOf(colName) >= 0 || hText === colName) {" +
            "      colIndex = i; break;" +
            "    }" +
            "  }" +
            "  if (colIndex === -1) {" +
            "    for (var i = 0; i < headers.length; i++) {" +
            "      var hText = headers[i].textContent.trim();" +
            "      if (hText.indexOf('\u6302\u5355') >= 0 || hText.indexOf('15') >= 0) {" +
            "        colIndex = i; break;" +
            "      }" +
            "    }" +
            "  }" +
            "  if (colIndex === -1) return JSON.stringify({error: 'column_not_found'});" +
            "  var rows = document.querySelectorAll('tbody tr, .ant-table-tbody tr');" +
            "  var values = [];" +
            "  for (var j = 0; j < rows.length; j++) {" +
            "    var cells = rows[j].querySelectorAll('td');" +
            "    if (cells.length > colIndex) {" +
            "      var val = parseFloat(cells[colIndex].textContent.trim());" +
            "      if (!isNaN(val)) {" +
            "        values.push({row: j, value: val, text: cells[colIndex].textContent.trim()});" +
            "      }" +
            "    }" +
            "  }" +
            "  return JSON.stringify({colIndex: colIndex, values: values, count: values.length});" +
            "})();";

        webView.evaluateJavascript(extractScript, result -> {
            Log.d(TAG, "Extract result: " + result);
            try {
                String jsonStr = result;
                if (jsonStr.startsWith(""") && jsonStr.endsWith(""")) {
                    jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                }
                jsonStr = jsonStr.replace("\\"", """);

                JSONObject data = new JSONObject(jsonStr);

                if (data.has("error")) {
                    log("提取数据失败: " + data.getString("error"), "alert");
                    return;
                }

                JSONArray values = data.getJSONArray("values");
                int count = values.length();
                double maxVal = 0;
                for (int i = 0; i < count; i++) {
                    double v = values.getJSONObject(i).getDouble("value");
                    if (v > maxVal) maxVal = v;
                }

                log("提取到 " + count + " 行数据，最大值: " + maxVal, null);
                checkThreshold(values);

            } catch (Exception e) {
                log("解析数据失败: " + e.getMessage(), "alert");
            }
        });
    }

    private void checkThreshold(JSONArray values) throws Exception {
        double threshold = config.threshold;
        String comparison = config.comparison;

        for (int i = 0; i < values.length(); i++) {
            JSONObject item = values.getJSONObject(i);
            double value = item.getDouble("value");
            boolean triggered = false;

            switch (comparison) {
                case ">": triggered = value > threshold; break;
                case "<": triggered = value < threshold; break;
                case ">=": triggered = value >= threshold; break;
                case "<=": triggered = value <= threshold; break;
                case "==": triggered = value == threshold; break;
                case "!=": triggered = value != threshold; break;
            }

            if (triggered) {
                long now = System.currentTimeMillis();
                if (now - lastAlertTime > config.cooldownSeconds * 1000) {
                    lastAlertTime = now;
                    triggerAlert(config.columnName, value, threshold);
                }
                break;
            }
        }
    }

    private void triggerAlert(String columnName, double value, double threshold) {
        log("⚠️ 触发提醒! " + columnName + "=" + value + " " + config.comparison + " " + threshold, "alert");

        // Play sound
        playAlarmSound();

        // Show notification
        String message = columnName + ": " + value + " " + config.comparison + " " + threshold;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("盯商圈数据异常提醒")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(new long[]{0, 500, 200, 500, 200, 500});

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(ALERT_NOTIFICATION_ID, builder.build());
        }
    }

    public void playAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();
            mediaPlayer.start();

            handler.postDelayed(() -> {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }, 3000);

        } catch (Exception e) {
            Log.e(TAG, "Play sound error: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "盯商圈监控",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("监控饿了么盯商圈数据异常提醒");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildForegroundNotification() {
        return buildForegroundNotification("监控服务运行中");
    }

    private Notification buildForegroundNotification(String content) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("饿了么盯商圈监控")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void updateForegroundNotification(String content) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(content));
        }
    }

    private String escapeJsString(String s) {
        return s.replace("\", "\\").replace("'", "\'").replace(""", "\"");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (webView != null) {
            webView.destroy();
        }
    }

    public class JSInterface {
        @JavascriptInterface
        public void log(String message) {
            Log.d(TAG, "JS: " + message);
        }
    }
}
