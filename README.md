# Samsung Gear Controller BLE Android Driver

An Android application that enables the Samsung Gear VR Controller to function as a system-wide input device via Bluetooth Low Energy (BLE) and Android Accessibility Services.

## Features

- Connect to paired Samsung Gear Controllers via BLE
- Map controller inputs to Android system functions:
  - Volume buttons → Adjust system volume
  - Home button → Android Home action
  - Back button → Android Back action
  - Trigger → Simulate mouse click
  - Touchpad → Pointer movement (gyro/accelerometer) and directional zones
  - Touchpad click → Recent apps menu
- Operates in background via Accessibility Service
- Automatic reconnection when controller wakes up

## Requirements

- Android 9.0 (API 28) or higher
- Samsung Gear VR Controller (paired via Bluetooth settings)

## Installation & Setup

1. Install the APK
2. Open the app and grant required permissions:
   - Bluetooth
   - Location (required for BLE scanning)
3. Enable Accessibility Service:
   - Go to Settings → Accessibility
   - Find "Samsung Gear Controller BLE Driver"
   - Toggle on and confirm
4. Enable Display Over Other Apps permission:
   - Go to Settings → Apps → Special app access
   - Select "Display over other apps"
   - Find and enable this app

## Usage

1. Open the app
2. Select your paired Gear Controller from the list
3. The controller will now work system-wide:
   - Volume buttons adjust media volume
   - Home/Back buttons work as expected
   - Gyro controls pointer movement
   - Trigger acts as mouse click
   - Touchpad zones provide directional input

## Known Limitations

- Touchpad directional zones may not work in all apps
- Pointer movement may require sensitivity adjustment
- Some Android versions may kill background service aggressively

## Contributing

Contributions are welcome! Please open issues or pull requests on GitHub.

## License

[MIT License](LICENSE) (if applicable)
