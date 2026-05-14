package com.example.strong_body;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EquipmentListAdapter extends RecyclerView.Adapter<EquipmentListAdapter.ViewHolder> {

    public interface OnEquipmentClickListener {
        void onEquipmentClick(Equipment equipment);
    }

    private final List<Equipment> equipmentList;
    private OnEquipmentClickListener listener;

    public EquipmentListAdapter(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    public void setOnEquipmentClickListener(OnEquipmentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);

        holder.tvName.setText(equipment.getName());
        holder.tvDesc.setText(equipment.getDescription());

        // 图标文字（取名称前两个字）
        String name = equipment.getName();
        holder.tvIcon.setText(name.length() >= 2 ? name.substring(0, 2) : name);

        // 难度标签
        String difficulty = equipment.getDifficulty();
        holder.tvDifficulty.setText(difficulty);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(20);
        bg.setColor(getDifficultyColor(difficulty));
        holder.tvDifficulty.setBackground(bg);

        // 肌肉群
        List<String> muscles = equipment.getTargetMuscles();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < muscles.size(); i++) {
            sb.append(EquipmentRepository.getMuscleNameCn(muscles.get(i)));
            if (i < muscles.size() - 1) sb.append("、");
        }
        holder.tvMuscles.setText(sb.toString());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEquipmentClick(equipment);
        });
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    private int getDifficultyColor(String difficulty) {
        if (difficulty == null) return 0xFF9E9E9E;
        switch (difficulty) {
            case "初级": return 0xFF4CAF50;
            case "中级": return 0xFFFF9800;
            case "高级": return 0xFFF44336;
            default: return 0xFF9E9E9E;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvDesc, tvDifficulty, tvMuscles;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvMuscles = itemView.findViewById(R.id.tvMuscles);
        }
    }
}
