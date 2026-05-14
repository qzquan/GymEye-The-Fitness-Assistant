package com.example.strong_body;

import java.util.List;

public class Exercise {
    private String id;
    private String name;
    private String description;
    private List<String> steps;
    private List<String> commonMistakes;
    private String safetyTips;
    private List<String> suitableFor;
    private String difficulty;
    private String videoUrl;

    public Exercise(String id, String name, String description, List<String> steps,
                    List<String> commonMistakes, String safetyTips,
                    List<String> suitableFor, String difficulty) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.steps = steps;
        this.commonMistakes = commonMistakes;
        this.safetyTips = safetyTips;
        this.suitableFor = suitableFor;
        this.difficulty = difficulty;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getSteps() { return steps; }
    public List<String> getCommonMistakes() { return commonMistakes; }
    public String getSafetyTips() { return safetyTips; }
    public List<String> getSuitableFor() { return suitableFor; }
    public String getDifficulty() { return difficulty; }
    public String getVideoUrl() { return videoUrl; }
}
