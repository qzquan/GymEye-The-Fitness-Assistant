package com.example.strong_body;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
    private TextView tvVideoPlaceholder;
    private ScrollView scrollView;

    private RecyclerView rvRecommendedExercises;
    private LinearLayout layoutExerciseDetail;
    private TextView tvDetailExerciseName;
    private LinearLayout layoutSteps;
    private LinearLayout layoutMistakes;
    private TextView tvSafetyTips;
    private ChipGroup chipGroupSuitableFor;

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
        setupRecommendedExercises(equipment);
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
        tvVideoPlaceholder = findViewById(R.id.tvVideoPlaceholder);
        scrollView = findViewById(R.id.scrollView);

        rvRecommendedExercises = findViewById(R.id.rvRecommendedExercises);
        layoutExerciseDetail = findViewById(R.id.layoutExerciseDetail);
        tvDetailExerciseName = findViewById(R.id.tvDetailExerciseName);
        layoutSteps = findViewById(R.id.layoutSteps);
        layoutMistakes = findViewById(R.id.layoutMistakes);
        tvSafetyTips = findViewById(R.id.tvSafetyTips);
        chipGroupSuitableFor = findViewById(R.id.chipGroupSuitableFor);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 重新播放按钮
        findViewById(R.id.btnReplay).setOnClickListener(v -> {
            if (videoView != null) {
                videoView.seekTo(0);
                videoView.start();
            }
        });

        // 关闭动作详情
        findViewById(R.id.btnCloseDetail).setOnClickListener(v -> hideExerciseDetail());
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
        Uri videoUri = resolveVideoUri(equipment);

        if (videoUri != null) {
            try {
                showVideoLoading();
                videoView.setVideoURI(videoUri);

                // 添加播放控制
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(videoView);
                videoView.setMediaController(mediaController);

                // 视频准备完成回调
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(false);
                    hideVideoPlaceholder();
                    videoView.start();
                });

                // 视频播放错误处理
                videoView.setOnErrorListener((mp, what, extra) -> {
                    showVideoError();
                    return true;
                });

            } catch (Exception e) {
                showVideoError();
            }
        } else {
            showVideoUnavailable();
        }
    }

    private Uri resolveVideoUri(Equipment equipment) {
        if ("leg_extension".equals(equipment.getId())) {
            return copyRawVideoToCache();
        }

        String videoUrl = equipment.getVideoUrl();
        if (videoUrl != null && !videoUrl.startsWith("https://example.com")) {
            return Uri.parse(videoUrl);
        }
        return null;
    }

    private Uri copyRawVideoToCache() {
        File outFile = new File(getCacheDir(), "leg_extension_demo.mp4");
        if (!outFile.exists() || outFile.length() == 0) {
            try (InputStream input = getResources().openRawResource(R.raw.leg_extension_demo);
                 FileOutputStream output = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return Uri.fromFile(outFile);
    }

    private void showVideoLoading() {
        if (tvVideoPlaceholder != null) {
            tvVideoPlaceholder.setVisibility(View.VISIBLE);
            tvVideoPlaceholder.setText("视频加载中...\n请稍后");
        }
    }

    private void showVideoUnavailable() {
        if (tvVideoPlaceholder != null) {
            tvVideoPlaceholder.setVisibility(View.VISIBLE);
            tvVideoPlaceholder.setText("暂无教学视频");
        }
    }

    private void showVideoError() {
        if (tvVideoPlaceholder != null) {
            tvVideoPlaceholder.setVisibility(View.VISIBLE);
            tvVideoPlaceholder.setText("视频加载失败\n请检查网络后重试");
        }
    }

    private void hideVideoPlaceholder() {
        if (tvVideoPlaceholder != null) {
            tvVideoPlaceholder.setVisibility(View.GONE);
        }
    }

    private void setupRecommendedExercises(Equipment equipment) {
        List<Exercise> exercises = equipment.getRecommendedExercises();
        if (exercises == null || exercises.isEmpty()) {
            return;
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvRecommendedExercises.setLayoutManager(layoutManager);

        ExerciseCardAdapter adapter = new ExerciseCardAdapter(exercises);
        adapter.setOnExerciseClickListener((exercise, position) -> {
            showExerciseDetail(exercise);
            // 滚动到详情区
            scrollView.post(() -> scrollView.smoothScrollTo(0, layoutExerciseDetail.getTop()));
        });
        rvRecommendedExercises.setAdapter(adapter);
    }

    private void showExerciseDetail(Exercise exercise) {
        layoutExerciseDetail.setVisibility(View.VISIBLE);
        tvDetailExerciseName.setText(exercise.getName());

        // 清空旧内容
        layoutSteps.removeAllViews();
        layoutMistakes.removeAllViews();
        chipGroupSuitableFor.removeAllViews();

        // 动作步骤
        List<String> steps = exercise.getSteps();
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                TextView stepView = new TextView(this);
                stepView.setText((i + 1) + ". " + steps.get(i));
                stepView.setTextColor(0xFF444444);
                stepView.setTextSize(14);
                stepView.setLineSpacing(0, 1.2f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
                stepView.setLayoutParams(lp);
                layoutSteps.addView(stepView);
            }
        }

        // 常见错误
        List<String> mistakes = exercise.getCommonMistakes();
        if (mistakes != null) {
            for (String mistake : mistakes) {
                TextView errorView = new TextView(this);
                errorView.setText("• " + mistake);
                errorView.setTextColor(0xFFC62828);
                errorView.setTextSize(14);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
                errorView.setLayoutParams(lp);
                layoutMistakes.addView(errorView);
            }
        }

        // 安全提示
        tvSafetyTips.setText(exercise.getSafetyTips());

        // 适合人群 Chip
        List<String> suitableFor = exercise.getSuitableFor();
        if (suitableFor != null) {
            for (String tag : suitableFor) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setClickable(false);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getSuitableForColor(tag)));
                chip.setTextColor(getSuitableForTextColor(tag));
                chipGroupSuitableFor.addView(chip);
            }
        }
    }

    private void hideExerciseDetail() {
        layoutExerciseDetail.setVisibility(View.GONE);
    }

    private int getSuitableForColor(String tag) {
        if (tag == null) return 0xFFE0E0E0;
        switch (tag) {
            case "新手": return 0xFFE3F2FD;
            case "进阶": return 0xFFFFF3E0;
            case "康复": return 0xFFFFEBEE;
            default: return 0xFFE0E0E0;
        }
    }

    private int getSuitableForTextColor(String tag) {
        if (tag == null) return 0xFF333333;
        switch (tag) {
            case "新手": return 0xFF1565C0;
            case "进阶": return 0xFFE65100;
            case "康复": return 0xFFC62828;
            default: return 0xFF333333;
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
