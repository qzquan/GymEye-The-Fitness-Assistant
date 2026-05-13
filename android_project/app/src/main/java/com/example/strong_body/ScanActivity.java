package com.example.strong_body;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanActivity extends AppCompatActivity {

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\\((\\d+%)\\)");
    private static final String DETECTING_TEXT = "正在识别...";
    private static final int STABLE_DETECTION_FRAMES = 3;

    private PreviewView viewFinder;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private MaterialButton btnAnalyze;
    private TextView tvScanTip;
    private TextView tvDetectedName;
    private TextView tvConfidence;

    private YOLOv8Detector yoloDetector;
    private ExecutorService cameraExecutor;

    private String currentResult = "";
    private String lockedEquipmentName = "";
    private String pendingEquipmentName = "";
    private String pendingResult = "";
    private int pendingValidFrames = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_scan);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        viewFinder = findViewById(R.id.viewFinder);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        tvScanTip = findViewById(R.id.tvScanTip);
        tvDetectedName = findViewById(R.id.tvDetectedName);
        tvConfidence = findViewById(R.id.tvConfidence);
        ImageButton btnBack = findViewById(R.id.btnScanBack);
        btnBack.setOnClickListener(v -> finish());
        btnAnalyze.setEnabled(false);
        btnAnalyze.setAlpha(0.62f);

        cameraExecutor = Executors.newSingleThreadExecutor();
        yoloDetector = new YOLOv8Detector(this, "best_float32.tflite", "labels.txt");

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) startCamera();
                    else Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_LONG).show();
                });

        btnAnalyze.setOnClickListener(v -> {
            String equipmentName = extractEquipmentName(currentResult);

            if (!isValidEquipmentName(equipmentName)) {
                Toast.makeText(this, "请先扫描健身器材", Toast.LENGTH_SHORT).show();
                return;
            }

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
                    runOnUiThread(() -> handleDetectionResult(result));

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

    private void handleDetectionResult(String result) {
        String equipmentName = extractEquipmentName(result);
        if (isValidEquipmentName(equipmentName)) {
            if (equipmentName.equals(lockedEquipmentName)) {
                currentResult = result;
                showDetectedResult(equipmentName, extractConfidence(result));
                return;
            }

            if (equipmentName.equals(pendingEquipmentName)) {
                pendingValidFrames++;
            } else {
                pendingEquipmentName = equipmentName;
                pendingResult = result;
                pendingValidFrames = 1;
            }

            if (pendingValidFrames >= STABLE_DETECTION_FRAMES) {
                lockedEquipmentName = pendingEquipmentName;
                currentResult = pendingResult;
                showDetectedResult(lockedEquipmentName, extractConfidence(currentResult));
            } else if (TextUtils.isEmpty(currentResult)) {
                showPendingRecognitionState();
            }
            return;
        }

        if (!TextUtils.isEmpty(currentResult)) {
            return;
        }

        currentResult = "";
        pendingEquipmentName = "";
        pendingResult = "";
        pendingValidFrames = 0;
        showScanningState(result);
    }

    private void showDetectedResult(String equipmentName, String confidence) {
        tvDetectedName.setText(equipmentName);
        tvConfidence.setText("置信度 " + confidence);
        tvScanTip.setText("识别成功，点击查看详情");
        btnAnalyze.setEnabled(true);
        btnAnalyze.setAlpha(1f);
    }

    private void showPendingRecognitionState() {
        tvDetectedName.setText(DETECTING_TEXT);
        tvConfidence.setText("置信度 --");
        tvScanTip.setText("识别中，确认后自动显示结果");
        btnAnalyze.setEnabled(false);
        btnAnalyze.setAlpha(0.62f);
    }

    private void showScanningState(String result) {
        tvDetectedName.setText(TextUtils.isEmpty(result) ? DETECTING_TEXT : result);
        tvConfidence.setText("置信度 --");
        tvScanTip.setText("将器械放入扫描框");
        btnAnalyze.setEnabled(false);
        btnAnalyze.setAlpha(0.62f);
    }

    private static String extractEquipmentName(String result) {
        if (TextUtils.isEmpty(result)) return "";
        return CONFIDENCE_PATTERN.matcher(result).replaceAll("").trim();
    }

    private static String extractConfidence(String result) {
        if (TextUtils.isEmpty(result)) return "--";
        Matcher matcher = CONFIDENCE_PATTERN.matcher(result);
        return matcher.find() ? matcher.group(1) : "--";
    }

    private static boolean isValidEquipmentName(String equipmentName) {
        return !TextUtils.isEmpty(equipmentName)
                && !"正在识别...".equals(equipmentName)
                && !equipmentName.contains("未识别")
                && !equipmentName.contains("识别中");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
    }
}
