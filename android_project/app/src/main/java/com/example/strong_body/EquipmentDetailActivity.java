package com.example.strong_body;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 器材详情页面
 * 显示教学视频和肌肉发力图
 */
public class EquipmentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EQUIPMENT_NAME = "equipment_name";

    private MuscleView muscleView;
    private VideoView videoView;
    private TextView tvEquipmentName;
    private TextView tvDescription;
    private TextView tvDifficulty;
    private TextView tvTips;
    private TextView tvPrimaryMuscles;
    private TextView tvSecondaryMuscles;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏沉浸式
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_equipment_detail);

        // 获取传递的器材名称
        Intent intent = getIntent();
        String equipmentName = intent.getStringExtra(EXTRA_EQUIPMENT_NAME);

        if (equipmentName == null) {
            finish();
            return;
        }

        // 获取器材数据
        Equipment equipment = EquipmentRepository.getEquipmentByName(equipmentName);
        if (equipment == null) {
            finish();
            return;
        }

        initViews();
        bindData(equipment);
        setupVideoPlayer(equipment);
        setupMuscleDiagram(equipment);
    }

    private void initViews() {
        muscleView = findViewById(R.id.muscleView);
        videoView = findViewById(R.id.videoView);
        tvEquipmentName = findViewById(R.id.tvEquipmentName);
        tvDescription = findViewById(R.id.tvDescription);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvTips = findViewById(R.id.tvTips);
        tvPrimaryMuscles = findViewById(R.id.tvPrimaryMuscles);
        tvSecondaryMuscles = findViewById(R.id.tvSecondaryMuscles);
        scrollView = findViewById(R.id.scrollView);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 重新播放按钮
        findViewById(R.id.btnReplay).setOnClickListener(v -> {
            if (videoView != null) {
                videoView.seekTo(0);
                videoView.start();
            }
        });
    }

    private void bindData(Equipment equipment) {
        tvEquipmentName.setText(equipment.getName());
        tvDescription.setText(equipment.getDescription());
        tvDifficulty.setText("难度: " + equipment.getDifficulty());

        // 设置提示文本
        String tipsText = equipment.getTips();
        tvTips.setText("使用技巧\n" + tipsText);

        // 显示主要肌肉群
        List<String> primaryMuscles = equipment.getTargetMuscles();
        StringBuilder primaryBuilder = new StringBuilder("主要锻炼: ");
        for (int i = 0; i < primaryMuscles.size(); i++) {
            String muscleCn = EquipmentRepository.getMuscleNameCn(primaryMuscles.get(i));
            primaryBuilder.append(muscleCn);
            if (i < primaryMuscles.size() - 1) {
                primaryBuilder.append("、");
            }
        }
        tvPrimaryMuscles.setText(primaryBuilder.toString());

        // 显示次要肌肉群
        List<String> secondaryMuscles = equipment.getSecondaryMuscles();
        if (secondaryMuscles != null && !secondaryMuscles.isEmpty()) {
            StringBuilder secondaryBuilder = new StringBuilder("辅助锻炼: ");
            for (int i = 0; i < secondaryMuscles.size(); i++) {
                String muscleCn = EquipmentRepository.getMuscleNameCn(secondaryMuscles.get(i));
                secondaryBuilder.append(muscleCn);
                if (i < secondaryMuscles.size() - 1) {
                    secondaryBuilder.append("、");
                }
            }
            tvSecondaryMuscles.setText(secondaryBuilder.toString());
            tvSecondaryMuscles.setVisibility(View.VISIBLE);
        } else {
            tvSecondaryMuscles.setVisibility(View.GONE);
        }
    }

    private void setupVideoPlayer(Equipment equipment) {
        // 设置视频URL（这里使用示例URL，实际项目中需要替换为真实视频URL）
        String videoUrl = equipment.getVideoUrl();

        // 检查是否是有效的视频URL
        if (videoUrl != null && !videoUrl.startsWith("https://example.com")) {
            try {
                videoView.setVideoURI(Uri.parse(videoUrl));

                // 添加播放控制
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(videoView);
                videoView.setMediaController(mediaController);

                // 视频准备完成回调
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(false);
                    // 自动播放
                    // videoView.start();
                });

                // 视频播放错误处理
                videoView.setOnErrorListener((mp, what, extra) -> {
                    // 显示错误提示或使用本地占位图
                    showVideoPlaceholder();
                    return true;
                });

            } catch (Exception e) {
                showVideoPlaceholder();
            }
        } else {
            // 示例URL或无效URL，显示占位图
            showVideoPlaceholder();
        }
    }

    private void showVideoPlaceholder() {
        // 视频不可用时显示占位提示
        TextView placeholder = findViewById(R.id.tvVideoPlaceholder);
        if (placeholder != null) {
            placeholder.setVisibility(View.VISIBLE);
            placeholder.setText("视频加载中...\n请稍后");
        }
    }

    private void setupMuscleDiagram(Equipment equipment) {
        // 将英文肌肉名称转换为肌肉图识别的格式
        Set<String> primarySet = new HashSet<>(equipment.getTargetMuscles());
        Set<String> secondarySet = new HashSet<>();
        if (equipment.getSecondaryMuscles() != null) {
            secondarySet = new HashSet<>(equipment.getSecondaryMuscles());
        }

        // 设置肌肉高亮
        muscleView.setMuscles(primarySet, secondarySet);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停视频
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放视频资源
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
