package com.example.strong_body;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
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

    private PreviewView viewFinder;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private MaterialButton btnAnalyze;
    private MaterialButton btnTorchControl;
    private TextView tvScanTip;
    private TextView tvDetectedName;
    private TextView tvConfidence;
    private TextView tvStepDetect;
    private TextView tvStepMatch;
    private TextView tvStepResult;
    private TextView btnZoom05;
    private TextView btnZoom1;
    private TextView btnZoom2;
    private ImageButton btnTorch;

    private YOLOv8Detector yoloDetector;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private boolean torchEnabled = false;

    private String currentResult = "";

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
        tvStepDetect = findViewById(R.id.tvStepDetect);
        tvStepMatch = findViewById(R.id.tvStepMatch);
        tvStepResult = findViewById(R.id.tvStepResult);
        btnZoom05 = findViewById(R.id.btnZoom05);
        btnZoom1 = findViewById(R.id.btnZoom1);
        btnZoom2 = findViewById(R.id.btnZoom2);
        btnTorch = findViewById(R.id.btnTorch);
        btnTorchControl = findViewById(R.id.btnTorchControl);
        ImageButton btnBack = findViewById(R.id.btnScanBack);
        btnBack.setOnClickListener(v -> finish());
        btnAnalyze.setEnabled(false);
        btnAnalyze.setAlpha(0.62f);
        btnAnalyze.setText("保持画面清晰");
        bindScannerControls();

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
                    currentResult = result;

                    runOnUiThread(() -> updateRecognitionUi(result));

                    imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                runOnUiThread(this::updateTorchAvailability);

            } catch (Exception e) {
                Log.e("ScanActivity", "相机启动失败", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindScannerControls() {
        bindZoomButton(btnZoom05, 0.5f);
        bindZoomButton(btnZoom1, 1f);
        bindZoomButton(btnZoom2, 2f);

        View.OnClickListener torchClick = v -> toggleTorch();
        btnTorch.setOnClickListener(torchClick);
        btnTorchControl.setOnClickListener(torchClick);
        btnTorch.setEnabled(false);
        btnTorch.setAlpha(0.48f);
        btnTorchControl.setEnabled(false);
        btnTorchControl.setAlpha(0.48f);
        setZoomSelected(btnZoom1);
        setScanningSteps(false);
    }

    private void bindZoomButton(TextView button, float ratio) {
        button.setOnClickListener(v -> {
            if (camera == null) {
                Toast.makeText(this, "相机准备中", Toast.LENGTH_SHORT).show();
                return;
            }
            setZoomRatio(ratio);
            setZoomSelected(button);
        });
    }

    private void setZoomRatio(float targetRatio) {
        try {
            Float min = camera.getCameraInfo().getZoomState().getValue() == null
                    ? null
                    : camera.getCameraInfo().getZoomState().getValue().getMinZoomRatio();
            Float max = camera.getCameraInfo().getZoomState().getValue() == null
                    ? null
                    : camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
            float ratio = targetRatio;
            if (min != null) ratio = Math.max(ratio, min);
            if (max != null) ratio = Math.min(ratio, max);
            camera.getCameraControl().setZoomRatio(ratio);
        } catch (Exception e) {
            Toast.makeText(this, "当前设备不支持该缩放", Toast.LENGTH_SHORT).show();
        }
    }

    private void setZoomSelected(TextView selected) {
        TextView[] buttons = {btnZoom05, btnZoom1, btnZoom2};
        for (TextView button : buttons) {
            boolean active = button == selected;
            button.setBackgroundResource(active ? R.drawable.bg_scan_zoom_selected : R.drawable.bg_scan_zoom_unselected);
            button.setTextColor(Color.parseColor(active ? "#071312" : "#C9D1D9"));
        }
    }

    private void toggleTorch() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "当前设备不支持闪光灯", Toast.LENGTH_SHORT).show();
            return;
        }
        torchEnabled = !torchEnabled;
        camera.getCameraControl().enableTorch(torchEnabled);
        btnTorchControl.setText(torchEnabled ? "关闭照明" : "轻触照亮");
        btnTorch.setAlpha(torchEnabled ? 1f : 0.78f);
        btnTorchControl.setAlpha(1f);
    }

    private void updateTorchAvailability() {
        boolean hasFlash = camera != null && camera.getCameraInfo().hasFlashUnit();
        btnTorch.setEnabled(hasFlash);
        btnTorchControl.setEnabled(hasFlash);
        btnTorch.setAlpha(hasFlash ? 0.78f : 0.35f);
        btnTorchControl.setAlpha(hasFlash ? 1f : 0.48f);
    }

    private Bitmap rotateBitmap(Bitmap source, int angle) {
        if (angle == 0) return source;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void updateRecognitionUi(String result) {
        String equipmentName = extractEquipmentName(result);
        if (isValidEquipmentName(equipmentName)) {
            tvDetectedName.setText(equipmentName);
            tvConfidence.setText("置信度 " + extractConfidence(result));
            tvScanTip.setText("识别成功，可查看器械详情");
            btnAnalyze.setEnabled(true);
            btnAnalyze.setAlpha(1f);
            btnAnalyze.setText("查看器械详情");
            setScanningSteps(true);
        } else {
            tvDetectedName.setText("正在识别器械…");
            tvConfidence.setText("请保持画面清晰，耐心等待");
            tvScanTip.setText("将器械主体或铭牌放入扫描框");
            btnAnalyze.setEnabled(false);
            btnAnalyze.setAlpha(0.62f);
            btnAnalyze.setText("保持画面清晰");
            setScanningSteps(false);
        }
    }

    private void setScanningSteps(boolean success) {
        tvStepDetect.setBackgroundResource(success ? R.drawable.bg_scan_step_inactive : R.drawable.bg_scan_step_active);
        tvStepDetect.setTextColor(Color.parseColor(success ? "#C9D1D9" : "#071312"));
        tvStepMatch.setBackgroundResource(success ? R.drawable.bg_scan_step_inactive : R.drawable.bg_scan_step_active);
        tvStepMatch.setTextColor(Color.parseColor(success ? "#C9D1D9" : "#071312"));
        tvStepResult.setBackgroundResource(success ? R.drawable.bg_scan_step_active : R.drawable.bg_scan_step_inactive);
        tvStepResult.setTextColor(Color.parseColor(success ? "#071312" : "#C9D1D9"));
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
        cameraExecutor.shutdown();
    }
}
