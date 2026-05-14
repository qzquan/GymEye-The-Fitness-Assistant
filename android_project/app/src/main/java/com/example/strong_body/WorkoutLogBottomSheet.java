package com.example.strong_body;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class WorkoutLogBottomSheet extends BottomSheetDialogFragment {

    public interface OnSubmittedListener {
        void onWorkoutSubmitted();
    }

    private OnSubmittedListener listener;
    private String defaultExerciseName;

    public static WorkoutLogBottomSheet newInstance(String defaultExerciseName, OnSubmittedListener listener) {
        WorkoutLogBottomSheet f = new WorkoutLogBottomSheet();
        f.defaultExerciseName = defaultExerciseName != null ? defaultExerciseName : "";
        f.listener = listener;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_bottom_sheet_workout, container, false);
        MaterialAutoCompleteTextView acEx = v.findViewById(R.id.acSheetExercise);
        MaterialAutoCompleteTextView acPart = v.findViewById(R.id.acSheetBodyPart);
        TextInputEditText etSets = v.findViewById(R.id.etSheetSets);
        TextInputEditText etReps = v.findViewById(R.id.etSheetReps);
        TextInputEditText etWeight = v.findViewById(R.id.etSheetWeight);
        TextInputEditText etDur = v.findViewById(R.id.etSheetDuration);
        ChipGroup chipFeeling = v.findViewById(R.id.rgSheetFeeling);
        MaterialButton btnSubmit = v.findViewById(R.id.btnSheetSubmit);

        bindSuggestionDropdowns(v, acEx, acPart);
        bindQuickSelectChips(v, etSets, etReps, etWeight, etDur);

        if (!TextUtils.isEmpty(defaultExerciseName)) {
            acEx.setText(defaultExerciseName);
        }

        btnSubmit.setOnClickListener(x -> submit(btnSubmit, acEx, acPart, etSets, etReps, etWeight, etDur, chipFeeling));
        return v;
    }

    private void bindSuggestionDropdowns(View root, MaterialAutoCompleteTextView acEx, MaterialAutoCompleteTextView acPart) {
        Context ctx = root.getContext();
        int drop = android.R.layout.simple_dropdown_item_1line;
        acEx.setAdapter(new ArrayAdapter<>(
                ctx,
                drop,
                ctx.getResources().getStringArray(R.array.workout_sheet_exercise_suggestions)));
        acPart.setAdapter(new ArrayAdapter<>(
                ctx,
                drop,
                ctx.getResources().getStringArray(R.array.workout_sheet_body_part_suggestions)));
        acEx.setThreshold(1);
        acPart.setThreshold(1);
    }

    private void bindQuickSelectChips(View root, TextInputEditText etSets, TextInputEditText etReps,
                                       TextInputEditText etWeight, TextInputEditText etDur) {
        // Sets quick-select: 3, 4, 5, 6
        bindChipToField(root, R.id.chipSets3, etSets, "3");
        bindChipToField(root, R.id.chipSets4, etSets, "4");
        bindChipToField(root, R.id.chipSets5, etSets, "5");
        bindChipToField(root, R.id.chipSets6, etSets, "6");

        // Reps quick-select: 8, 10, 12, 15
        bindChipToField(root, R.id.chipReps8, etReps, "8");
        bindChipToField(root, R.id.chipReps10, etReps, "10");
        bindChipToField(root, R.id.chipReps12, etReps, "12");
        bindChipToField(root, R.id.chipReps15, etReps, "15");

        // Weight quick-select: 10, 15, 20, 25, 30, 40, 50
        bindChipToField(root, R.id.chipWeight10, etWeight, "10");
        bindChipToField(root, R.id.chipWeight15, etWeight, "15");
        bindChipToField(root, R.id.chipWeight20, etWeight, "20");
        bindChipToField(root, R.id.chipWeight25, etWeight, "25");
        bindChipToField(root, R.id.chipWeight30, etWeight, "30");
        bindChipToField(root, R.id.chipWeight40, etWeight, "40");
        bindChipToField(root, R.id.chipWeight50, etWeight, "50");

        // Duration quick-select: 15, 20, 30, 45, 60
        bindChipToField(root, R.id.chipDur15, etDur, "15");
        bindChipToField(root, R.id.chipDur20, etDur, "20");
        bindChipToField(root, R.id.chipDur30, etDur, "30");
        bindChipToField(root, R.id.chipDur45, etDur, "45");
        bindChipToField(root, R.id.chipDur60, etDur, "60");
    }

    private void bindChipToField(View root, int chipId, EditText target, String value) {
        Chip chip = root.findViewById(chipId);
        if (chip != null) {
            chip.setOnClickListener(v -> {
                target.setText(value);
                target.setSelection(value.length());
                // Deselect the chip after a short delay to reset its visual state
                target.postDelayed(() -> chip.setChecked(false), 150);
            });
        }
    }

    private void submit(
            MaterialButton submitBtn,
            MaterialAutoCompleteTextView acEx,
            MaterialAutoCompleteTextView acPart,
            TextInputEditText etSets,
            TextInputEditText etReps,
            TextInputEditText etWeight,
            TextInputEditText etDur,
            ChipGroup chipFeeling
    ) {
        if (getContext() == null) return;
        String token = AuthAccountStorage.getSessionToken(getContext());
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String exercise = text(acEx);
        if (TextUtils.isEmpty(exercise)) {
            Toast.makeText(getContext(), "请填写动作名称", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer sets = parseInt(text(etSets));
        Integer reps = parseInt(text(etReps));
        Double weight = parseDouble(text(etWeight));
        Integer duration = parseInt(text(etDur));

        boolean hasSr = sets != null && reps != null;
        boolean hasW = weight != null;
        boolean hasD = duration != null;
        if (!hasSr && !hasW && !hasD) {
            Toast.makeText(getContext(), "请填写组数+次数、重量或时长至少一项", Toast.LENGTH_LONG).show();
            return;
        }
        if (sets != null && reps == null) {
            Toast.makeText(getContext(), "请同时填写次数", Toast.LENGTH_SHORT).show();
            return;
        }
        if (reps != null && sets == null) {
            Toast.makeText(getContext(), "请同时填写组数", Toast.LENGTH_SHORT).show();
            return;
        }

        String feeling = "moderate";
        int checkedFeelingId = chipFeeling.getCheckedChipId();
        if (checkedFeelingId == R.id.rbSheetEasy) feeling = "easy";
        else if (checkedFeelingId == R.id.rbSheetHard) feeling = "hard";

        if (checkedFeelingId == View.NO_ID) {
            Toast.makeText(getContext(), "请选择训练感受", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("exerciseName", exercise);
            body.put("feeling", feeling);
            if (sets != null) body.put("sets", sets);
            if (reps != null) body.put("repsPerSet", reps);
            if (weight != null) body.put("weightKg", weight);
            if (duration != null) body.put("durationMinutes", duration);
            String bp = text(acPart);
            if (!TextUtils.isEmpty(bp)) {
                body.put("bodyPart", bp);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "数据错误", Toast.LENGTH_SHORT).show();
            return;
        }

        submitBtn.setEnabled(false);
        new Thread(() -> {
            try {
                GymEyeApiClient.HttpResult res = GymEyeApiClient.postJson("/api/workouts", token, body);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    submitBtn.setEnabled(true);
                    if (res.code == 201) {
                        Toast.makeText(getContext(), "打卡成功", Toast.LENGTH_SHORT).show();
                        dismiss();
                        if (listener != null) {
                            listener.onWorkoutSubmitted();
                        }
                    } else {
                        Toast.makeText(getContext(), res.jsonOrEmpty().optString("message", "失败 " + res.code), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    submitBtn.setEnabled(true);
                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private static String text(EditText e) {
        if (e == null || e.getText() == null) return "";
        return e.getText().toString().trim();
    }

    private static Integer parseInt(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            int v = Integer.parseInt(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            double v = Double.parseDouble(s);
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}