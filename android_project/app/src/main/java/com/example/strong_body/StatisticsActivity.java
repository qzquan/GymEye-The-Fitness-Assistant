package com.example.strong_body;

import android.content.res.ColorStateList;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsActivity extends AppCompatActivity {

    private static final int HEAT_COLUMNS = 18;

    private TextView tvWeeklyCount, tvTotalDuration, tvTopBodyPart, tvStreak, tvMonthLabel;
    private LineChart lineChart;
    private GridLayout calendarGrid;
    private LinearLayout recentRecordsContainer, heroHeatStrip;
    private ChipGroup chipGroupExercises, chipGroupMuscleFilter;
    private EditText etExerciseSearch;
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
        tvStreak = findViewById(R.id.tvStreak);
        tvMonthLabel = findViewById(R.id.tvMonthLabel);
        lineChart = findViewById(R.id.lineChart);
        calendarGrid = findViewById(R.id.calendarGrid);
        recentRecordsContainer = findViewById(R.id.recentRecordsContainer);
        heroHeatStrip = findViewById(R.id.heroHeatStrip);
        chipGroupExercises = findViewById(R.id.chipGroupExercises);
        chipGroupMuscleFilter = findViewById(R.id.chipGroupMuscleFilter);
        etExerciseSearch = findViewById(R.id.etExerciseSearch);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

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
        tvStreak.setText("...");
        renderHeroHeatStrip(null);
    }

    private void setupExerciseFilters() {
        addMuscleChip("全部", "", true);
        addMuscleChip("胸部", "chest", false);
        addMuscleChip("背部", "back", false);
        addMuscleChip("腿部", "legs", false);
        addMuscleChip("肩部", "shoulders", false);
        addMuscleChip("手臂", "arms", false);

        etExerciseSearch.addTextChangedListener(new TextWatcher() {
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
        String query = etExerciseSearch.getText() == null
                ? ""
                : etExerciseSearch.getText().toString().trim().toLowerCase();
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
        chipGroupExercises.removeAllViews();
        if (visibleEquipment.isEmpty()) {
            Chip empty = createChoiceChip("无匹配器材");
            empty.setEnabled(false);
            chipGroupExercises.addView(empty);
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
            chipGroupExercises.addView(chip);
        }
    }

    private Chip createChoiceChip(String label) {
        Chip chip = new Chip(this);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setTextSize(12);
        chip.setChipMinHeight(dp(36));
        chip.setChipCornerRadius(dp(18));
        chip.setChipStrokeWidth(dp(1));
        chip.setChipBackgroundColor(new ColorStateList(states, new int[]{
                Color.parseColor("#E7FFFB"),
                Color.WHITE
        }));
        chip.setChipStrokeColor(new ColorStateList(states, new int[]{
                Color.parseColor("#10C7B5"),
                Color.parseColor("#E5E7EB")
        }));
        chip.setTextColor(new ColorStateList(states, new int[]{
                Color.parseColor("#087C70"),
                Color.parseColor("#6B7280")
        }));
        chip.setRippleColor(ColorStateList.valueOf(Color.parseColor("#3310C7B5")));
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
                        tvStreak.setText("-");
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
                    tvStreak.setText("离线");
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
            tvStreak.setText("0 天");
            return;
        }

        int weeklyCount = statsData.optInt("weeklyCount", 0);
        int totalDuration = statsData.optInt("totalDurationMinutes", 0);
        int streak = statsData.optInt("streak", 0);

        tvWeeklyCount.setText(weeklyCount + " 次");
        tvStreak.setText(streak + " 天");

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
            lineChart.clear();
            setupChartBaseStyle();
            lineChart.invalidate();
            return;
        }

        @SuppressWarnings("unchecked")
        List<Entry> entries = (List<Entry>) weightProgression.get(exerciseName);
        if (entries == null || entries.isEmpty()) {
            lineChart.clear();
            setupChartBaseStyle();
            lineChart.setNoDataText(exerciseName + " 暂无重量数据");
            lineChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, exerciseName + " 重量 (kg)");
        dataSet.setColor(Color.parseColor("#10C7B5"));
        dataSet.setCircleColor(Color.parseColor("#10C7B5"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#6B7280"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#CFF8F3"));
        dataSet.setFillAlpha(120);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        lineChart.setData(new LineData(dataSet));
        lineChart.getAxisLeft().setGranularity(1f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getLegend().setTextColor(Color.parseColor("#6B7280"));
        lineChart.animateX(800);
    }

    private void updateCalendar() {
        calendarGrid.removeAllViews();
        if (statsData == null) {
            renderHeroHeatStrip(null);
            return;
        }

        JSONArray calendarDays = statsData.optJSONArray("calendarDays");
        renderHeroHeatStrip(calendarDays);
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

    private void renderHeroHeatStrip(JSONArray calendarDays) {
        if (heroHeatStrip == null) return;
        heroHeatStrip.removeAllViews();

        int visibleDays = 7;
        int start = calendarDays == null ? 0 : Math.max(0, calendarDays.length() - visibleDays);
        int maxCount = 1;
        for (int i = 0; i < visibleDays; i++) {
            if (calendarDays != null && start + i < calendarDays.length()) {
                JSONObject day = calendarDays.optJSONObject(start + i);
                if (day != null) {
                    maxCount = Math.max(maxCount, day.optInt("count", 0));
                }
            }
        }

        for (int i = 0; i < visibleDays; i++) {
            int count = 0;
            String date = "";
            if (calendarDays != null && start + i < calendarDays.length()) {
                JSONObject day = calendarDays.optJSONObject(start + i);
                if (day != null) {
                    count = day.optInt("count", 0);
                    date = day.optString("date", "");
                }
            }

            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            columnParams.setMargins(dp(2), 0, dp(2), 0);

            TextView value = new TextView(this);
            value.setText(count > 0 ? count + "次" : "0");
            value.setTextColor(Color.parseColor("#111827"));
            value.setTextSize(12);
            value.setTypeface(null, Typeface.BOLD);
            value.setGravity(Gravity.CENTER);
            column.addView(value);

            View bar = new View(this);
            bar.setBackgroundResource(count > 0
                    ? R.drawable.bg_record_bar_active
                    : R.drawable.bg_record_bar_inactive);
            int barHeight = count > 0 ? dp(28 + Math.round((count * 58f) / maxCount)) : dp(6);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(24), barHeight);
            barParams.setMargins(0, dp(6), 0, dp(8));
            column.addView(bar, barParams);

            TextView label = new TextView(this);
            label.setText(formatChartDayLabel(date, i));
            label.setTextColor(Color.parseColor("#6B7280"));
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            column.addView(label);

            heroHeatStrip.addView(column, columnParams);
        }
    }

    private void updateRecentRecords() {
        recentRecordsContainer.removeAllViews();
        if (workoutLogs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无训练记录，完成一次打卡后这里会显示日志。");
            empty.setTextSize(14);
            empty.setTextColor(Color.parseColor("#6B7280"));
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.bg_record_white_card);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            recentRecordsContainer.addView(empty);
            return;
        }

        int showCount = Math.min(5, workoutLogs.size());
        for (int i = 0; i < showCount; i++) {
            JSONObject log = workoutLogs.get(i);
            String name = log.optString("exerciseName", "未知动作");
            String feeling = log.optString("feeling", "");
            String performedAt = log.optString("performedAt", "");

            MaterialCardView card = new MaterialCardView(this);
            card.setCardBackgroundColor(Color.WHITE);
            card.setRadius(dp(18));
            card.setCardElevation(dp(1));
            card.setStrokeColor(Color.parseColor("#EEF0F3"));
            card.setStrokeWidth(dp(1));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(12);
            card.setLayoutParams(cardParams);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));

            TextView icon = new TextView(this);
            icon.setText(getRecordIconText(name));
            icon.setTextColor(Color.WHITE);
            icon.setTextSize(19);
            icon.setTypeface(null, Typeface.BOLD);
            icon.setGravity(Gravity.CENTER);
            icon.setBackgroundResource(R.drawable.bg_record_list_icon);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(52), dp(52));
            iconParams.setMargins(0, dp(2), dp(14), 0);
            row.addView(icon, iconParams);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextSize(17);
            tvName.setTextColor(Color.parseColor("#111827"));
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setSingleLine(true);
            tvName.setEllipsize(TextUtils.TruncateAt.END);
            titleRow.addView(tvName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvFeeling = new TextView(this);
            tvFeeling.setText(getFeelingText(feeling));
            tvFeeling.setTextSize(12);
            tvFeeling.setTextColor(getFeelingColor(feeling));
            tvFeeling.setTypeface(null, Typeface.BOLD);
            tvFeeling.setGravity(Gravity.CENTER);
            tvFeeling.setSingleLine(true);
            tvFeeling.setBackgroundResource(getFeelingBackground(feeling));
            tvFeeling.setPadding(dp(10), dp(4), dp(10), dp(4));
            LinearLayout.LayoutParams feelingParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            feelingParams.leftMargin = dp(8);
            titleRow.addView(tvFeeling, feelingParams);
            content.addView(titleRow);

            TextView tvSubtitle = new TextView(this);
            tvSubtitle.setText(formatEquipmentSubtitle(name, log.optString("bodyPart", "")));
            tvSubtitle.setTextSize(13);
            tvSubtitle.setTextColor(Color.parseColor("#6B7280"));
            tvSubtitle.setSingleLine(true);
            tvSubtitle.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subtitleParams.topMargin = dp(7);
            content.addView(tvSubtitle, subtitleParams);

            LinearLayout metaRow = new LinearLayout(this);
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            metaParams.topMargin = dp(10);
            addMetaText(metaRow, formatDurationMeta(log));
            addMetaText(metaRow, formatSetsMeta(log));
            addMetaText(metaRow, formatVolumeMeta(log));
            if (metaRow.getChildCount() == 0) {
                addMetaText(metaRow, "已完成");
            }
            content.addView(metaRow, metaParams);
            row.addView(content);

            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);
            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rightParams.leftMargin = dp(10);

            TextView tvDate = new TextView(this);
            tvDate.setText(formatPerformedAt(performedAt));
            tvDate.setTextSize(13);
            tvDate.setTextColor(Color.parseColor("#9CA3AF"));
            tvDate.setGravity(Gravity.END);
            tvDate.setSingleLine(true);
            right.addView(tvDate);

            row.addView(right, rightParams);
            card.addView(row);
            recentRecordsContainer.addView(card);
        }
    }

    private String formatChartDayLabel(String date, int fallbackIndex) {
        if (!TextUtils.isEmpty(date)) {
            try {
                Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(date);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsed);
                String[] labels = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                return labels[calendar.get(Calendar.DAY_OF_WEEK) - 1];
            } catch (Exception ignored) {
            }
        }
        String[] fallback = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return fallback[fallbackIndex % fallback.length];
    }

    private String getRecordIconText(String name) {
        if (TextUtils.isEmpty(name)) return "训";
        return name.substring(0, Math.min(1, name.length()));
    }

    private int getFeelingBackground(String feeling) {
        switch (feeling) {
            case "moderate":
                return R.drawable.bg_record_badge_moderate;
            case "hard":
                return R.drawable.bg_record_badge_hard;
            case "easy":
            default:
                return R.drawable.bg_record_badge_easy;
        }
    }

    private int getFeelingColor(String feeling) {
        switch (feeling) {
            case "moderate":
                return Color.parseColor("#D97706");
            case "hard":
                return Color.parseColor("#2563EB");
            case "easy":
            default:
                return Color.parseColor("#16A34A");
        }
    }

    private void addMetaText(LinearLayout row, String value) {
        if (TextUtils.isEmpty(value)) return;
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.parseColor("#6B7280"));
        text.setTextSize(13);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(12);
        row.addView(text, params);
    }

    private String formatDurationMeta(JSONObject log) {
        int duration = log.optInt("durationMinutes", 0);
        return duration > 0 ? duration + " 分钟" : "";
    }

    private String formatSetsMeta(JSONObject log) {
        int sets = log.optInt("sets", 0);
        int reps = log.optInt("repsPerSet", 0);
        if (sets > 0 && reps > 0) {
            return sets + "组 x " + reps + "次";
        }
        return "";
    }

    private String formatVolumeMeta(JSONObject log) {
        int sets = log.optInt("sets", 0);
        int reps = log.optInt("repsPerSet", 0);
        double weight = log.optDouble("weightKg", -1);
        if (weight < 0) return "";
        if (sets > 0 && reps > 0) {
            double total = sets * reps * weight;
            return trimNumber(total) + " kg";
        }
        return trimNumber(weight) + " kg";
    }

    private String trimNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.01) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.CHINA, "%.1f", value);
    }

    private String formatEquipmentSubtitle(String name, String bodyPart) {
        if (!TextUtils.isEmpty(bodyPart)) {
            String mapped = EquipmentRepository.getMuscleNameCn(bodyPart);
            return mapped + " · 训练记录";
        }
        Equipment equipment = EquipmentRepository.getEquipmentByName(name);
        if (equipment != null && equipment.getTargetMuscles() != null && !equipment.getTargetMuscles().isEmpty()) {
            return EquipmentRepository.getMuscleNameCn(equipment.getTargetMuscles().get(0)) + " · 力量器械";
        }
        return "力量训练";
    }

    private String formatPerformedAt(String performedAt) {
        if (TextUtils.isEmpty(performedAt)) return "-";
        String date = performedAt.substring(0, Math.min(10, performedAt.length()));
        String time = performedAt.length() >= 16 ? performedAt.substring(11, 16) : "";
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());

        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(yesterdayCal.getTime());

        if (date.equals(today)) return TextUtils.isEmpty(time) ? "今天" : "今天 " + time;
        if (date.equals(yesterday)) return TextUtils.isEmpty(time) ? "昨天" : "昨天 " + time;
        if (date.length() == 10) return TextUtils.isEmpty(time) ? date.substring(5) : date.substring(5) + " " + time;
        return performedAt;
    }

    private String formatLogDetail(JSONObject log) {
        int sets = log.optInt("sets", 0);
        int reps = log.optInt("repsPerSet", 0);
        double weight = log.optDouble("weightKg", -1);
        int duration = log.optInt("durationMinutes", 0);

        StringBuilder detail = new StringBuilder();
        if (sets > 0) detail.append(sets).append("组");
        if (reps > 0) detail.append(" x ").append(reps).append("次");
        if (weight >= 0) detail.append("  ").append((int) weight).append("kg");
        if (duration > 0) detail.append("  ").append(duration).append("分钟");
        if (detail.length() == 0) detail.append("已完成");
        return detail.toString().trim();
    }

    private void updateCalendarLegacy() {
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

    private void updateRecentRecordsLegacy() {
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
        lineChart.setNoDataText("暂无重量数据，先记录训练吧");
        lineChart.setNoDataTextColor(Color.parseColor("#6B7280"));
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(Color.parseColor("#6B7280"));

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#6B7280"));

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#33BFEAD9"));
        leftAxis.setTextColor(Color.parseColor("#6B7280"));
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
                return TextUtils.isEmpty(feeling) ? "-" : feeling;
        }
    }

    private String getFeelingTextLegacy(String feeling) {
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
