package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class EquipmentListActivity extends AppCompatActivity {

    private final List<Equipment> allEquipment = new ArrayList<>();
    private EquipmentListAdapter adapter;
    private EditText etSearchEquipment;
    private ChipGroup chipGroupDifficulty;
    private ChipGroup chipGroupMuscle;
    private TextView tvEmptyEquipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_list);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etSearchEquipment = findViewById(R.id.etSearchEquipment);
        chipGroupDifficulty = findViewById(R.id.chipGroupDifficulty);
        chipGroupMuscle = findViewById(R.id.chipGroupMuscle);
        tvEmptyEquipment = findViewById(R.id.tvEmptyEquipment);

        RecyclerView rv = findViewById(R.id.rvEquipmentList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        allEquipment.addAll(EquipmentRepository.getAllEquipment());
        adapter = new EquipmentListAdapter(allEquipment);
        adapter.setOnEquipmentClickListener(equipment -> {
            Intent intent = new Intent(this, EquipmentDetailActivity.class);
            intent.putExtra(EquipmentDetailActivity.EXTRA_EQUIPMENT_NAME, equipment.getName());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        etSearchEquipment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroupDifficulty.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
        chipGroupMuscle.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
        applyFilters();
    }

    private void applyFilters() {
        String query = etSearchEquipment.getText() == null
                ? ""
                : etSearchEquipment.getText().toString().trim();
        String difficulty = getSelectedDifficulty();
        String muscle = getSelectedMuscle();
        List<Equipment> filtered = new ArrayList<>();

        for (Equipment equipment : allEquipment) {
            if (!TextUtils.isEmpty(difficulty) && !difficulty.equals(equipment.getDifficulty())) {
                continue;
            }
            if (!TextUtils.isEmpty(muscle) && !matchesMuscleFilter(equipment, muscle)) {
                continue;
            }
            if (!TextUtils.isEmpty(query) && !matchesQuery(equipment, query)) {
                continue;
            }
            filtered.add(equipment);
        }

        adapter.updateData(filtered);
        tvEmptyEquipment.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getSelectedDifficulty() {
        int checkedId = chipGroupDifficulty.getCheckedChipId();
        if (checkedId == R.id.chipBeginner) return "初级";
        if (checkedId == R.id.chipIntermediate) return "中级";
        if (checkedId == R.id.chipAdvanced) return "高级";
        return "";
    }

    private String getSelectedMuscle() {
        int checkedId = chipGroupMuscle.getCheckedChipId();
        if (checkedId == R.id.chipMuscleBack) return "back";
        if (checkedId == R.id.chipMuscleChest) return "chest";
        if (checkedId == R.id.chipMuscleShoulders) return "shoulders";
        if (checkedId == R.id.chipMuscleArms) return "arms";
        if (checkedId == R.id.chipMuscleLegs) return "legs";
        return "";
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

    private boolean matchesQuery(Equipment equipment, String query) {
        String lowerQuery = query.toLowerCase();
        return contains(equipment.getName(), lowerQuery)
                || contains(equipment.getDescription(), lowerQuery)
                || contains(equipment.getDifficulty(), lowerQuery)
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
}
