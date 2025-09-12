package com.example.demo_platform_channel;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContextWrapper;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings;
import android.view.WindowManager;
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
    private static final String CHANNEL = "com.example.platformchannel/info";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
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
                        }
                );
    }

    private int getBatteryLevel() {
        int batteryLevel = -1;
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            Intent intent = new ContextWrapper(getApplicationContext()).
                    registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            batteryLevel = (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100) /
                    intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        }
        return batteryLevel;
    }

    private String getDeviceInfo() {
        return Build.MODEL;
    }

    private int getScreenBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }

    private void setScreenBrightness(int brightness) throws Exception {
        brightness = Math.max(0, Math.min(255, brightness));
        try {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            if (VERSION.SDK_INT >= VERSION_CODES.CUPCAKE) {
                layoutParams.screenBrightness = brightness / 255.0f;
            }
            getWindow().setAttributes(layoutParams);
        } catch (Exception e) {
            throw new Exception("Không thể thay đổi độ sáng: " + e.getMessage());
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
            throw new Exception("Không thể thay đổi âm lượng: " + e.getMessage());
        }
    }

    private boolean checkUsageStatsPermission() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        List<UsageStats> usageStatsList = null;
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            usageStatsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, calendar.getTimeInMillis(), System.currentTimeMillis());
        }
        return !usageStatsList.isEmpty();
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, 200);
    }

    private List<Map<String, Object>> getLauncherApps() {
        List<Map<String, Object>> launcherApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(launcherIntent, 0);

        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        for (ResolveInfo info : resolveInfos) {
            String packageName = info.activityInfo.packageName;
            String appName = info.loadLabel(pm).toString();
            Drawable iconDrawable = info.loadIcon(pm);

            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && iconDrawable instanceof AdaptiveIconDrawable) {
                Drawable backgroundDr = ((AdaptiveIconDrawable) iconDrawable).getBackground();
                Drawable foregroundDr = ((AdaptiveIconDrawable) iconDrawable).getForeground();
                Drawable[] layers = new Drawable[]{backgroundDr, foregroundDr};
                bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                for (Drawable layer : layers) {
                    if (layer != null) {
                        layer.setBounds(0, 0, 108, 108);
                        layer.draw(canvas);
                    }
                }
            } else {
                bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] iconBytes = stream.toByteArray();

            UsageStats usageStats = null;
            long usageTimeMillis = 0;
            if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime).get(packageName);
                usageTimeMillis = (usageStats != null) ? usageStats.getTotalTimeInForeground() : 0;
            }
            long usageTimeMinutes = usageTimeMillis / (1000 * 60);

            Map<String, Object> appData = new HashMap<>();
            appData.put("packageName", packageName);
            appData.put("appName", appName);
            appData.put("usageTime", usageTimeMinutes);
            appData.put("icon", iconBytes);
            launcherApps.add(appData);
        }
        return launcherApps;
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