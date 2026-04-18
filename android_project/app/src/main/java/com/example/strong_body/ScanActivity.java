package com.example.strong_body;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    private TextView tvScanTip;

    private YOLOv8Detector yoloDetector;
    private ExecutorService cameraExecutor;

    // 当前识别结果
    private String currentResult = "";
    private boolean isShowingDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_scan);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        viewFinder = findViewById(R.id.viewFinder);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        tvScanTip = findViewById(R.id.tvScanTip);
        cameraExecutor = Executors.newSingleThreadExecutor();

        yoloDetector = new YOLOv8Detector(this, "best_float32.tflite", "labels.txt");

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) startCamera();
                    else Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_LONG).show();
                });

        // 点击按钮跳转到详情页面
        btnAnalyze.setOnClickListener(v -> {
            // 提取器材名称（去掉置信度）
            String equipmentName = currentResult.replaceAll("\\(\\d+%\\)", "").trim();

            // 检查是否识别到了有效的器材
            if (equipmentName.isEmpty() || equipmentName.equals("正在识别...") || equipmentName.contains("未识别")) {
                Toast.makeText(this, "请先扫描健身器材", Toast.LENGTH_SHORT).show();
                return;
            }

            // 跳转到器材详情页面
            Intent intent = new Intent(ScanActivity.this, EquipmentDetailActivity.class);
            intent.putExtra(EquipmentDetailActivity.EXTRA_EQUIPMENT_NAME, equipmentName);
            startActivity(intent);
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

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                    Bitmap rotatedBitmap = rotateBitmap(bitmap, rotationDegrees);

                    String result = yoloDetector.detect(rotatedBitmap);

                    currentResult = result;

                    runOnUiThread(() -> {
                        btnAnalyze.setText(result);

                        // 更新提示文字
                        if (result.contains("%")) {
                            tvScanTip.setText("识别成功，点击查看详情");
                        }
                    });

                    imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("ScanActivity", "相机启动失败", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap rotateBitmap(Bitmap source, int angle) {
        if (angle == 0) return source;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
