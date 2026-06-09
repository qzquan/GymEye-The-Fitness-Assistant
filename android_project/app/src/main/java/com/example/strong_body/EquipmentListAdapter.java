package com.example.strong_body;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EquipmentListAdapter extends RecyclerView.Adapter<EquipmentListAdapter.ViewHolder> {

    public interface OnEquipmentClickListener {
        void onEquipmentClick(Equipment equipment);
    }

    private final List<Equipment> equipmentList = new ArrayList<>();
    private OnEquipmentClickListener listener;

    public EquipmentListAdapter(List<Equipment> equipmentList) {
        updateData(equipmentList);
    }

    public void updateData(List<Equipment> nextList) {
        equipmentList.clear();
        if (nextList != null) {
            equipmentList.addAll(nextList);
        }
        notifyDataSetChanged();
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
        holder.tvDifficulty.setText(equipment.getDifficulty());
        holder.tvDifficulty.setBackground(createDifficultyBackground(equipment.getDifficulty()));

        List<String> muscles = equipment.getTargetMuscles();
        StringBuilder sb = new StringBuilder("目标肌群: ");
        for (int i = 0; i < muscles.size(); i++) {
            sb.append(EquipmentRepository.getMuscleNameCn(muscles.get(i)));
            if (i < muscles.size() - 1) sb.append("、");
        }
        holder.tvMuscles.setText(sb.toString());

        int coverResId = EquipmentImageResolver.getCoverResId(holder.itemView.getContext(), equipment);
        if (coverResId != 0) {
            holder.ivCover.setImageResource(coverResId);
            holder.ivCover.setVisibility(View.VISIBLE);
            holder.tvCoverPlaceholder.setVisibility(View.GONE);
        } else {
            holder.ivCover.setImageDrawable(null);
            holder.ivCover.setVisibility(View.GONE);
            holder.tvCoverPlaceholder.setVisibility(View.VISIBLE);
            holder.tvCoverPlaceholder.setText(equipment.getName() + "\n封面待补充");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEquipmentClick(equipment);
        });
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    private GradientDrawable createDifficultyBackground(String difficulty) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(999);
        bg.setColor(getDifficultyColor(difficulty));
        return bg;
    }

    private int getDifficultyColor(String difficulty) {
        if (difficulty == null) return 0xFF6B7280;
        switch (difficulty) {
            case "初级": return 0xFF16A34A;
            case "中级": return 0xFFF97316;
            case "高级": return 0xFFDC2626;
            default: return 0xFF6B7280;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvCoverPlaceholder;
        TextView tvName;
        TextView tvDesc;
        TextView tvDifficulty;
        TextView tvMuscles;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvCoverPlaceholder = itemView.findViewById(R.id.tvCoverPlaceholder);
            tvName = itemView.findViewById(R.id.tvName);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvMuscles = itemView.findViewById(R.id.tvMuscles);
        }
    }
}
