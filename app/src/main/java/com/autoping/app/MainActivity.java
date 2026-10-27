package com.autoping.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(17, 19, 23);
    private final int panel = Color.rgb(29, 32, 38);
    private final int accent = Color.rgb(65, 82, 104);
    private TextView status, stats;
    private EditText host, port, interval, timeout;
    private String mode = "OFF";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        buildUi();
    }

    private TextView label(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(Color.rgb(225, 228, 234)); t.setTextSize(size);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(20));
        scroll.addView(root);

        TextView title = label("AUTO PING", 26);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        TextView subtitle = label("TCP connection monitor • Android 15", 13);
        subtitle.setTextColor(Color.LTGRAY);
        root.addView(subtitle, space(0, 4, 0, 20));

        status = label("สถานะ: ปิดการทำงาน", 16);
        status.setPadding(dp(14), dp(14), dp(14), dp(14));
        status.setBackgroundColor(panel);
        root.addView(status, space(0, 0, 0, 12));

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.VERTICAL);
        String[] names = {"OFF", "KEEP-ALIVE", "RESET", "AGGRESSIVE"};
        for (String name : names) {
            Button b = new Button(this);
            b.setText(name);
            b.setAllCaps(false);
            b.setOnClickListener(v -> chooseMode(name));
            modes.addView(b, space(0, 3, 0, 3));
        }
        root.addView(modes);

        TextView advanced = label("ตั้งค่าการตรวจสอบ", 19);
        advanced.setTypeface(null, Typeface.BOLD);
        root.addView(advanced, space(0, 18, 0, 8));

        host = field("ปลายทาง Host", "www.google.com", InputType.TYPE_CLASS_TEXT);
        port = field("พอร์ต TCP", "443", InputType.TYPE_CLASS_NUMBER);
        interval = field("ช่วงตรวจสอบ (วินาที)", "15", InputType.TYPE_CLASS_NUMBER);
        timeout = field("Timeout (วินาที)", "3", InputType.TYPE_CLASS_NUMBER);
        addField(root, "TCP target", host);
        addField(root, "TCP port", port);
        addField(root, "Interval (1–60 วินาที)", interval);
        addField(root, "Timeout (1–30 วินาที)", timeout);

        Button apply = new Button(this);
        apply.setText("บันทึกค่าและเริ่มทำงาน");
        apply.setAllCaps(false);
        apply.setOnClickListener(v -> startPing());
        root.addView(apply, space(0, 14, 0, 8));

        Button stop = new Button(this);
        stop.setText("หยุดการทำงาน");
        stop.setAllCaps(false);
        stop.setOnClickListener(v -> stopPing());
        root.addView(stop);

        stats = label("สถิติจะปรากฏใน Notification ระหว่างทำงาน", 14);
        stats.setPadding(dp(14), dp(14), dp(14), dp(14));
        stats.setBackgroundColor(panel);
        root.addView(stats, space(0, 14, 0, 0));

        TextView note = label("หมายเหตุ: Auto Ping ตรวจสอบ TCP เท่านั้น ไม่เพิ่มความแรงสัญญาณมือถือ และไม่รีเซ็ตโมเด็ม/APN/DNS", 12);
        note.setTextColor(Color.LTGRAY);
        root.addView(note, space(0, 16, 0, 0));
        setContentView(scroll);
    }

    private EditText field(String hint, String value, int type) {
        EditText e = new EditText(this);
        e.setSingleLine(true); e.setText(value); e.setHint(hint); e.setInputType(type);
        e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackgroundColor(panel);
        return e;
    }

    private void addField(LinearLayout root, String title, EditText e) {
        root.addView(label(title, 14), space(0, 8, 0, 3));
        root.addView(e, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void chooseMode(String selected) {
        mode = selected;
        if ("OFF".equals(selected)) { stopPing(); return; }
        status.setText("เลือกโหมด: " + selected + " — กดบันทึกค่าและเริ่มทำงาน");
    }

    private void startPing() {
        if ("OFF".equals(mode)) mode = "KEEP-ALIVE";
        String h = host.getText().toString().trim();
        int p, i, t;
        try {
            p = Integer.parseInt(port.getText().toString().trim());
            i = Integer.parseInt(interval.getText().toString().trim());
            t = Integer.parseInt(timeout.getText().toString().trim());
        } catch (Exception ex) { toast("กรุณากรอกตัวเลขพอร์ต/ช่วงเวลา/Timeout ให้ถูกต้อง"); return; }
        if (h.isEmpty() || h.length() > 253 || p < 1 || p > 65535 || i < 1 || i > 60 || t < 1 || t > 30) {
            toast("ค่าที่กรอกไม่ถูกต้อง ตรวจสอบ Host, Port, Interval และ Timeout");
            return;
        }
        Intent intent = new Intent(this, PingService.class);
        intent.setAction(PingService.ACTION_START);
        intent.putExtra("host", h);
        intent.putExtra("port", p);
        intent.putExtra("interval", i);
        intent.putExtra("timeout", t);
        intent.putExtra("mode", mode);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
            status.setText("สถานะ: กำลังตรวจสอบ " + h + ":" + p + " (" + mode + ")");
            stats.setText("เปิดการทำงานแล้ว ดูสถิติสดจาก Notification");
        } catch (Exception e) { toast("เริ่มบริการไม่ได้: " + e.getMessage()); }
    }

    private void stopPing() {
        stopService(new Intent(this, PingService.class));
        mode = "OFF";
        if (status != null) status.setText("สถานะ: ปิดการทำงาน");
        if (stats != null) stats.setText("หยุดตรวจสอบแล้ว");
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout.LayoutParams space(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p;
    }
}
