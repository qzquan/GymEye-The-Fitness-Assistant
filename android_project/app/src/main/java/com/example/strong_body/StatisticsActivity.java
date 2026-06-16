package com.example.strong_body;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsActivity extends AppCompatActivity {

    private static final int HEAT_COLUMNS = 18;

    private TextView tvWeeklyCount, tvTotalDuration, tvTopBodyPart, tvStreakDays, tvMonthLabel;
    private LineChart weightLineChart;
    private GridLayout calendarGrid;
    private LinearLayout recentRecordsContainer;
    private ChipGroup chipGroupEquipment, chipGroupMuscleFilter;
    private EditText etSearchExercise;
    private Button btnCheckIn;

    private JSONObject statsData;
    private Map<String, Object> weightProgression;
    private final List<JSONObject> workoutLogs = new ArrayList<>();
    private final List<Equipment> allEquipment = new ArrayList<>();
    private final List<Equipment> visibleEquipment = new ArrayList<>();
    private String selectedExerciseName = "";
    private String selectedMuscleFilter = "";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        tvWeeklyCount = findViewById(R.id.tvWeeklyCount);
        tvTotalDuration = findViewById(R.id.tvTotalDuration);
        tvTopBodyPart = findViewById(R.id.tvTopBodyPart);
        tvStreakDays = findViewById(R.id.tvStreakDays);
        tvMonthLabel = findViewById(R.id.tvMonthLabel);
        weightLineChart = findViewById(R.id.weightLineChart);
        calendarGrid = findViewById(R.id.calendarGrid);
        recentRecordsContainer = findViewById(R.id.recentRecordsContainer);
        chipGroupEquipment = findViewById(R.id.chipGroupEquipment);
        chipGroupMuscleFilter = findViewById(R.id.chipGroupMuscleFilter);
        etSearchExercise = findViewById(R.id.etSearchExercise);
        btnCheckIn = findViewById(R.id.btnCheckIn);

        allEquipment.addAll(EquipmentRepository.getAllEquipment());
        if (!allEquipment.isEmpty()) {
            selectedExerciseName = allEquipment.get(0).getName();
        }

        setupChartBaseStyle();
        setupExerciseFilters();
        showLoadingState();
        loadData();

        btnCheckIn.setOnClickListener(v -> {
            String exercise = TextUtils.isEmpty(selectedExerciseName)
                    ? getFallbackExerciseName()
                    : selectedExerciseName;
            WorkoutLogBottomSheet.newInstance(exercise, this::loadData)
                    .show(getSupportFragmentManager(), "WorkoutLog");
        });
    }

    private void showLoadingState() {
        tvWeeklyCount.setText("...");
        tvTotalDuration.setText("...");
        tvTopBodyPart.setText("...");
        tvStreakDays.setText("...");
    }

    private void setupExerciseFilters() {
        addMuscleChip("全部", "", true);
        addMuscleChip("胸部", "chest", false);
        addMuscleChip("背部", "back", false);
        addMuscleChip("腿部", "legs", false);
        addMuscleChip("肩部", "shoulders", false);
        addMuscleChip("手臂", "arms", false);

        etSearchExercise.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyExerciseFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        applyExerciseFilters();
    }

    private void addMuscleChip(String label, String value, boolean checked) {
        Chip chip = createChoiceChip(label);
        chip.setTag(value);
        chip.setChecked(checked);
        chip.setOnClickListener(v -> {
            selectedMuscleFilter = String.valueOf(v.getTag());
            applyExerciseFilters();
        });
        chipGroupMuscleFilter.addView(chip);
    }

    private void applyExerciseFilters() {
        String query = etSearchExercise.getText() == null
                ? ""
                : etSearchExercise.getText().toString().trim().toLowerCase();
        visibleEquipment.clear();

        for (Equipment equipment : allEquipment) {
            if (!TextUtils.isEmpty(selectedMuscleFilter)
                    && !matchesMuscleFilter(equipment, selectedMuscleFilter)) {
                continue;
            }
            if (!TextUtils.isEmpty(query) && !matchesExerciseQuery(equipment, query)) {
                continue;
            }
            visibleEquipment.add(equipment);
        }

        if (visibleEquipment.isEmpty()) {
            selectedExerciseName = "";
        } else if (TextUtils.isEmpty(selectedExerciseName)
                || !containsEquipmentName(visibleEquipment, selectedExerciseName)) {
            selectedExerciseName = visibleEquipment.get(0).getName();
        }

        renderEquipmentChips();
        updateWeightChart(TextUtils.isEmpty(selectedExerciseName) ? getFallbackExerciseName() : selectedExerciseName);
    }

    private void renderEquipmentChips() {
        chipGroupEquipment.removeAllViews();
        if (visibleEquipment.isEmpty()) {
            Chip empty = createChoiceChip("无匹配器材");
            empty.setEnabled(false);
            chipGroupEquipment.addView(empty);
            return;
        }

        for (Equipment equipment : visibleEquipment) {
            Chip chip = createChoiceChip(equipment.getName());
            chip.setChecked(equipment.getName().equals(selectedExerciseName));
            chip.setOnClickListener(v -> {
                selectedExerciseName = equipment.getName();
                renderEquipmentChips();
                updateWeightChart(selectedExerciseName);
            });
            chipGroupEquipment.addView(chip);
        }
    }

    private Chip createChoiceChip(String label) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setTextSize(12);
        chip.setChipMinHeight(dp(36));
        chip.setChipCornerRadius(dp(18));
        chip.setChipStrokeWidth(dp(1));
        chip.setChipStrokeColorResource(R.color.gymeye_border);
        chip.setTextColor(Color.parseColor("#56616D"));
        return chip;
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                String token = AuthAccountStorage.getSessionToken(this);
                if (TextUtils.isEmpty(token)) {
                    mainHandler.post(() -> {
                        tvWeeklyCount.setText("-");
                        tvTotalDuration.setText("-");
                        tvTopBodyPart.setText("-");
                        tvStreakDays.setText("-");
                        Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                GymEyeApiClient.HttpResult statsResult = GymEyeApiClient.get("/api/workouts/stats/summary", token);
                if (statsResult.code == 200) {
                    statsData = statsResult.jsonOrEmpty().optJSONObject("data");
                }

                GymEyeApiClient.HttpResult listResult = GymEyeApiClient.get("/api/workouts?limit=10", token);
                if (listResult.code == 200) {
                    JSONObject listJson = listResult.jsonOrEmpty();
                    JSONArray rows = listJson.optJSONArray("data");
                    if (rows == null) {
                        rows = listJson.optJSONArray("rows");
                    }
                    workoutLogs.clear();
                    if (rows != null) {
                        for (int i = 0; i < rows.length(); i++) {
                            workoutLogs.add(rows.getJSONObject(i));
                        }
                    }
                }

                buildWeightProgressionFromStats();

                mainHandler.post(() -> {
                    updateSummaryCards();
                    updateWeightChart(TextUtils.isEmpty(selectedExerciseName) ? getFallbackExerciseName() : selectedExerciseName);
                    updateCalendar();
                    updateRecentRecords();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvWeeklyCount.setText("离线");
                    tvTotalDuration.setText("离线");
                    tvTopBodyPart.setText("-");
                    tvStreakDays.setText("离线");
                    Toast.makeText(this, "无法连接服务器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void buildWeightProgressionFromStats() {
        weightProgression = new HashMap<>();
        if (statsData == null) return;

        JSONObject wp = statsData.optJSONObject("weightProgression");
        if (wp == null) return;

        java.util.Iterator<String> keys = wp.keys();
        while (keys.hasNext()) {
            String exerciseName = keys.next();
            JSONArray points = wp.optJSONArray(exerciseName);
            if (points == null) continue;

            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < points.length(); i++) {
                JSONObject pt = points.optJSONObject(i);
                if (pt == null) continue;
                double weight = pt.optDouble("weight", -1);
                if (weight < 0) continue;
                entries.add(new Entry(entries.size(), (float) weight));
            }
            if (!entries.isEmpty()) {
                weightProgression.put(exerciseName, entries);
            }
        }
    }

    private void updateSummaryCards() {
        if (statsData == null) {
            tvWeeklyCount.setText("无数据");
            tvTotalDuration.setText("无数据");
            tvTopBodyPart.setText("-");
            tvStreakDays.setText("0 天");
            return;
        }

        int weeklyCount = statsData.optInt("weeklyCount", 0);
        int totalDuration = statsData.optInt("totalDurationMinutes", 0);
        int streak = statsData.optInt("streak", 0);

        tvWeeklyCount.setText(weeklyCount + " 次");
        tvStreakDays.setText(streak + " 天");

        if (totalDuration >= 60) {
            int hours = totalDuration / 60;
            int mins = totalDuration % 60;
            tvTotalDuration.setText(hours + "h " + mins + "m");
        } else {
            tvTotalDuration.setText(totalDuration + " 分钟");
        }

        JSONArray topParts = statsData.optJSONArray("topBodyParts");
        if (topParts != null && topParts.length() > 0) {
            tvTopBodyPart.setText(topParts.optJSONObject(0).optString("name", "-"));
        } else {
            tvTopBodyPart.setText("-");
        }
    }

    private void updateWeightChart(String exerciseName) {
        if (weightProgression == null || TextUtils.isEmpty(exerciseName)) {
            weightLineChart.clear();
            setupChartBaseStyle();
            weightLineChart.invalidate();
            return;
        }

        @SuppressWarnings("unchecked")
        List<Entry> entries = (List<Entry>) weightProgression.get(exerciseName);
        if (entries == null || entries.isEmpty()) {
            weightLineChart.clear();
            setupChartBaseStyle();
            weightLineChart.setNoDataText(exerciseName + " 暂无重量数据");
            weightLineChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, exerciseName + " 重量 (kg)");
        dataSet.setColor(Color.parseColor("#00BFA6"));
        dataSet.setCircleColor(Color.parseColor("#00BFA6"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#56616D"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#BFEAD9"));
        dataSet.setFillAlpha(120);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        weightLineChart.setData(new LineData(dataSet));
        weightLineChart.getAxisLeft().setGranularity(1f);
        weightLineChart.getAxisLeft().setAxisMinimum(0f);
        weightLineChart.getLegend().setTextColor(Color.parseColor("#56616D"));
        weightLineChart.animateX(800);
    }

    private void updateCalendar() {
        calendarGrid.removeAllViews();
        if (statsData == null) return;

        JSONArray calendarDays = statsData.optJSONArray("calendarDays");
        if (calendarDays == null || calendarDays.length() == 0) return;

        calendarGrid.setColumnCount(HEAT_COLUMNS);
        for (int i = 0; i < calendarDays.length(); i++) {
            JSONObject day = calendarDays.optJSONObject(i);
            if (day == null) continue;

            String date = day.optString("date", "");
            int count = day.optInt("count", 0);

            View cell = new View(this);
            cell.setBackgroundResource(getHeatBackground(count));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(14);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            cell.setLayoutParams(params);
            cell.setOnClickListener(v ->
                    Toast.makeText(this, date + ": " + count + " 次训练", Toast.LENGTH_SHORT).show());
            calendarGrid.addView(cell);
        }

        String firstDate = calendarDays.optJSONObject(0).optString("date", "");
        String lastDate = calendarDays.optJSONObject(calendarDays.length() - 1).optString("date", "");
        tvMonthLabel.setText(firstDate + " ~ " + lastDate);
    }

    private void updateRecentRecords() {
        recentRecordsContainer.removeAllViews();
        if (workoutLogs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无训练记录，快去打卡吧!");
            empty.setTextSize(14);
            empty.setTextColor(Color.parseColor("#86868B"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            recentRecordsContainer.addView(empty);
            return;
        }

        int showCount = Math.min(5, workoutLogs.size());
        for (int i = 0; i < showCount; i++) {
            JSONObject log = workoutLogs.get(i);
            String name = log.optString("exerciseName", "未知动作");
            int sets = log.optInt("sets", 0);
            int reps = log.optInt("repsPerSet", 0);
            double weight = log.optDouble("weightKg", -1);
            int duration = log.optInt("durationMinutes", 0);
            String feeling = log.optString("feeling", "");
            String performedAt = log.optString("performedAt", "");
            String date = performedAt.substring(0, Math.min(10, performedAt.length()));

            CardView card = new CardView(this);
            card.setCardElevation(0);
            card.setCardBackgroundColor(Color.parseColor("#F5F7FA"));
            card.setRadius(dp(14));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(8);
            card.setLayoutParams(cardParams);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextSize(15);
            tvName.setTextColor(Color.parseColor("#111827"));
            tvName.setTypeface(null, Typeface.BOLD);
            left.addView(tvName);

            StringBuilder detail = new StringBuilder();
            if (sets > 0) detail.append(sets).append("组");
            if (reps > 0) detail.append(" x ").append(reps).append("次");
            if (weight >= 0) detail.append("  ").append((int) weight).append("kg");
            if (duration > 0) detail.append("  ").append(duration).append("分钟");

            TextView tvDetail = new TextView(this);
            tvDetail.setText(detail.toString().trim());
            tvDetail.setTextSize(12);
            tvDetail.setTextColor(Color.parseColor("#56616D"));
            left.addView(tvDetail);
            row.addView(left);

            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);

            TextView tvFeeling = new TextView(this);
            tvFeeling.setText(getFeelingText(feeling));
            tvFeeling.setTextSize(12);
            tvFeeling.setTextColor(Color.parseColor("#008C7A"));
            tvFeeling.setTypeface(null, Typeface.BOLD);
            right.addView(tvFeeling);

            TextView tvDate = new TextView(this);
            tvDate.setText(date);
            tvDate.setTextSize(11);
            tvDate.setTextColor(Color.parseColor("#8A96A3"));
            right.addView(tvDate);

            row.addView(right);
            card.addView(row);
            recentRecordsContainer.addView(card);
        }
    }

    private void setupChartBaseStyle() {
        weightLineChart.setNoDataText("暂无重量数据，先记录训练吧");
        weightLineChart.setNoDataTextColor(Color.parseColor("#56616D"));
        weightLineChart.getDescription().setEnabled(false);
        weightLineChart.setDrawGridBackground(false);
        weightLineChart.setTouchEnabled(true);
        weightLineChart.setPinchZoom(false);
        weightLineChart.getAxisRight().setEnabled(false);

        XAxis xAxis = weightLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#8A96A3"));

        YAxis leftAxis = weightLineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#33BFEAD9"));
        leftAxis.setTextColor(Color.parseColor("#8A96A3"));
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "kg";
            }
        });
    }

    private boolean matchesExerciseQuery(Equipment equipment, String lowerQuery) {
        return contains(equipment.getName(), lowerQuery)
                || contains(equipment.getDescription(), lowerQuery)
                || containsMuscle(equipment.getTargetMuscles(), lowerQuery)
                || containsMuscle(equipment.getSecondaryMuscles(), lowerQuery);
    }

    private boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    private boolean containsMuscle(List<String> muscles, String lowerQuery) {
        if (muscles == null) return false;
        for (String muscle : muscles) {
            if (contains(muscle, lowerQuery)
                    || contains(EquipmentRepository.getMuscleNameCn(muscle), lowerQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesMuscleFilter(Equipment equipment, String muscle) {
        if ("chest".equals(muscle) && "shoulder_press".equals(equipment.getId())) {
            return false;
        }
        if ("arms".equals(muscle)) {
            if (isExcludedFromArms(equipment)) {
                return false;
            }
            return hasAnyMuscle(equipment, "biceps", "triceps", "forearms");
        }
        if ("legs".equals(muscle)) {
            return hasAnyMuscle(equipment, "quadriceps", "hamstrings", "glutes", "calves");
        }
        return hasAnyMuscle(equipment, muscle);
    }

    private boolean isExcludedFromArms(Equipment equipment) {
        String id = equipment.getId();
        return "shoulder_press".equals(id)
                || "lat_pulldown".equals(id)
                || "seated_row".equals(id)
                || "assisted_pull_up".equals(id)
                || "bench_press".equals(id);
    }

    private boolean hasAnyMuscle(Equipment equipment, String... muscles) {
        for (String muscle : muscles) {
            if (hasMuscle(equipment.getTargetMuscles(), muscle)
                    || hasMuscle(equipment.getSecondaryMuscles(), muscle)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMuscle(List<String> muscles, String target) {
        return muscles != null && muscles.contains(target);
    }

    private boolean containsEquipmentName(List<Equipment> list, String name) {
        for (Equipment equipment : list) {
            if (equipment.getName().equals(name)) return true;
        }
        return false;
    }

    private String getFallbackExerciseName() {
        return allEquipment.isEmpty() ? "" : allEquipment.get(0).getName();
    }

    private String getFeelingText(String feeling) {
        switch (feeling) {
            case "easy":
                return "轻松";
            case "moderate":
                return "适中";
            case "hard":
                return "困难";
            default:
                return feeling;
        }
    }

    private int getHeatBackground(int count) {
        if (count <= 0) return R.drawable.bg_heat_0;
        if (count == 1) return R.drawable.bg_heat_1;
        if (count == 2) return R.drawable.bg_heat_2;
        if (count == 3) return R.drawable.bg_heat_3;
        if (count == 4) return R.drawable.bg_heat_4;
        return R.drawable.bg_heat_5;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
