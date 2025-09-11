// lib/main.dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
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
  static const platform = MethodChannel('com.example.demoplatformchannel/info');
  String _batteryLevel = 'Chưa có thông tin pin.';
  String _deviceInfo = 'Chưa có thông tin thiết bị.';

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    _getBatteryLevel();
    _getDeviceInfo();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.lightBlue[300]!, Colors.white],
          begin: Alignment.topCenter,
          end: Alignment.center,
        ),
      ),
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: AppBar(
          backgroundColor: Colors.transparent,
          elevation: 0,
          title: Container(
            alignment: Alignment.center,
            child: Text(
              'Thông tin thiết bị',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 28),
              textAlign: TextAlign.center,
            ),
          ),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            //Device information
            Center(
              child: Container(
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
                    _buildInfoField("Độ sáng màn hình:", ""),
                    _buildInfoField("Âm lượng:", ""),
                  ],
                ),
              ),
            ),

            //App information
            Center(
              child: Container(
                margin: const EdgeInsets.all(16),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  color: Colors.white,
                ),
                child: Column(
                  children: [
                    Text("Ứng dụng đã cài đặt", style: TextStyle(fontSize: 20)),
                  ],
                ),
              ),
            ),
          ],
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

  Widget _buildAppInfoField(String title, String name) {
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

  //=======================handle logic=======================
  Future<void> _getBatteryLevel() async {
    String batteryLevel;
    try {
      final int result = await platform.invokeMethod('getBatteryLevel');
      batteryLevel = '$result% PIN';
    } on PlatformException catch (e) {
      batteryLevel = "Lỗi khi lấy thông tin pin: '${e.message}'.";
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
      deviceInfo = "Lỗi khi lấy thông tin thiết bị: '${e.message}'.";
    }
    setState(() {
      _deviceInfo = deviceInfo;
    });
  }
}
