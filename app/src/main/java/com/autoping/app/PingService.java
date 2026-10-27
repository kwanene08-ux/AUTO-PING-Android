package com.autoping.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingService extends Service {
    public static final String ACTION_START = "com.autoping.app.START";
    private static final String CHANNEL = "auto_ping_status";
    private static final int NOTIFICATION_ID = 2401;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;
    private String host = "www.google.com", mode = "KEEP-ALIVE";
    private int port = 443, interval = 15, timeout = 3;
    private long success = 0, failed = 0, lastMs = -1;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !ACTION_START.equals(intent.getAction())) {
            stopSelf(); return START_NOT_STICKY;
        }
        host = intent.getStringExtra("host");
        if (host == null || host.trim().isEmpty()) host = "www.google.com";
        port = clamp(intent.getIntExtra("port", 443), 1, 65535);
        interval = clamp(intent.getIntExtra("interval", 15), 1, 60);
        timeout = clamp(intent.getIntExtra("timeout", 3), 1, 30);
        mode = intent.getStringExtra("mode");
        if (mode == null) mode = "KEEP-ALIVE";

        running.set(false);
        if (executor != null) executor.shutdownNow();
        success = failed = 0; lastMs = -1;
        startForeground(NOTIFICATION_ID, buildNotification("กำลังเริ่มตรวจสอบ..."));
        running.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.execute(this::loop);
        return START_NOT_STICKY;
    }

    private void loop() {
        while (running.get()) {
            long start = SystemClock.elapsedRealtime();
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), timeout * 1000);
                lastMs = SystemClock.elapsedRealtime() - start;
                success++;
            } catch (Exception e) {
                lastMs = -1;
                failed++;
            } finally {
                try { socket.close(); } catch (Exception ignored) {}
            }
            updateNotification();
            int wait = interval;
            if ("AGGRESSIVE".equals(mode)) wait = Math.max(1, Math.min(interval, 3));
            if ("RESET".equals(mode)) wait = Math.max(5, interval); // Monitor-only; does not reset Android networking.
            long until = SystemClock.elapsedRealtime() + wait * 1000L;
            while (running.get() && SystemClock.elapsedRealtime() < until) {
                try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void updateNotification() {
        String latency = lastMs >= 0 ? lastMs + " ms" : "Timeout/Fail";
        String text = host + ":" + port + " • ล่าสุด " + latency +
                " • สำเร็จ " + success + " / ล้มเหลว " + failed;
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setContentTitle("AUTO PING • " + mode)
         .setContentText(text)
         .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
         .setContentIntent(pi)
         .setOngoing(true)
         .setOnlyAlertOnce(true);
        return b.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "AUTO PING status", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("สถานะการตรวจสอบ TCP");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }

    @Override public void onDestroy() {
        running.set(false);
        if (executor != null) executor.shutdownNow();
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
