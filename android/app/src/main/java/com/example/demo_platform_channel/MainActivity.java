package com.example.demo_platform_channel;
import android.content.ContextWrapper;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings;
import android.view.WindowManager;

import androidx.annotation.NonNull;
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
}