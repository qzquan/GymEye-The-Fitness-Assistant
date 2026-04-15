package com.example.strong_body;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private PreviewView viewFinder;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Button btnAnalyze;

    // 👇 引入我们刚刚植入的大脑
    private YOLOv8Detector yoloDetector;
    // 专门用来跑图像分析的后台线程，防止卡顿手机画面
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_scan);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        viewFinder = findViewById(R.id.viewFinder);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 👇 初始化我们的大脑（注意这俩名字必须和 assets 里的文件一模一样！）
        yoloDetector = new YOLOv8Detector(this, "best_float32.tflite", "labels.txt");

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) startCamera();
                    else Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_LONG).show();
                });

        if (hasCameraPermission()) startCamera();
        else requestPermissionLauncher.launch(CAMERA_PERMISSION);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. 预览功能（给用户看）
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. 👇 核心增加：图像分析功能（给模型看）
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        // 强制输出 RGBA 格式，方便转 Bitmap
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        // 如果处理不过来，丢弃旧画面，只看最新的一帧
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // 开始疯狂截取画面
                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    // 把摄像头的帧转成 Bitmap
                    Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

                    // 根据手机姿态旋转图片，保证模型看到的是正立的器械
                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                    Bitmap rotatedBitmap = rotateBitmap(bitmap, rotationDegrees);

                    // 🧠 喂给模型！获取识别结果
                    String result = yoloDetector.detect(rotatedBitmap);

                    // 在主线程更新 UI：把结果显示在那个按钮上
                    runOnUiThread(() -> btnAnalyze.setText(result));

                    // 必须关掉这一帧，才能接收下一帧
                    imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();

                // 👇 把 preview 和 imageAnalysis 一起绑上去！
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("ScanActivity", "相机启动失败", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // 旋转图片的辅助方法
    private Bitmap rotateBitmap(Bitmap source, int angle) {
        if (angle == 0) return source;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown(); // 退出时关掉后台线程
    }
}