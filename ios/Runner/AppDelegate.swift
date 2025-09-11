import UIKit
import Flutter

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    let controller : FlutterViewController = window?.rootViewController as! FlutterViewController
    let channel = FlutterMethodChannel(name: "com.example.demoplatformchannel/info", binaryMessenger: controller.binaryMessenger)

    channel.setMethodCallHandler({
      (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
      switch call.method {
      case "getBatteryLevel":
        self.getBatteryLevel(result: result)
      case "getDeviceInfo":
        self.getDeviceInfo(result: result)
      case "getGPSInfo":
        // Demo: Trả về tọa độ giả lập
        let location = ["latitude": 10.762622, "longitude": 106.660172]
        result(location)
      default:
        result(FlutterMethodNotImplemented)
      }
    })

    GeneratedPluginRegistrant.register(with: self)
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  private func getBatteryLevel(result: FlutterResult) {
    let device = UIDevice.current
    device.isBatteryMonitoringEnabled = true
    if device.batteryState == .unknown {
      result(FlutterError(code: "UNAVAILABLE", message: "Battery info not available.", details: nil))
    } else {
      result(Int(device.batteryLevel * 100))
    }
  }

  private func getDeviceInfo(result: FlutterResult) {
    let processInfo = ProcessInfo.processInfo
    let ramInMB = processInfo.physicalMemory / (1024 * 1024)
    let deviceModel = UIDevice.current.model
    
    // Lấy dung lượng bộ nhớ khả dụng (không phải tổng dung lượng)
    let fileSystem = try? FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory())
    let freeSpace = fileSystem?[.systemSize] as? NSNumber
    let storageInGB = (freeSpace?.int64Value ?? 0) / (1024 * 1024 * 1024)
    
    let deviceInfo = "Model: \(deviceModel), RAM: \(ramInMB) MB, Storage: \(storageInGB) GB"
    result(deviceInfo)
  }
}