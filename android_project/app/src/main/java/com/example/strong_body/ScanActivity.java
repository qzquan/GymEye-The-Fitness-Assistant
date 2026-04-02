package com.example.strong_body;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class ScanActivity extends AppCompatActivity {

    // 相机权限常量，统一管理，避免硬编码
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    // 预览控件：承接 CameraX 输出画面
    private PreviewView viewFinder;

    // 动态权限请求启动器
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 扫描页使用沉浸式内容区域，让预览真正铺满全屏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_scan);

        // 隐藏 ActionBar，避免顶部预留空间影响扫码页视觉
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        viewFinder = findViewById(R.id.viewFinder);
        Button btnAnalyze = findViewById(R.id.btnAnalyze);

        // “开始分析”占位逻辑：后续可在这里接入识别/上传/解析流程
        btnAnalyze.setOnClickListener(v ->
                Toast.makeText(this, "开始分析（功能待接入）", Toast.LENGTH_SHORT).show()
        );

        // 初始化权限回调：授权后启动相机，拒绝则提示用户
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (isGranted) {
                                startCamera();
                            } else {
                                Toast.makeText(
                                        this,
                                        "相机权限被拒绝，无法开启扫码预览",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });

        // 先检查权限，已授权则直接初始化预览
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(CAMERA_PERMISSION);
        }
    }

    /**
     * 检查是否已经授予相机权限。
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 初始化 CameraX：选择后置摄像头并绑定预览到当前页面生命周期。
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 预览用例：将图像流渲染到 PreviewView
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 指定使用后置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 每次绑定前先解绑，避免重复绑定报错
                cameraProvider.unbindAll();

                // 与 Activity 生命周期绑定：界面可见时自动打开，不可见时自动释放
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview
                );
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(
                        this,
                        "相机初始化失败：" + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                // 中断异常需恢复中断状态，避免吞掉中断信号
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } catch (@NonNull IllegalArgumentException e) {
                Toast.makeText(
                        this,
                        "后置摄像头不可用：" + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}
