package com.example.strong_body;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

public class ExerciseCardAdapter extends RecyclerView.Adapter<ExerciseCardAdapter.ViewHolder> {

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise, int position);
    }

    private final List<Exercise> exercises;
    private OnExerciseClickListener listener;
    private int selectedPosition = -1;

    public ExerciseCardAdapter(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public void setOnExerciseClickListener(OnExerciseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);

        holder.tvName.setText(exercise.getName());
        holder.tvDesc.setText(exercise.getDescription());

        // 难度 Chip
        String difficulty = exercise.getDifficulty();
        holder.chipDifficulty.setText(difficulty);
        holder.chipDifficulty.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getDifficultyColor(difficulty)));
        holder.chipDifficulty.setTextColor(Color.WHITE);

        // 适合人群 Chip（取第一个）
        List<String> suitableFor = exercise.getSuitableFor();
        if (suitableFor != null && !suitableFor.isEmpty()) {
            String tag = suitableFor.get(0);
            holder.chipSuitableFor.setText(tag);
            holder.chipSuitableFor.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getSuitableForColor(tag)));
            holder.chipSuitableFor.setTextColor(getSuitableForTextColor(tag));
            holder.chipSuitableFor.setVisibility(View.VISIBLE);
        } else {
            holder.chipSuitableFor.setVisibility(View.GONE);
        }

        // 选中高亮
        boolean selected = position == selectedPosition;
        holder.cardView.setCardBackgroundColor(selected ? 0xFFE3F2FD : 0xFFFFFFFF);
        holder.cardView.setCardElevation(selected ? 8f : 4f);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (prev >= 0) notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onExerciseClick(exercise, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
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

    private int getSuitableForColor(String tag) {
        if (tag == null) return 0xFFE0E0E0;
        switch (tag) {
            case "新手": return 0xFFE3F2FD;
            case "进阶": return 0xFFFFF3E0;
            case "康复": return 0xFFFFEBEE;
            default: return 0xFFE0E0E0;
        }
    }

    private int getSuitableForTextColor(String tag) {
        if (tag == null) return 0xFF333333;
        switch (tag) {
            case "新手": return 0xFF1565C0;
            case "进阶": return 0xFFE65100;
            case "康复": return 0xFFC62828;
            default: return 0xFF333333;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvName;
        TextView tvDesc;
        Chip chipDifficulty;
        Chip chipSuitableFor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvName = itemView.findViewById(R.id.tvExerciseName);
            tvDesc = itemView.findViewById(R.id.tvExerciseDesc);
            chipDifficulty = itemView.findViewById(R.id.chipDifficulty);
            chipSuitableFor = itemView.findViewById(R.id.chipSuitableFor);
        }
    }
}
