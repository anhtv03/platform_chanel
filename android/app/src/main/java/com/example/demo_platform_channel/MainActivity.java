package com.example.demo_platform_channel;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.example.insight/info";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL).setMethodCallHandler((call, result) -> {
            switch (call.method) {
                case "getBatteryLevel":
                    int batteryLevel = getBatteryLevel();
                    if (batteryLevel != -1) {
                        result.success(batteryLevel);
                    } else {
                        result.error("UNAVAILABLE", "Battery level not available.", null);
                    }
                    break;
                case "getDeviceInfo":
                    String deviceInfo = getDeviceInfo();
                    result.success(deviceInfo);
                    break;
                case "getScreenBrightness":
                    try {
                        int brightness = getScreenBrightness();
                        result.success(brightness);
                    } catch (Exception e) {
                        result.error("UNAVAILABLE", "Screen brightness not available: " + e.getMessage(), null);
                    }
                    break;
                case "setScreenBrightness":
                    try {
                        Integer brightnessValue = call.argument("brightness");
                        if (brightnessValue != null) {
                            setScreenBrightness(brightnessValue);
                            result.success("Brightness set successfully");
                        } else {
                            result.error("INVALID_ARGUMENT", "Brightness value is null", null);
                        }
                    } catch (Exception e) {
                        result.error("UNAVAILABLE", "Cannot set brightness: " + e.getMessage(), null);
                    }
                    break;
                case "getVolumeLevel":
                    try {
                        int volume = getVolumeLevel();
                        result.success(volume);
                    } catch (Exception e) {
                        result.error("UNAVAILABLE", "Volume level not available: " + e.getMessage(), null);
                    }
                    break;
                case "getMaxVolumeLevel":
                    try {
                        int maxVolume = getMaxVolumeLevel();
                        result.success(maxVolume);
                    } catch (Exception e) {
                        result.error("UNAVAILABLE", "Max volume level not available: " + e.getMessage(), null);
                    }
                case "setVolumeLevel":
                    try {
                        Integer volumeValue = call.argument("volume");
                        if (volumeValue != null) {
                            setVolumeLevel(volumeValue);
                            result.success("Volume set successfully");
                        } else {
                            result.error("INVALID_ARGUMENT", "Volume value is null", null);
                        }
                    } catch (Exception e) {
                        result.error("UNAVAILABLE", "Cannot set volume: " + e.getMessage(), null);
                    }
                    break;
                case "getLauncherApps":
                    if (checkUsageStatsPermission()) {
                        List<Map<String, Object>> apps = getLauncherApps();
                        result.success(apps);
                    } else {
                        requestUsageStatsPermission();
                        result.error("PERMISSION_DENIED", "Usage stats permission not granted.", null);
                    }
                    break;
                case "openLaunchApp":
                    String packageName = call.argument("packageName");
                    if (packageName != null) {
                        openLaunchApp(packageName, result);
                    } else {
                        result.error("INVALID_ARGUMENT", "Package name is null", null);
                    }
                    break;
                default:
                    result.notImplemented();
                    break;
            }
        });
    }

    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private String getDeviceInfo() {
        String deviceName = Settings.Global.getString(getContentResolver(), "device_name");
        return (deviceName != null) ? deviceName : Build.MANUFACTURER + " " + Build.MODEL;
    }

    private int getScreenBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }

    private void setScreenBrightness(int brightness) {
        brightness = Math.max(0, Math.min(255, brightness));
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1001);
        } else {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        }
    }

    private int getVolumeLevel() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private int getMaxVolumeLevel() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    private void setVolumeLevel(int volume) throws Exception {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volume = Math.max(0, Math.min(maxVolume, volume));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        } catch (Exception e) {
            throw new Exception("Cannot change volume: " + e.getMessage());
        }
    }

    private boolean checkUsageStatsPermission() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.getTimeInMillis(), System.currentTimeMillis());
        return !usageStatsList.isEmpty();
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, 200);
    }

    private List<Map<String, Object>> getLauncherApps() {
        List<Map<String, Object>> installedApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        Map<String, Long> usageMap = new HashMap<>();
        Map<String, UsageStats> aggregatedStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
        for (Map.Entry<String, UsageStats> entry : aggregatedStats.entrySet()) {
            usageMap.put(entry.getKey(), entry.getValue().getTotalTimeInForeground());
        }

        for (ApplicationInfo info : appInfos) {
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                continue;
            }

            String packageName = info.packageName;
            String appName = pm.getApplicationLabel(info).toString();
            Drawable iconDrawable = pm.getApplicationIcon(info);

            byte[] iconBytes = drawableToByteArray(iconDrawable);

            long usageTimeMillis = usageMap.getOrDefault(packageName, 0L);
            long usageTimeMinutes = usageTimeMillis / (1000 * 60);

            Map<String, Object> appData = new HashMap<>();
            appData.put("packageName", packageName);
            appData.put("appName", appName);
            appData.put("usageTime", usageTimeMinutes);
            appData.put("icon", iconBytes);
            installedApps.add(appData);
        }

        installedApps.sort((a, b) -> {
            long timeA = (long) a.get("usageTime");
            long timeB = (long) b.get("usageTime");
            return Long.compare(timeB, timeA);
        });

        return installedApps;
    }

    private byte[] drawableToByteArray(Drawable drawable) {
        if (drawable == null) return new byte[0];

        int width = Math.max(drawable.getIntrinsicWidth(), 108);
        int height = Math.max(drawable.getIntrinsicHeight(), 108);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void openLaunchApp(String packageName, MethodChannel.Result result) {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
                result.success("App launched successfully");
            } else {
                result.error("NOT_FOUND", "No launch intent found for package: " + packageName, null);
            }
        } catch (Exception e) {
            result.error("LAUNCH_ERROR", "Failed to launch app: " + e.getMessage(), null);
        }
    }
}