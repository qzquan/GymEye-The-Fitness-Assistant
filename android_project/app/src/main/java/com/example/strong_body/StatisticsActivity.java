package com.example.strong_body;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tvTotalSets, tvStreakDays;
    private String selectedEquipment;
    private Map<String, ArrayList<Entry>> allEquipmentEntries = new HashMap<>();
    private Map<String, Integer> currentDayIndexes = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        lineChart = findViewById(R.id.setsLineChart);
        tvTotalSets = findViewById(R.id.tvTotalSets);
        tvStreakDays = findViewById(R.id.tvStreakDays);
        ChipGroup chipGroup = findViewById(R.id.chipGroupEquipment);
        Button btnCheckIn = findViewById(R.id.btnCheckIn);
        Button btnStartNewWeek = findViewById(R.id.btnStartNewWeek);

        // 初始化UI显示
        tvTotalSets.setText("0 组");
        tvStreakDays.setText("0 天");

        String[] equipments = new String[]{"坐姿推胸 😌", "坐姿腿弯举 😐", "坐姿腿屈伸 🥵"};
        for (String eq : equipments) {
            allEquipmentEntries.put(eq, new ArrayList<>());
            currentDayIndexes.put(eq, 0);
        }

        selectedEquipment = equipments[0];
        setupChartBaseStyle();

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipChestPress) selectedEquipment = equipments[0];
            else if (checkedId == R.id.chipLegCurl) selectedEquipment = equipments[1];
            else if (checkedId == R.id.chipLegExtension) selectedEquipment = equipments[2];
            updateChartData(false);
        });

        btnCheckIn.setOnClickListener(v -> {
            Integer dayIndex = currentDayIndexes.get(selectedEquipment);
            if (dayIndex > 6) {
                Toast.makeText(this, "本周打卡已满！", Toast.LENGTH_SHORT).show();
                return;
            }
            RecordBottomSheetDialog.newInstance(selectedEquipment, (equipmentName, sets) -> {
                ArrayList<Entry> entries = allEquipmentEntries.get(equipmentName);
                if (entries != null) {
                    int currentIndex = currentDayIndexes.get(equipmentName);
                    entries.add(new Entry(currentIndex, sets));
                    currentDayIndexes.put(equipmentName, currentIndex + 1);
                }
                if (equipmentName.equals(selectedEquipment)) updateChartData(true);
                updateTotalSetsSummary();
                updateStreakSummary();
            }).show(getSupportFragmentManager(), "Record");
        });

        btnStartNewWeek.setOnClickListener(v -> {
            for (String eq : equipments) {
                allEquipmentEntries.get(eq).clear();
                currentDayIndexes.put(eq, 0);
            }
            tvTotalSets.setText("0 组");
            tvStreakDays.setText("0 天");
            lineChart.clear();
            setupChartBaseStyle();
            Toast.makeText(this, "数据已重置！", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateStreakSummary() {
        int maxDay = 0;
        for (Integer day : currentDayIndexes.values()) if (day > maxDay) maxDay = day;
        tvStreakDays.setText(maxDay + " 天");
    }

    private void updateTotalSetsSummary() {
        float total = 0;
        for (ArrayList<Entry> e : allEquipmentEntries.values()) for (Entry entry : e) total += entry.getY();
        tvTotalSets.setText((int)total + " 组");
    }

    private void updateChartData(boolean showAnimation) {
        ArrayList<Entry> entries = allEquipmentEntries.get(selectedEquipment);
        if (entries == null || entries.isEmpty()) {
            lineChart.clear();
            setupChartBaseStyle();
            return;
        }
        LineDataSet dataSet = new LineDataSet(entries, "每天做了几组 (Sets)");
        dataSet.setColor(Color.parseColor("#1D1D1F"));
        dataSet.setCircleColor(Color.parseColor("#1D1D1F"));
        dataSet.setLineWidth(3f);
        dataSet.setValueTextSize(12f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E5E5EA"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) { return String.valueOf((int) value); }
        });

        lineChart.setData(new LineData(dataSet));
        YAxis left = lineChart.getAxisLeft();
        left.setGranularity(1f);
        left.setAxisMinimum(0f);
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) { return String.valueOf((int) value); }
        });
        if (showAnimation) lineChart.animateX(1000); else lineChart.invalidate();
    }

    private void setupChartBaseStyle() {
        lineChart.setNoDataText("暂无打卡数据");
        lineChart.getDescription().setEnabled(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(6f);
        xAxis.setLabelCount(7, true);
        final String[] days = new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < days.length) ? days[i] : "";
            }
        });
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
    }
}