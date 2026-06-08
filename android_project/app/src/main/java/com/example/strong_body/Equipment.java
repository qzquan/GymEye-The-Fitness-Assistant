package com.example.strong_body;

import java.util.Arrays;
import java.util.List;

/**
 * 健身器材数据模型
 */
public class Equipment {
    private String id;
    private String name;                    // 器材名称
    private String description;             // 器材描述
    private String videoUrl;                // 教学视频URL
    private List<String> targetMuscles;    // 主要锻炼的肌肉群
    private List<String> secondaryMuscles; // 次要锻炼的肌肉群
    private String difficulty;              // 难度等级
    private String tips;                    // 使用技巧
    private List<Exercise> recommendedExercises; // 推荐动作
    private int backendId;                  // 后端数据库对应的数字ID

    public Equipment(String id, String name, String description, String videoUrl,
                     List<String> targetMuscles, List<String> secondaryMuscles,
                     String difficulty, String tips) {
        this(id, name, description, videoUrl, targetMuscles, secondaryMuscles,
             difficulty, tips, null, 0);
    }

    public Equipment(String id, String name, String description, String videoUrl,
                     List<String> targetMuscles, List<String> secondaryMuscles,
                     String difficulty, String tips, List<Exercise> recommendedExercises) {
        this(id, name, description, videoUrl, targetMuscles, secondaryMuscles,
             difficulty, tips, recommendedExercises, 0);
    }

    public Equipment(String id, String name, String description, String videoUrl,
                     List<String> targetMuscles, List<String> secondaryMuscles,
                     String difficulty, String tips, List<Exercise> recommendedExercises,
                     int backendId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.videoUrl = videoUrl;
        this.targetMuscles = targetMuscles;
        this.secondaryMuscles = secondaryMuscles;
        this.difficulty = difficulty;
        this.tips = tips;
        this.recommendedExercises = recommendedExercises;
        this.backendId = backendId;
    }

    // Getter方法
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVideoUrl() { return videoUrl; }
    public List<String> getTargetMuscles() { return targetMuscles; }
    public List<String> getSecondaryMuscles() { return secondaryMuscles; }
    public String getDifficulty() { return difficulty; }
    public String getTips() { return tips; }
    public List<Exercise> getRecommendedExercises() { return recommendedExercises; }
    public int getBackendId() { return backendId; }
}
