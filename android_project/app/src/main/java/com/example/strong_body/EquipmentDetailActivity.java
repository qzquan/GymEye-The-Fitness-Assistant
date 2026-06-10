package com.example.strong_body;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 器材详情页面
 * 显示教学视频、器材图片和肌肉解剖图
 */
public class EquipmentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EQUIPMENT_NAME = "equipment_name";

    private VideoView videoView;
    private ImageView ivVideoCover;
    private ImageView ivAnatomyLarge;
    private TextView tvEquipmentName;
    private TextView tvDescription;
    private TextView tvDifficulty;
    private TextView tvTips;
    private TextView tvPrimaryMuscles;
    private TextView tvSecondaryMuscles;
    private TextView tvVideoPlaceholder;
    private TextView tvAnatomyPlaceholder;
    private TextView tvAnatomyMuscleSummary;
    private ScrollView scrollView;

    private RecyclerView rvRecommendedExercises;
    private LinearLayout layoutExerciseDetail;
    private TextView tvDetailExerciseName;
    private LinearLayout layoutSteps;
    private LinearLayout layoutMistakes;
    private TextView tvSafetyTips;
    private ChipGroup chipGroupSuitableFor;

    private ExecutorService networkExecutor;
    private Equipment currentEquipment;

    // 自定义视频控制条
    private View videoControls;
    private ImageButton btnPlayPause;
    private SeekBar seekBarVideo;
    private TextView tvVideoTime;
    private android.os.Handler videoHandler;
    private boolean isSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_equipment_detail);

        Intent intent = getIntent();
        String equipmentName = intent.getStringExtra(EXTRA_EQUIPMENT_NAME);
        if (equipmentName == null) {
            finish();
            return;
        }

        currentEquipment = EquipmentRepository.getEquipmentByName(equipmentName);
        if (currentEquipment == null) {
            finish();
            return;
        }

        networkExecutor = Executors.newSingleThreadExecutor();
        videoHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        initViews();
        bindData(currentEquipment);
        setupMediaImages(currentEquipment);
        setupVideoPlayer(currentEquipment);
        setupRecommendedExercises(currentEquipment);
    }

    private void initViews() {
        videoView = findViewById(R.id.videoView);
        ivVideoCover = findViewById(R.id.ivVideoCover);
        ivAnatomyLarge = findViewById(R.id.ivAnatomyLarge);
        tvEquipmentName = findViewById(R.id.tvEquipmentName);
        tvDescription = findViewById(R.id.tvDescription);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvTips = findViewById(R.id.tvTips);
        tvPrimaryMuscles = findViewById(R.id.tvPrimaryMuscles);
        tvSecondaryMuscles = findViewById(R.id.tvSecondaryMuscles);
        tvVideoPlaceholder = findViewById(R.id.tvVideoPlaceholder);
        tvAnatomyPlaceholder = findViewById(R.id.tvAnatomyPlaceholder);
        tvAnatomyMuscleSummary = findViewById(R.id.tvAnatomyMuscleSummary);
        scrollView = findViewById(R.id.scrollView);

        rvRecommendedExercises = findViewById(R.id.rvRecommendedExercises);
        layoutExerciseDetail = findViewById(R.id.layoutExerciseDetail);
        tvDetailExerciseName = findViewById(R.id.tvDetailExerciseName);
        layoutSteps = findViewById(R.id.layoutSteps);
        layoutMistakes = findViewById(R.id.layoutMistakes);
        tvSafetyTips = findViewById(R.id.tvSafetyTips);
        chipGroupSuitableFor = findViewById(R.id.chipGroupSuitableFor);

        // 自定义视频控制条
        videoControls = findViewById(R.id.videoControls);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        seekBarVideo = findViewById(R.id.seekBarVideo);
        tvVideoTime = findViewById(R.id.tvVideoTime);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReplay).setOnClickListener(v -> {
            if (videoView != null) {
                videoView.seekTo(0);
                videoView.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                showVideoControls();
            }
        });
        findViewById(R.id.btnCloseDetail).setOnClickListener(v -> hideExerciseDetail());
    }

    private void bindData(Equipment equipment) {
        tvEquipmentName.setText(equipment.getName());
        tvDescription.setText(equipment.getDescription());
        tvDifficulty.setText("难度: " + equipment.getDifficulty());
        tvTips.setText(equipment.getTips());

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

        List<String> secondaryMuscles = equipment.getSecondaryMuscles();
        String secondaryText = "";
        if (secondaryMuscles != null && !secondaryMuscles.isEmpty()) {
            StringBuilder secondaryBuilder = new StringBuilder("辅助锻炼: ");
            for (int i = 0; i < secondaryMuscles.size(); i++) {
                String muscleCn = EquipmentRepository.getMuscleNameCn(secondaryMuscles.get(i));
                secondaryBuilder.append(muscleCn);
                if (i < secondaryMuscles.size() - 1) {
                    secondaryBuilder.append("、");
                }
            }
            secondaryText = secondaryBuilder.toString();
            tvSecondaryMuscles.setText(secondaryText);
            tvSecondaryMuscles.setVisibility(View.VISIBLE);
        } else {
            tvSecondaryMuscles.setVisibility(View.GONE);
        }

        String anatomySummary = secondaryText.isEmpty()
                ? primaryBuilder.toString()
                : primaryBuilder + "\n" + secondaryText;
        tvAnatomyMuscleSummary.setText(anatomySummary);
    }

    private void setupMediaImages(Equipment equipment) {
        int anatomyResId = EquipmentImageResolver.getAnatomyResId(this, equipment);
        showImageOrPlaceholder(
                ivAnatomyLarge,
                tvAnatomyPlaceholder,
                anatomyResId,
                "请将 " + equipment.getName() + " 的彩色肌肉解剖图放入 drawable-nodpi"
        );
    }

    private void showImageOrPlaceholder(ImageView imageView, TextView placeholder, int resId, String text) {
        if (resId != 0) {
            imageView.setImageResource(resId);
            imageView.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
        } else {
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            placeholder.setText(text);
        }
    }

    private void setupVideoPlayer(Equipment equipment) {
        Uri localUri = resolveVideoUri(equipment);
        if (localUri != null) {
            playVideo(localUri);
            return;
        }

        if (equipment.getBackendId() > 0) {
            fetchAndPlayBackendVideo(equipment);
        } else {
            showVideoUnavailable();
        }
    }

    private Uri resolveVideoUri(Equipment equipment) {
        Uri rawUri = tryRawResource(equipment.getId());
        if (rawUri != null) return rawUri;

        String videoUrl = equipment.getVideoUrl();
        if (videoUrl != null && !videoUrl.isEmpty()
                && !videoUrl.startsWith("https://example.com")) {
            return Uri.parse(videoUrl);
        }
        return null;
    }

    private Uri tryRawResource(String equipmentId) {
        String resourceName = equipmentId + "_demo";
        int resId = getResources().getIdentifier(resourceName, "raw", getPackageName());
        if (resId == 0) return null;

        File outFile = new File(getCacheDir(), resourceName + ".mp4");
        if (!outFile.exists() || outFile.length() == 0) {
            try (InputStream input = getResources().openRawResource(resId);
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

    private void fetchAndPlayBackendVideo(Equipment equipment) {
        showVideoLoading();
        int backendId = equipment.getBackendId();

        networkExecutor.execute(() -> {
            try {
                String token = AuthAccountStorage.getSessionToken(this);
                GymEyeApiClient.HttpResult result = GymEyeApiClient.get(
                        "/api/exercise-videos/exercise/" + backendId, token);

                if (result.code == 200) {
                    JSONObject json = result.jsonOrEmpty();
                    JSONArray data = json.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        JSONObject firstVideo = data.getJSONObject(0);
                        String url = firstVideo.optString("url", "");
                        if (!url.isEmpty()) {
                            runOnUiThread(() -> playVideo(Uri.parse(url)));
                            return;
                        }
                    }
                }
                runOnUiThread(this::showVideoUnavailable);
            } catch (Exception e) {
                runOnUiThread(this::showVideoUnavailable);
            }
        });
    }

    private void playVideo(Uri videoUri) {
        try {
            showVideoLoading();
            videoView.setVideoURI(videoUri);
            videoView.setVisibility(View.VISIBLE);
            ivVideoCover.setVisibility(View.GONE);

            // 不使用 MediaController，用自定义控制条
            videoView.setMediaController(null);

            // 播放/暂停按钮
            btnPlayPause.setOnClickListener(v -> {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    videoView.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                }
                showVideoControls();
            });

            // SeekBar 拖动
            seekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && videoView != null) {
                        int duration = videoView.getDuration();
                        if (duration > 0) {
                            videoView.seekTo((int) (duration * progress / 100.0));
                        }
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isSeeking = true;
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isSeeking = false;
                    showVideoControls();
                }
            });

            // 点击视频区域显示/隐藏控制条
            videoView.setOnClickListener(v -> {
                if (videoControls.getVisibility() == View.VISIBLE) {
                    videoControls.setVisibility(View.GONE);
                } else {
                    showVideoControls();
                }
            });

            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(false);
                hideVideoPlaceholder();
                View infoBar = findViewById(R.id.videoInfoBar);
                if (infoBar != null) infoBar.setVisibility(View.GONE);
                videoView.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                showVideoControls();
                startProgressUpdate();
            });

            videoView.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                videoControls.setVisibility(View.VISIBLE);
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                showVideoError();
                return true;
            });
        } catch (Exception e) {
            showVideoError();
        }
    }

    private void showVideoControls() {
        if (videoControls != null) {
            videoControls.setVisibility(View.VISIBLE);
            videoHandler.removeCallbacks(hideControlsRunnable);
            videoHandler.postDelayed(hideControlsRunnable, 4000);
        }
    }

    private final Runnable hideControlsRunnable = () -> {
        if (videoView != null && videoView.isPlaying() && !isSeeking) {
            if (videoControls != null) videoControls.setVisibility(View.GONE);
        }
    };

    private void startProgressUpdate() {
        videoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (videoView != null && videoView.getDuration() > 0) {
                    int current = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();
                    tvVideoTime.setText(formatTime(current) + " / " + formatTime(duration));
                    if (!isSeeking) {
                        seekBarVideo.setProgress((int) (current * 100L / duration));
                    }
                }
                videoHandler.postDelayed(this, 500);
            }
        }, 500);
    }

    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void showVideoLoading() {
        if (tvVideoPlaceholder != null) {
            tvVideoPlaceholder.setVisibility(View.VISIBLE);
            tvVideoPlaceholder.setText("视频加载中...\n请稍后");
        }
    }

    private void showVideoUnavailable() {
        showVideoFallback("暂无教学视频");
    }

    private void showVideoError() {
        showVideoFallback("视频加载失败\n请检查网络后重试");
    }

    private void showVideoFallback(String message) {
        if (videoView != null) {
            videoView.setVisibility(View.GONE);
        }
        int coverResId = EquipmentImageResolver.getCoverResId(this, currentEquipment);
        if (coverResId != 0) {
            ivVideoCover.setImageResource(coverResId);
            ivVideoCover.setVisibility(View.VISIBLE);
            tvVideoPlaceholder.setVisibility(View.GONE);
        } else if (tvVideoPlaceholder != null) {
            ivVideoCover.setImageDrawable(null);
            ivVideoCover.setVisibility(View.GONE);
            tvVideoPlaceholder.setVisibility(View.VISIBLE);
            tvVideoPlaceholder.setText(currentEquipment.getName() + "\n" + message + "\n封面待补充");
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
            scrollView.post(() -> scrollView.smoothScrollTo(0, layoutExerciseDetail.getTop()));
        });
        rvRecommendedExercises.setAdapter(adapter);
    }

    private void showExerciseDetail(Exercise exercise) {
        layoutExerciseDetail.setVisibility(View.VISIBLE);
        tvDetailExerciseName.setText(exercise.getName());

        layoutSteps.removeAllViews();
        layoutMistakes.removeAllViews();
        chipGroupSuitableFor.removeAllViews();

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

        tvSafetyTips.setText(exercise.getSafetyTips());

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

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdownNow();
        }
    }
}
