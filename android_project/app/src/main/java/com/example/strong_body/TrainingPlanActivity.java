package com.example.strong_body;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrainingPlanActivity extends AppCompatActivity {

    private static final String[] SEX_LABELS = {"男", "女", "其他"};
    private static final String[] SEX_VALUES = {"male", "female", "other"};
    private static final String[] GOAL_LABELS = {"增肌", "减脂", "塑形", "康复", "提高力量"};
    private static final String[] GOAL_VALUES = {"muscle_gain", "fat_loss", "body_shaping", "rehab", "strength"};
    private static final String[] LEVEL_LABELS = {"新手", "普通", "进阶"};
    private static final String[] LEVEL_VALUES = {"beginner", "normal", "advanced"};
    private static final String[] WEEKLY_LABELS = {"每周 1 次", "每周 2 次", "每周 3 次", "每周 4 次", "每周 5 次", "每周 6 次", "每周 7 次"};

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<EquipmentOption> equipmentOptions = new ArrayList<>();
    private final List<CheckBox> equipmentChecks = new ArrayList<>();

    private ProgressBar progressPlan;
    private LinearLayout profileForm;
    private LinearLayout planContent;
    private Spinner spSex;
    private Spinner spGoal;
    private Spinner spLevel;
    private Spinner spWeekly;
    private EditText etHeight;
    private EditText etWeight;
    private LinearLayout layoutEquipmentOptions;
    private MaterialButton btnSaveProfile;
    private MaterialButton btnRegenerate;
    private MaterialButton btnEditProfile;
    private TextView tvPlanTitle;
    private TextView tvWeeklySummary;
    private TextView tvRationale;
    private TextView tvPlanEmpty;
    private LinearLayout layoutPlanItems;

    private JSONObject currentProfile;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_plan);

        token = AuthAccountStorage.getSessionToken(this);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "请先登录后生成训练计划", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSpinners();
        buildEquipmentOptions();
        bindEvents();
        loadInitialState();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        progressPlan = findViewById(R.id.progressPlan);
        profileForm = findViewById(R.id.profileForm);
        planContent = findViewById(R.id.planContent);
        spSex = findViewById(R.id.spSex);
        spGoal = findViewById(R.id.spGoal);
        spLevel = findViewById(R.id.spLevel);
        spWeekly = findViewById(R.id.spWeekly);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        layoutEquipmentOptions = findViewById(R.id.layoutEquipmentOptions);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        tvPlanTitle = findViewById(R.id.tvPlanTitle);
        tvWeeklySummary = findViewById(R.id.tvWeeklySummary);
        tvRationale = findViewById(R.id.tvRationale);
        tvPlanEmpty = findViewById(R.id.tvPlanEmpty);
        layoutPlanItems = findViewById(R.id.layoutPlanItems);
    }

    private void setupSpinners() {
        bindSpinner(spSex, SEX_LABELS);
        bindSpinner(spGoal, GOAL_LABELS);
        bindSpinner(spLevel, LEVEL_LABELS);
        bindSpinner(spWeekly, WEEKLY_LABELS);
        spWeekly.setSelection(2);
    }

    private void bindSpinner(Spinner spinner, String[] labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void buildEquipmentOptions() {
        equipmentOptions.clear();
        for (Equipment equipment : EquipmentRepository.getAllEquipment()) {
            if (equipment != null) {
                equipmentOptions.add(new EquipmentOption(equipment.getId(), equipment.getName()));
            }
        }
        equipmentOptions.add(new EquipmentOption("treadmill", "跑步机"));
        renderEquipmentChecks(null);
    }

    private void renderEquipmentChecks(Set<String> selectedIds) {
        layoutEquipmentOptions.removeAllViews();
        equipmentChecks.clear();
        boolean selectAll = selectedIds == null || selectedIds.isEmpty();
        for (EquipmentOption option : equipmentOptions) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(option.name);
            checkBox.setTextColor(Color.parseColor("#FF111827"));
            checkBox.setTextSize(14);
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF00BFA6")));
            checkBox.setChecked(selectAll || selectedIds.contains(option.id));
            checkBox.setTag(option.id);
            checkBox.setPadding(0, dp(3), 0, dp(3));
            layoutEquipmentOptions.addView(checkBox);
            equipmentChecks.add(checkBox);
        }
    }

    private void bindEvents() {
        btnSaveProfile.setOnClickListener(v -> saveProfileAndGenerate());
        btnRegenerate.setOnClickListener(v -> generatePlan());
        btnEditProfile.setOnClickListener(v -> showProfileForm(currentProfile));
    }

    private void loadInitialState() {
        setLoading(true);
        executor.execute(() -> {
            try {
                GymEyeApiClient.HttpResult profileResult = GymEyeApiClient.get("/api/training-plan/profile", token);
                if (profileResult.code != 200) {
                    throw new IllegalStateException("profile " + profileResult.code);
                }
                JSONObject profile = profileResult.jsonOrEmpty().optJSONObject("profile");
                if (profile == null) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        showProfileForm(null);
                    });
                    return;
                }

                currentProfile = profile;
                JSONObject plan = loadCurrentOrGeneratePlan();
                mainHandler.post(() -> {
                    setLoading(false);
                    showPlan(plan);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showProfileForm(currentProfile);
                    Toast.makeText(this, "训练计划加载失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private JSONObject loadCurrentOrGeneratePlan() throws Exception {
        GymEyeApiClient.HttpResult currentResult = GymEyeApiClient.get("/api/training-plan/current", token);
        if (currentResult.code == 200) {
            JSONObject plan = currentResult.jsonOrEmpty().optJSONObject("plan");
            if (plan != null) {
                return plan;
            }
        }
        GymEyeApiClient.HttpResult generated = GymEyeApiClient.postJson("/api/training-plan/generate", token, new JSONObject());
        if (generated.code != 201) {
            throw new IllegalStateException("generate " + generated.code);
        }
        return generated.jsonOrEmpty().optJSONObject("plan");
    }

    private void showProfileForm(JSONObject profile) {
        currentProfile = profile;
        setLoading(false);
        planContent.setVisibility(View.GONE);
        profileForm.setVisibility(View.VISIBLE);

        if (profile == null) {
            etHeight.setText("");
            etWeight.setText("");
            spSex.setSelection(0);
            spGoal.setSelection(0);
            spLevel.setSelection(0);
            spWeekly.setSelection(2);
            renderEquipmentChecks(null);
            return;
        }

        etHeight.setText(formatNumber(profile.optDouble("heightCm", 0)));
        etWeight.setText(formatNumber(profile.optDouble("weightKg", 0)));
        spSex.setSelection(indexOf(SEX_VALUES, profile.optString("sex", "male")));
        spGoal.setSelection(indexOf(GOAL_VALUES, profile.optString("goal", "muscle_gain")));
        spLevel.setSelection(indexOf(LEVEL_VALUES, profile.optString("level", "beginner")));
        int weekly = profile.optInt("weeklySessions", 3);
        spWeekly.setSelection(Math.max(0, Math.min(6, weekly - 1)));

        Set<String> selectedIds = new HashSet<>();
        JSONArray arr = profile.optJSONArray("availableEquipmentIds");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                selectedIds.add(arr.optString(i));
            }
        }
        renderEquipmentChecks(selectedIds);
    }

    private void saveProfileAndGenerate() {
        JSONObject body = buildProfilePayload();
        if (body == null) return;

        setFormEnabled(false);
        setLoading(true);
        executor.execute(() -> {
            try {
                GymEyeApiClient.HttpResult profileResult = GymEyeApiClient.putJson("/api/training-plan/profile", token, body);
                if (profileResult.code != 200) {
                    throw new IllegalStateException(profileResult.jsonOrEmpty().optString("message", "保存失败"));
                }
                currentProfile = profileResult.jsonOrEmpty().optJSONObject("profile");
                GymEyeApiClient.HttpResult planResult = GymEyeApiClient.postJson("/api/training-plan/generate", token, new JSONObject());
                if (planResult.code != 201) {
                    throw new IllegalStateException(planResult.jsonOrEmpty().optString("message", "生成失败"));
                }
                JSONObject plan = planResult.jsonOrEmpty().optJSONObject("plan");
                mainHandler.post(() -> {
                    setFormEnabled(true);
                    setLoading(false);
                    showPlan(plan);
                    Toast.makeText(this, "训练计划已生成", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setFormEnabled(true);
                    setLoading(false);
                    Toast.makeText(this, "生成失败: " + safeMessage(e), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private JSONObject buildProfilePayload() {
        Double height = parseDouble(etHeight.getText() == null ? "" : etHeight.getText().toString());
        Double weight = parseDouble(etWeight.getText() == null ? "" : etWeight.getText().toString());
        if (height == null || height < 100 || height > 230) {
            etHeight.setError("请输入 100-230 cm");
            return null;
        }
        if (weight == null || weight < 30 || weight > 250) {
            etWeight.setError("请输入 30-250 kg");
            return null;
        }

        JSONArray equipmentIds = new JSONArray();
        for (CheckBox checkBox : equipmentChecks) {
            if (checkBox.isChecked()) {
                equipmentIds.put(String.valueOf(checkBox.getTag()));
            }
        }
        if (equipmentIds.length() == 0) {
            Toast.makeText(this, "至少选择一个可用器械", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("sex", SEX_VALUES[spSex.getSelectedItemPosition()]);
            body.put("heightCm", height);
            body.put("weightKg", weight);
            body.put("goal", GOAL_VALUES[spGoal.getSelectedItemPosition()]);
            body.put("level", LEVEL_VALUES[spLevel.getSelectedItemPosition()]);
            body.put("weeklySessions", spWeekly.getSelectedItemPosition() + 1);
            body.put("availableEquipmentIds", equipmentIds);
            return body;
        } catch (Exception e) {
            Toast.makeText(this, "资料格式错误", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void generatePlan() {
        setLoading(true);
        btnRegenerate.setEnabled(false);
        executor.execute(() -> {
            try {
                GymEyeApiClient.HttpResult result = GymEyeApiClient.postJson("/api/training-plan/generate", token, new JSONObject());
                if (result.code != 201) {
                    throw new IllegalStateException(result.jsonOrEmpty().optString("message", "生成失败"));
                }
                JSONObject plan = result.jsonOrEmpty().optJSONObject("plan");
                mainHandler.post(() -> {
                    setLoading(false);
                    btnRegenerate.setEnabled(true);
                    showPlan(plan);
                    Toast.makeText(this, "已重新生成", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    btnRegenerate.setEnabled(true);
                    Toast.makeText(this, "生成失败: " + safeMessage(e), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showPlan(JSONObject plan) {
        if (plan == null) {
            showProfileForm(currentProfile);
            return;
        }
        profileForm.setVisibility(View.GONE);
        planContent.setVisibility(View.VISIBLE);
        tvPlanTitle.setText(plan.optString("title", "今日训练计划"));
        tvWeeklySummary.setText(plan.optString("weeklySummary", ""));
        tvRationale.setText(plan.optString("rationale", ""));
        layoutPlanItems.removeAllViews();

        JSONArray items = plan.optJSONArray("items");
        if (items == null || items.length() == 0) {
            tvPlanEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvPlanEmpty.setVisibility(View.GONE);
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null) {
                layoutPlanItems.addView(createPlanItemCard(item, i + 1));
            }
        }
    }

    private View createPlanItemCard(JSONObject item, int index) {
        CardView card = new CardView(this);
        card.setCardElevation(dp(2));
        card.setRadius(dp(12));
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView title = new TextView(this);
        title.setText(index + ". " + item.optString("equipmentName", "器械") + "："
                + item.optString("actionName", "训练动作"));
        title.setTextColor(Color.parseColor("#FF111827"));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        box.addView(title);

        TextView prescription = new TextView(this);
        prescription.setText(formatPrescription(item));
        prescription.setTextColor(Color.parseColor("#FF008C7A"));
        prescription.setTextSize(14);
        prescription.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams prescriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        prescriptionParams.topMargin = dp(6);
        prescription.setLayoutParams(prescriptionParams);
        box.addView(prescription);

        TextView muscles = new TextView(this);
        muscles.setText("目标肌群: " + formatMuscles(item.optJSONArray("targetMuscles")));
        muscles.setTextColor(Color.parseColor("#FFDC2626"));
        muscles.setTextSize(13);
        LinearLayout.LayoutParams musclesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        musclesParams.topMargin = dp(6);
        muscles.setLayoutParams(musclesParams);
        box.addView(muscles);

        TextView note = new TextView(this);
        note.setText(item.optString("note", ""));
        note.setTextColor(Color.parseColor("#FF56616D"));
        note.setTextSize(13);
        note.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(8);
        note.setLayoutParams(noteParams);
        box.addView(note);

        Equipment equipment = EquipmentRepository.getEquipmentById(item.optString("equipmentId", ""));
        if (equipment != null) {
            TextView hint = new TextView(this);
            hint.setText("点击查看器械详情");
            hint.setGravity(Gravity.END);
            hint.setTextColor(Color.parseColor("#FF1E88E5"));
            hint.setTextSize(12);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            hintParams.topMargin = dp(8);
            hint.setLayoutParams(hintParams);
            box.addView(hint);
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, EquipmentDetailActivity.class);
                intent.putExtra(EquipmentDetailActivity.EXTRA_EQUIPMENT_NAME, equipment.getName());
                startActivity(intent);
            });
        }

        card.addView(box);
        return card;
    }

    private String formatPrescription(JSONObject item) {
        int duration = item.optInt("durationMinutes", 0);
        if (duration > 0) {
            return duration + " 分钟";
        }
        int sets = item.optInt("sets", 0);
        int reps = item.optInt("reps", 0);
        if (sets > 0 && reps > 0) {
            return sets + " 组 × " + reps + " 次";
        }
        return "按计划完成";
    }

    private String formatMuscles(JSONArray arr) {
        if (arr == null || arr.length() == 0) return "-";
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            out.add(EquipmentRepository.getMuscleNameCn(arr.optString(i)));
        }
        return TextUtils.join("、", out);
    }

    private void setLoading(boolean loading) {
        progressPlan.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void setFormEnabled(boolean enabled) {
        btnSaveProfile.setEnabled(enabled);
        spSex.setEnabled(enabled);
        spGoal.setEnabled(enabled);
        spLevel.setEnabled(enabled);
        spWeekly.setEnabled(enabled);
        etHeight.setEnabled(enabled);
        etWeight.setEnabled(enabled);
        for (CheckBox checkBox : equipmentChecks) {
            checkBox.setEnabled(enabled);
        }
    }

    private int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) return i;
        }
        return 0;
    }

    private Double parseDouble(String raw) {
        if (TextUtils.isEmpty(raw)) return null;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatNumber(double value) {
        if (value <= 0) return "";
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? "网络错误" : e.getMessage();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private static final class EquipmentOption {
        final String id;
        final String name;

        EquipmentOption(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
