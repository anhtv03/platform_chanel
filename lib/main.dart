import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(home: DemoPage(), debugShowCheckedModeBanner: false);
  }
}

class DemoPage extends StatefulWidget {
  const DemoPage({super.key});

  @override
  DemoPageState createState() => DemoPageState();
}

class DemoPageState extends State<DemoPage> {
  static const platform = MethodChannel('com.example.platformchannel/info');
  String _batteryLevel = 'Chưa có thông tin pin.';
  String _deviceInfo = 'Chưa có thông tin thiết bị.';
  String _screenBrightness = 'Chưa có thông tin độ sáng.';
  String _volumeLevel = 'Chưa có thông tin âm lượng.';
  double _brightnessValue = 0.0;
  double _volumeValue = 0.0;
  int _maxVolume = 100;
  List<Map<String, dynamic>> _launcherApps = [];

  @override
  void initState() {
    super.initState();
    _getBatteryLevel();
    _getDeviceInfo();
    _getScreenBrightness();
    _getVolumeLevel();
    _getLauncherApps();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.lightBlue[200],
        title: Container(
          alignment: Alignment.center,
          child: Text(
            'Thông tin thiết bị',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 28,
              color: Colors.black,
            ),
            textAlign: TextAlign.center,
          ),
        ),
      ),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [Colors.lightBlue[200]!, Colors.white],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ),
        ),
        child: SafeArea(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              //Device information
              Container(
                margin: const EdgeInsets.all(16),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  color: Colors.white,
                ),
                child: Column(
                  children: [
                    _buildInfoField("Tên thiết bị:", _deviceInfo),
                    _buildInfoField("Phần trăm PIN:", _batteryLevel),
                    _buildSliderField(
                      "Độ sáng màn hình:",
                      _screenBrightness,
                      _brightnessValue,
                      0,
                      100,
                      (value) async {
                        setState(() {
                          _brightnessValue = value;
                        });
                        await _setScreenBrightness(value);
                      },
                    ),
                    _buildSliderField(
                      "Âm lượng:",
                      _volumeLevel,
                      _volumeValue,
                      0,
                      100,
                      (value) async {
                        setState(() {
                          _volumeValue = value;
                        });
                        await _setVolumeLevel(value);
                      },
                    ),
                  ],
                ),
              ),

              //App information
              Expanded(
                child: Container(
                  margin: const EdgeInsets.all(16),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: Colors.grey[200],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "Ứng dụng đã cài đặt",
                        style: TextStyle(fontSize: 20),
                      ),
                      SizedBox(height: 10),
                      Expanded(child: _buildAppInfoField()),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  //=======================handle UI=======================
  Widget _buildInfoField(String title, String name) {
    return Column(
      children: [
        Row(
          children: [
            Text(title, style: TextStyle(fontSize: 20)),
            Spacer(),
            Text(name, style: TextStyle(fontSize: 20)),
          ],
        ),
        const SizedBox(height: 20),
      ],
    );
  }

  Widget _buildSliderField(
    String title,
    String name,
    double sliderValue,
    double min,
    double max,
    ValueChanged<double> onChanged,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Text(title, style: TextStyle(fontSize: 20)),
            Spacer(),
            Text(name, style: TextStyle(fontSize: 20)),
          ],
        ),
        SizedBox(
          width: double.infinity,
          child: SliderTheme(
            data: SliderTheme.of(context).copyWith(
              activeTrackColor: Colors.lightBlue[500],
              thumbColor: Colors.lightBlue[100],
              trackHeight: 2,
              thumbShape: RoundSliderThumbShape(enabledThumbRadius: 8),
            ),
            child: Slider(
              value: sliderValue,
              min: min,
              max: max,
              onChanged: onChanged,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAppInfoField() {
    return ListView.builder(
      itemCount: _launcherApps.length,
      itemBuilder: (context, index) {
        final app = _launcherApps[index];
        int totalMinutes = app['usageTime'] as int;
        int hours = totalMinutes ~/ 60;
        int minutes = totalMinutes % 60;
        String usageTime = '';
        if (hours > 0) {
          usageTime = '$hours' + 'h';
          if (minutes > 0) {
            usageTime += ' ' + '$minutes' + 'm';
          }
        } else {
          usageTime = '$minutes' + 'm';
        }

        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 0, vertical: 4),
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(color: Colors.white),
          child: ListTile(
            leading:
                _launcherApps[index]['icon'] != null
                    ? Image.memory(
                      _launcherApps[index]['icon'] as Uint8List,
                      cacheWidth: 48,
                      cacheHeight: 48,
                    )
                    : Icon(Icons.android),
            title: Text(
              app['appName'] as String,
              style: TextStyle(fontSize: 16),
            ),
            trailing: Text(
              usageTime,
              style: TextStyle(fontSize: 16, color: Colors.black),
            ),
            onTap: () {
              _openLaunchApp(app['packageName'] as String);
            },
          ),
        );
      },
    );
  }

  //==========handle logic========================
  Future<void> _getBatteryLevel() async {
    String batteryLevel;
    try {
      final int result = await platform.invokeMethod('getBatteryLevel');
      batteryLevel = '$result% PIN';
    } on PlatformException catch (e) {
      batteryLevel = "Error getting battery information: '${e.message}'.";
    }
    setState(() {
      _batteryLevel = batteryLevel;
    });
  }

  Future<void> _getDeviceInfo() async {
    String deviceInfo;
    try {
      final String result = await platform.invokeMethod('getDeviceInfo');
      deviceInfo = result;
    } on PlatformException catch (e) {
      deviceInfo = "Error getting device information: '${e.message}'.";
    }
    setState(() {
      _deviceInfo = deviceInfo;
    });
  }

  Future<void> _getScreenBrightness() async {
    try {
      final int result = await platform.invokeMethod('getScreenBrightness');
      double percentage = (result / 255) * 100;
      setState(() {
        _brightnessValue = percentage;
        _screenBrightness = '${percentage.round()}%';
      });
    } on PlatformException catch (e) {
      print("Error getting brightness: ${e.message}");
    }
  }

  Future<void> _setScreenBrightness(double percentage) async {
    try {
      int brightnessValue = ((percentage / 100) * 255).round();
      await platform.invokeMethod('setScreenBrightness', {
        'brightness': brightnessValue,
      });
      await _getScreenBrightness();
    } on PlatformException catch (e) {
      print("Error changing brightness: ${e.message}");
      _getScreenBrightness();
    }
  }

  Future<void> _getVolumeLevel() async {
    try {
      final int currentVolume = await platform.invokeMethod('getVolumeLevel');
      final int maxVolume = await platform.invokeMethod('getMaxVolumeLevel');
      double percentage = (currentVolume / maxVolume) * 100;
      setState(() {
        _volumeLevel = '${(percentage).round()}%';
        _volumeValue = percentage;
        _maxVolume = maxVolume;
      });
    } on PlatformException catch (e) {
      print("Error getting volume: ${e.message}");
    }
  }

  Future<void> _setVolumeLevel(double percentage) async {
    try {
      int volumeValue = ((percentage / 100) * _maxVolume).round();
      await platform.invokeMethod('setVolumeLevel', {'volume': volumeValue});
      await _getVolumeLevel();
    } on PlatformException catch (e) {
      print("Error changing volume: ${e.message}");
      _getVolumeLevel();
    }
  }

  Future<void> _getLauncherApps() async {
    try {
      final List<dynamic> result = await platform.invokeMethod(
        'getLauncherApps',
      );
      setState(() {
        _launcherApps =
            result.map((dynamic item) {
              return (item as Map).cast<String, dynamic>();
            }).toList();
      });
    } on PlatformException catch (e) {
      print("Error getting application list: ${e.message}");
    }
  }

  Future<void> _openLaunchApp(String packageName) async {
    try {
      await platform.invokeMethod('openLaunchApp', {
        'packageName': packageName,
      });
    } on PlatformException catch (e) {
      print("Error opening app: ${e.message}");
    }
  }
}
