import 'package:flutter/material.dart';
import 'package:panoramic_camera/panoramic_camera.dart';
import 'package:panoramic_camera/panoramic_camera_controller.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String helperText = "Loading";
  int isVertical = 0;
  bool isShootingStarted = false;
  int photos = 0;
  bool isHide = false;
  double pitch = 0.0;
  double roll = 0.0;
  double percentage = 0.0;
  late PanoramicCameraController controller;

  @override
  void initState() {
    super.initState();
    controller = PanoramicCameraController(
      onCameraStarted: () {
        debugPrint('Camera started');
      },
      onFinishRelease: () {
        debugPrint('Finished release');
      },
      onShootingCompleted: (bool value) {
        debugPrint('onShootingCompleted $value');
        isShootingStarted = false;
      },
      onShootingCanceled: (bool value) {
        debugPrint('onShootingCanceled $value');
      },
      onCameraStopped: () {},
      onDeviceVerticalityChanged: (int val) {
        isVertical = val;
        if (isVertical == 1) {
          if (!isShootingStarted) {
            setState(() {
              helperText = 'Tap to start';
            });
          }
        } else {
          setState(() {
            helperText = 'Hold the device vertically';
          });
        }
      },
      onDirectionUpdated: (value) {
        // print('Device onDirectionUpdated changed: $value');
      },
      onCompassEvent: (value) {},
      onUpdateIndicators: (value) {
        setState(() {
          percentage = value.percentage;
          pitch = value.pitch;
          roll = value.roll;
        });
      },
      onFinishGeneratingEqui: (value) {
        debugPrint("---------------Foto creada---------------");
      },
      onPhotoTaken: () {
        debugPrint("---------------onPhotoTaken---------------");
        photos++;
        if (photos <= 0) {
          setState(() {
            helperText = 'Tap to start';
          });
        } else if (photos == 1) {
          setState(() {
            helperText = 'Rotate left right or tap to restart';
          });
        } else {
          setState(() {
            helperText = 'Tap to finish when ready or continue rotating';
          });
        }
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Plugin example app'),
          ),
          floatingActionButton: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              FloatingActionButton(
                child: const Icon(Icons.camera_alt),
                onPressed: () {
                  isShootingStarted = true;
                  controller.startShooting();
                },
              ),
              FloatingActionButton(
                child: const Icon(Icons.change_circle),
                onPressed: () {
                  setState(() {
                    isHide = !isHide;
                  });
                },
              ),
            ],
          ),
          body: Stack(
            children: [
              SizedBox(
                width: double.infinity,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Text(
                      helperText,
                      style: const TextStyle(color: Colors.black),
                    ),
                    Container(
                      decoration: BoxDecoration(
                          border: Border.all(width: 2, color: Colors.yellow)),
                      height: isHide ? 200 : 630,
                      width: isHide ? 150 : 330,
                      child: PanoramicCameraWidget(
                        showGuide: true,
                        controller: controller,
                      ),
                    ),
                  ],
                ),
              ),
              Center(
                child: Container(
                  color: Colors.white,
                  child: Text(
                      "pitch: ${pitch.toStringAsFixed(2)}  roll: ${roll.toStringAsFixed(2)}  percentage: ${percentage.toStringAsFixed(2)}"),
                ),
              )
            ],
          )),
    );
  }
}
