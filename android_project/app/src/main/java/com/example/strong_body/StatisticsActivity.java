package com.example.strong_body;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class StatisticsActivity extends AppCompatActivity {

    private TextView tvWeeklyCount, tvTotalDuration, tvTopBodyPart, tvStreakDays, tvMonthLabel;
    private LineChart weightLineChart;
    private GridLayout calendarGrid;
    private LinearLayout recentRecordsContainer;
    private ChipGroup chipGroupEquipment;
    private Button btnCheckIn;

    private JSONObject statsData;
    private Map<String, Object> weightProgression;
    private List<JSONObject> workoutLogs = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String[] KNOWN_EXERCISES = {"坐姿推胸", "坐姿腿弯举", "坐姿腿屈伸"};

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
        btnCheckIn = findViewById(R.id.btnCheckIn);

        setupChartBaseStyle();
        showLoadingState();

        loadData();

        chipGroupEquipment.setOnCheckedChangeListener((group, checkedId) -> {
            String exercise = getSelectedExercise(checkedId);
            updateWeightChart(exercise);
        });

        btnCheckIn.setOnClickListener(v -> {
            String exercise = getSelectedExercise(chipGroupEquipment.getCheckedChipId());
            WorkoutLogBottomSheet.newInstance(exercise, this::loadData)
                    .show(getSupportFragmentManager(), "WorkoutLog");
        });
    }

    private String getSelectedExercise(int checkedId) {
        if (checkedId == R.id.chipChestPress) return KNOWN_EXERCISES[0];
        else if (checkedId == R.id.chipLegCurl) return KNOWN_EXERCISES[1];
        else if (checkedId == R.id.chipLegExtension) return KNOWN_EXERCISES[2];
        return KNOWN_EXERCISES[0];
    }

    private void showLoadingState() {
        tvWeeklyCount.setText("...");
        tvTotalDuration.setText("...");
        tvTopBodyPart.setText("...");
        tvStreakDays.setText("...");
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                SavedAccount account = AuthAccountStorage.getAutoLoginAccount(this);
                String token = account != null ? account.token : "";

                // Fetch stats
                GymEyeApiClient.HttpResult statsResult = GymEyeApiClient.get("/api/workouts/stats/summary", token);
                if (statsResult.code == 200) {
                    statsData = statsResult.jsonOrEmpty().optJSONObject("data");
                }

                // Fetch recent logs
                GymEyeApiClient.HttpResult listResult = GymEyeApiClient.get("/api/workouts?limit=10", token);
                if (listResult.code == 200) {
                    JSONArray rows = listResult.jsonOrEmpty().optJSONArray("rows");
                    workoutLogs.clear();
                    if (rows != null) {
                        for (int i = 0; i < rows.length(); i++) {
                            workoutLogs.add(rows.getJSONObject(i));
                        }
                    }
                }

                // Build weight progression from stats data or logs
                buildWeightProgressionFromStats();

                mainHandler.post(() -> {
                    updateSummaryCards();
                    updateWeightChart(getSelectedExercise(chipGroupEquipment.getCheckedChipId()));
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
        weightProgression = new java.util.HashMap<>();
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
        if (weightProgression == null) {
            weightLineChart.clear();
            setupChartBaseStyle();
            return;
        }

        @SuppressWarnings("unchecked")
        List<Entry> entries = (List<Entry>) weightProgression.get(exerciseName);
        if (entries == null || entries.isEmpty()) {
            weightLineChart.clear();
            setupChartBaseStyle();
            weightLineChart.setNoDataText(exerciseName + " 暂无重量数据");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, exerciseName + " 重量 (kg)");
        dataSet.setColor(Color.parseColor("#1D1D1F"));
        dataSet.setCircleColor(Color.parseColor("#1D1D1F"));
        dataSet.setLineWidth(3f);
        dataSet.setValueTextSize(12f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E5E5EA"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        weightLineChart.setData(new LineData(dataSet));
        weightLineChart.getAxisLeft().setGranularity(1f);
        weightLineChart.getAxisLeft().setAxisMinimum(0f);
        weightLineChart.animateX(1000);
    }

    private void updateCalendar() {
        calendarGrid.removeAllViews();
        if (statsData == null) return;

        JSONArray calendarDays = statsData.optJSONArray("calendarDays");
        if (calendarDays == null || calendarDays.length() == 0) return;

        // Add day-of-week headers
        String[] weekHeaders = {"一", "二", "三", "四", "五", "六", "日"};
        for (String header : weekHeaders) {
            TextView tv = new TextView(this);
            tv.setText(header);
            tv.setTextSize(10);
            tv.setTextColor(Color.parseColor("#86868B"));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(2, 4, 2, 4);
            calendarGrid.addView(tv);
        }

        // Determine first day of week offset (1=Mon .. 7=Sun → 0=Mon .. 6=Sun)
        int firstDayOffset = 0;
        if (calendarDays.length() > 0) {
            String firstDate = calendarDays.optJSONObject(0).optString("date", "");
            try {
                String[] parts = firstDate.split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int d = Integer.parseInt(parts[2]);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(y, m - 1, d);
                int dow = cal.get(java.util.Calendar.DAY_OF_WEEK); // 1=Sun .. 7=Sat
                firstDayOffset = dow == 1 ? 6 : dow - 2; // → 0=Mon .. 6=Sun
            } catch (Exception ignored) {}
        }

        // Add empty cells for offset
        for (int i = 0; i < firstDayOffset; i++) {
            View v = new View(this);
            v.setLayoutParams(new ViewGroup.LayoutParams(0, 24));
            calendarGrid.addView(v);
        }

        // Determine month boundaries for labels
        String currentMonth = "";

        // Add day cells
        for (int i = 0; i < calendarDays.length(); i++) {
            JSONObject day = calendarDays.optJSONObject(i);
            String date = day.optString("date", "");
            int count = day.optInt("count", 0);

            // Track month changes
            if (date.length() >= 7) {
                String month = date.substring(0, 7);
                if (!month.equals(currentMonth)) {
                    currentMonth = month;
                }
            }

            TextView tv = new TextView(this);
            tv.setText(String.valueOf(Integer.parseInt(date.substring(8, 10))));
            tv.setTextSize(9);
            tv.setTextColor(count > 0 ? Color.WHITE : Color.parseColor("#86868B"));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(2, 3, 2, 3);

            int bgColor;
            if (count == 0) bgColor = Color.parseColor("#F0F0F0");
            else if (count == 1) bgColor = Color.parseColor("#A1D4A8");
            else if (count == 2) bgColor = Color.parseColor("#5ABF60");
            else if (count == 3) bgColor = Color.parseColor("#388E3C");
            else bgColor = Color.parseColor("#1B5E20");

            tv.setBackgroundColor(bgColor);

            // Add tooltip with full date
            tv.setOnClickListener(v -> Toast.makeText(this, date + ": " + count + "次训练", Toast.LENGTH_SHORT).show());

            calendarGrid.addView(tv);
        }

        // Show month label
        if (calendarDays.length() > 0) {
            String firstDate = calendarDays.optJSONObject(0).optString("date", "");
            String lastDate = calendarDays.optJSONObject(calendarDays.length() - 1).optString("date", "");
            tvMonthLabel.setText(firstDate + " ~ " + lastDate);
        }
    }

    private void updateRecentRecords() {
        recentRecordsContainer.removeAllViews();
        if (workoutLogs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无训练记录，快去打卡吧!");
            empty.setTextSize(14);
            empty.setTextColor(Color.parseColor("#86868B"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 24, 0, 24);
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
            String date = log.optString("performedAt", "").substring(0, Math.min(10, log.optString("performedAt", "").length()));

            CardView card = new CardView(this);
            card.setCardElevation(0);
            card.setCardBackgroundColor(Color.parseColor("#F5F7FA"));
            card.setRadius(12);
            card.setUseCompatPadding(true);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = 8;
            card.setLayoutParams(cardParams);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(12, 10, 12, 10);

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextSize(15);
            tvName.setTextColor(Color.parseColor("#1D1D1F"));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            left.addView(tvName);

            StringBuilder detail = new StringBuilder();
            if (sets > 0) detail.append(sets).append("组");
            if (reps > 0) detail.append(" × ").append(reps).append("次");
            if (weight >= 0) detail.append("  ").append((int) weight).append("kg");
            if (duration > 0) detail.append("  ").append(duration).append("分钟");

            TextView tvDetail = new TextView(this);
            tvDetail.setText(detail.toString().trim());
            tvDetail.setTextSize(12);
            tvDetail.setTextColor(Color.parseColor("#86868B"));
            left.addView(tvDetail);

            row.addView(left);

            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);

            TextView tvFeeling = new TextView(this);
            String feelingText;
            switch (feeling) {
                case "easy": feelingText = "轻松"; break;
                case "moderate": feelingText = "适中"; break;
                case "hard": feelingText = "困难"; break;
                default: feelingText = feeling;
            }
            tvFeeling.setText(feelingText);
            tvFeeling.setTextSize(12);
            tvFeeling.setTextColor(Color.parseColor("#1D1D1F"));
            right.addView(tvFeeling);

            TextView tvDate = new TextView(this);
            tvDate.setText(date);
            tvDate.setTextSize(11);
            tvDate.setTextColor(Color.parseColor("#86868B"));
            right.addView(tvDate);

            row.addView(right);
            card.addView(row);
            recentRecordsContainer.addView(card);
        }
    }

    private void setupChartBaseStyle() {
        weightLineChart.setNoDataText("暂无重量数据，先记录训练吧");
        weightLineChart.getDescription().setEnabled(false);
        XAxis xAxis = weightLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        weightLineChart.getAxisRight().setEnabled(false);
        weightLineChart.getAxisLeft().setDrawGridLines(false);
        weightLineChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "kg";
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}