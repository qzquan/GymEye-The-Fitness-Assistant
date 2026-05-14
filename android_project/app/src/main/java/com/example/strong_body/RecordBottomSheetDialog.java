package com.example.strong_body;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

public class RecordBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnRecordSavedListener {
        void onRecordSaved(String equipmentName, float sets);
    }

    private OnRecordSavedListener listener;
    private String equipmentName;

    public static RecordBottomSheetDialog newInstance(String equipmentName, OnRecordSavedListener listener) {
        RecordBottomSheetDialog fragment = new RecordBottomSheetDialog();
        fragment.equipmentName = equipmentName;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_record, container, false);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("记录: " + equipmentName);

        TextInputEditText etSets = view.findViewById(R.id.etWeight);
        Button btnSaveRecord = view.findViewById(R.id.btnSaveRecord);

        btnSaveRecord.setOnClickListener(v -> {
            String setsStr = etSets.getText().toString();
            if (setsStr.isEmpty()) {
                Toast.makeText(getContext(), "请输入组数", Toast.LENGTH_SHORT).show();
            } else {
                if (listener != null) {
                    listener.onRecordSaved(equipmentName, Float.parseFloat(setsStr));
                    dismiss();
                }
            }
        });
        return view;
    }
}