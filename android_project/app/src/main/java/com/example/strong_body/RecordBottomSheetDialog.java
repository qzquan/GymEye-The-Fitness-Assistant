package com.example.strong_body;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class RecordBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnRecordSavedListener {
        void onRecordSaved();
    }

    private OnRecordSavedListener listener;
    private String exerciseName;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static RecordBottomSheetDialog newInstance(String exerciseName, OnRecordSavedListener listener) {
        RecordBottomSheetDialog fragment = new RecordBottomSheetDialog();
        fragment.exerciseName = exerciseName;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_record, container, false);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null && exerciseName != null) tvTitle.setText("记录: " + exerciseName);

        TextInputEditText etExerciseName = view.findViewById(R.id.etExerciseName);
        TextInputEditText etSets = view.findViewById(R.id.etSets);
        TextInputEditText etReps = view.findViewById(R.id.etReps);
        TextInputEditText etWeight = view.findViewById(R.id.etWeight);
        TextInputEditText etDuration = view.findViewById(R.id.etDuration);
        TextInputEditText etBodyPart = view.findViewById(R.id.etBodyPart);
        ChipGroup chipGroupFeeling = view.findViewById(R.id.chipGroupFeeling);
        Button btnSaveRecord = view.findViewById(R.id.btnSaveRecord);

        if (etExerciseName != null && exerciseName != null) {
            etExerciseName.setText(exerciseName);
        }

        btnSaveRecord.setOnClickListener(v -> {
            String name = etExerciseName != null ? etExerciseName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "请输入动作名称", Toast.LENGTH_SHORT).show();
                return;
            }

            int checkedFeelingId = chipGroupFeeling.getCheckedChipId();
            final String feeling = getFeelingString(checkedFeelingId);

            if (feeling == null) {
                Toast.makeText(getContext(), "请选择训练感受", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSaveRecord.setEnabled(false);
            btnSaveRecord.setText("保存中...");

            executor.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("exerciseName", name);
                    body.put("feeling", feeling);

                    String setsStr = etSets.getText().toString().trim();
                    if (!setsStr.isEmpty()) body.put("sets", Integer.parseInt(setsStr));

                    String repsStr = etReps.getText().toString().trim();
                    if (!repsStr.isEmpty()) body.put("repsPerSet", Integer.parseInt(repsStr));

                    String weightStr = etWeight.getText().toString().trim();
                    if (!weightStr.isEmpty()) body.put("weightKg", Double.parseDouble(weightStr));

                    String durationStr = etDuration.getText().toString().trim();
                    if (!durationStr.isEmpty()) body.put("durationMinutes", Integer.parseInt(durationStr));

                    String bodyPart = etBodyPart.getText().toString().trim();
                    if (!bodyPart.isEmpty()) body.put("bodyPart", bodyPart);

                    String token = AuthAccountStorage.getSessionToken(getContext());
                    if (TextUtils.isEmpty(token)) {
                        mainHandler.post(() -> {
                            btnSaveRecord.setEnabled(true);
                            btnSaveRecord.setText("淇濆瓨璁粌璁板綍");
                            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    GymEyeApiClient.HttpResult result = GymEyeApiClient.postJson("/api/workouts", token, body);

                    mainHandler.post(() -> {
                        btnSaveRecord.setEnabled(true);
                        btnSaveRecord.setText("保存训练记录");
                        if (result.code == 201) {
                            Toast.makeText(getContext(), "记录保存成功!", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onRecordSaved();
                            dismiss();
                        } else {
                            String msg = result.jsonOrEmpty().optString("error", "保存失败");
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        btnSaveRecord.setEnabled(true);
                        btnSaveRecord.setText("保存训练记录");
                        Toast.makeText(getContext(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        return view;
    }

    private String getFeelingString(int checkedId) {
        if (checkedId == R.id.chipEasy) return "easy";
        if (checkedId == R.id.chipModerate) return "moderate";
        if (checkedId == R.id.chipHard) return "hard";
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
