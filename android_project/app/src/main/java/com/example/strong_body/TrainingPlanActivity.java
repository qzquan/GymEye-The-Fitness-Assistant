package com.example.strong_body;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
    private static final String[] WEEKLY_LABELS = {"1 次", "2 次", "3 次", "4 次", "5 次", "6 次", "7 次"};
    private static final String[] WEEKLY_VALUES = {"1", "2", "3", "4", "5", "6", "7"};

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<EquipmentOption> equipmentOptions = new ArrayList<>();
    private final List<Chip> equipmentChips = new ArrayList<>();

    private ProgressBar progressPlan;
    private LinearLayout profileForm;
    private LinearLayout planContent;
    private View stepBasic;
    private View stepGoal;
    private View stepEquipment;
    private View stepDot1;
    private View stepDot2;
    private View stepDot3;
    private TextView tvStepLabel;
    private TextView tvStepTitle;
    private TextView tvStepHint;
    private TextView tvProfileSummary;
    private ChipGroup chipGroupSex;
    private ChipGroup chipGroupGoal;
    private ChipGroup chipGroupLevel;
    private ChipGroup chipGroupWeekly;
    private ChipGroup chipGroupEquipment;
    private EditText etHeight;
    private EditText etWeight;
    private MaterialButton btnStepBack;
    private MaterialButton btnStepNext;
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
    private int currentStep;

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
        setupChoiceGroups();
        buildEquipmentOptions();
        bindEvents();
        loadInitialState();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        progressPlan = findViewById(R.id.progressPlan);
        profileForm = findViewById(R.id.profileForm);
        planContent = findViewById(R.id.planContent);
        stepBasic = findViewById(R.id.stepBasic);
        stepGoal = findViewById(R.id.stepGoal);
        stepEquipment = findViewById(R.id.stepEquipment);
        stepDot1 = findViewById(R.id.stepDot1);
        stepDot2 = findViewById(R.id.stepDot2);
        stepDot3 = findViewById(R.id.stepDot3);
        tvStepLabel = findViewById(R.id.tvStepLabel);
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepHint = findViewById(R.id.tvStepHint);
        tvProfileSummary = findViewById(R.id.tvProfileSummary);
        chipGroupSex = findViewById(R.id.chipGroupSex);
        chipGroupGoal = findViewById(R.id.chipGroupGoal);
        chipGroupLevel = findViewById(R.id.chipGroupLevel);
        chipGroupWeekly = findViewById(R.id.chipGroupWeekly);
        chipGroupEquipment = findViewById(R.id.chipGroupEquipment);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        btnStepBack = findViewById(R.id.btnStepBack);
        btnStepNext = findViewById(R.id.btnStepNext);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        tvPlanTitle = findViewById(R.id.tvPlanTitle);
        tvWeeklySummary = findViewById(R.id.tvWeeklySummary);
        tvRationale = findViewById(R.id.tvRationale);
        tvPlanEmpty = findViewById(R.id.tvPlanEmpty);
        layoutPlanItems = findViewById(R.id.layoutPlanItems);
    }

    private void setupChoiceGroups() {
        addChoiceChips(chipGroupSex, SEX_LABELS, SEX_VALUES, 0);
        addChoiceChips(chipGroupGoal, GOAL_LABELS, GOAL_VALUES, 0);
        addChoiceChips(chipGroupLevel, LEVEL_LABELS, LEVEL_VALUES, 0);
        addChoiceChips(chipGroupWeekly, WEEKLY_LABELS, WEEKLY_VALUES, 2);
    }

    private void addChoiceChips(ChipGroup group, String[] labels, String[] values, int selectedIndex) {
        group.removeAllViews();
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        for (int i = 0; i < labels.length; i++) {
            Chip chip = createChoiceChip(labels[i]);
            chip.setId(View.generateViewId());
            chip.setTag(values[i]);
            chip.setChecked(i == selectedIndex);
            group.addView(chip);
        }
    }

    private Chip createChoiceChip(String label) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setTextSize(13);
        chip.setMinHeight(dp(38));
        chip.setChipMinHeight(dp(38));
        chip.setChipCornerRadius(dp(19));
        chip.setChipStrokeWidth(dp(1));
        chip.setChipBackgroundColor(chipBackgroundColors());
        chip.setChipStrokeColor(chipStrokeColors());
        chip.setTextColor(chipTextColors());
        chip.setRippleColor(ColorStateList.valueOf(Color.parseColor("#3300BFA6")));
        return chip;
    }

    private ColorStateList chipBackgroundColors() {
        return new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#FF101418"), Color.parseColor("#FFF7FBFC")}
        );
    }

    private ColorStateList chipTextColors() {
        return new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.WHITE, Color.parseColor("#FF56616D")}
        );
    }

    private ColorStateList chipStrokeColors() {
        return new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Color.parseColor("#FF00BFA6"), Color.parseColor("#FFD8E0E7")}
        );
    }

    private void buildEquipmentOptions() {
        equipmentOptions.clear();
        for (Equipment equipment : EquipmentRepository.getAllEquipment()) {
            if (equipment != null) {
                equipmentOptions.add(new EquipmentOption(equipment.getId(), equipment.getName()));
            }
        }
        equipmentOptions.add(new EquipmentOption("treadmill", "跑步机"));
        renderEquipmentChips(null);
    }

    private void renderEquipmentChips(Set<String> selectedIds) {
        chipGroupEquipment.removeAllViews();
        equipmentChips.clear();
        boolean selectAll = selectedIds == null || selectedIds.isEmpty();
        for (EquipmentOption option : equipmentOptions) {
            Chip chip = createChoiceChip(option.name);
            chip.setId(View.generateViewId());
            chip.setTag(option.id);
            chip.setChecked(selectAll || selectedIds.contains(option.id));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked && countCheckedEquipment() == 0) {
                    chip.setChecked(true);
                    Toast.makeText(this, "至少保留 1 个可用器械", Toast.LENGTH_SHORT).show();
                }
                updateEquipmentSummary();
            });
            chipGroupEquipment.addView(chip);
            equipmentChips.add(chip);
        }
        updateEquipmentSummary();
    }

    private void updateEquipmentSummary() {
        if (tvProfileSummary == null) return;
        tvProfileSummary.setText("已选择 " + countCheckedEquipment() + " 个可用器械，默认全选，可按实际情况调整。");
    }

    private int countCheckedEquipment() {
        int count = 0;
        for (Chip chip : equipmentChips) {
            if (chip.isChecked()) count++;
        }
        return count;
    }

    private void bindEvents() {
        btnStepBack.setOnClickListener(v -> setStep(currentStep - 1));
        btnStepNext.setOnClickListener(v -> {
            if (currentStep == 0 && !validateBasicInputs()) return;
            setStep(currentStep + 1);
        });
        btnSaveProfile.setOnClickListener(v -> saveProfileAndGenerate());
        btnRegenerate.setOnClickListener(v -> generatePlan());
        btnEditProfile.setOnClickListener(v -> showProfileForm(currentProfile));
    }

    private void setStep(int step) {
        currentStep = Math.max(0, Math.min(2, step));
        stepBasic.setVisibility(currentStep == 0 ? View.VISIBLE : View.GONE);
        stepGoal.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        stepEquipment.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);

        stepDot1.setBackgroundResource(currentStep >= 0 ? R.drawable.bg_plan_step_active : R.drawable.bg_plan_step_inactive);
        stepDot2.setBackgroundResource(currentStep >= 1 ? R.drawable.bg_plan_step_active : R.drawable.bg_plan_step_inactive);
        stepDot3.setBackgroundResource(currentStep >= 2 ? R.drawable.bg_plan_step_active : R.drawable.bg_plan_step_inactive);

        btnStepBack.setVisibility(currentStep == 0 ? View.INVISIBLE : View.VISIBLE);
        btnStepNext.setVisibility(currentStep == 2 ? View.GONE : View.VISIBLE);
        btnSaveProfile.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);

        if (currentStep == 0) {
            tvStepLabel.setText("STEP 1 / 3");
            tvStepTitle.setText("基础身体信息");
            tvStepHint.setText("填写用于估算训练强度的基础信息。");
        } else if (currentStep == 1) {
            tvStepLabel.setText("STEP 2 / 3");
            tvStepTitle.setText("目标与训练节奏");
            tvStepHint.setText("选择你的目标、水平和一周训练频率。");
        } else {
            tvStepLabel.setText("STEP 3 / 3");
            tvStepTitle.setText("可用器械");
            tvStepHint.setText("系统会优先从已选器械里匹配今天的训练动作。");
        }
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
            selectChipByTag(chipGroupSex, "male");
            selectChipByTag(chipGroupGoal, "muscle_gain");
            selectChipByTag(chipGroupLevel, "beginner");
            selectChipByTag(chipGroupWeekly, "3");
            renderEquipmentChips(null);
            setStep(0);
            return;
        }

        etHeight.setText(formatNumber(profile.optDouble("heightCm", 0)));
        etWeight.setText(formatNumber(profile.optDouble("weightKg", 0)));
        selectChipByTag(chipGroupSex, profile.optString("sex", "male"));
        selectChipByTag(chipGroupGoal, profile.optString("goal", "muscle_gain"));
        selectChipByTag(chipGroupLevel, profile.optString("level", "beginner"));
        selectChipByTag(chipGroupWeekly, String.valueOf(profile.optInt("weeklySessions", 3)));

        Set<String> selectedIds = new HashSet<>();
        JSONArray arr = profile.optJSONArray("availableEquipmentIds");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                selectedIds.add(arr.optString(i));
            }
        }
        renderEquipmentChips(selectedIds);
        setStep(0);
    }

    private void selectChipByTag(ChipGroup group, String targetTag) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip && TextUtils.equals(String.valueOf(child.getTag()), targetTag)) {
                ((Chip) child).setChecked(true);
                return;
            }
        }
        if (group.getChildCount() > 0 && group.getChildAt(0) instanceof Chip) {
            ((Chip) group.getChildAt(0)).setChecked(true);
        }
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
        if (!validateBasicInputs()) return null;

        JSONArray equipmentIds = new JSONArray();
        for (Chip chip : equipmentChips) {
            if (chip.isChecked()) {
                equipmentIds.put(String.valueOf(chip.getTag()));
            }
        }
        if (equipmentIds.length() == 0) {
            Toast.makeText(this, "至少选择一个可用器械", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("sex", getCheckedTag(chipGroupSex, "male"));
            body.put("heightCm", parseDouble(etHeight.getText() == null ? "" : etHeight.getText().toString()));
            body.put("weightKg", parseDouble(etWeight.getText() == null ? "" : etWeight.getText().toString()));
            body.put("goal", getCheckedTag(chipGroupGoal, "muscle_gain"));
            body.put("level", getCheckedTag(chipGroupLevel, "beginner"));
            body.put("weeklySessions", Integer.parseInt(getCheckedTag(chipGroupWeekly, "3")));
            body.put("availableEquipmentIds", equipmentIds);
            return body;
        } catch (Exception e) {
            Toast.makeText(this, "资料格式错误", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private boolean validateBasicInputs() {
        Double height = parseDouble(etHeight.getText() == null ? "" : etHeight.getText().toString());
        Double weight = parseDouble(etWeight.getText() == null ? "" : etWeight.getText().toString());
        if (height == null || height < 100 || height > 230) {
            etHeight.setError("请输入 100-230 cm");
            return false;
        }
        if (weight == null || weight < 30 || weight > 250) {
            etWeight.setError("请输入 30-250 kg");
            return false;
        }
        return true;
    }

    private String getCheckedTag(ChipGroup group, String fallback) {
        int checkedId = group.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            View checked = group.findViewById(checkedId);
            if (checked != null && checked.getTag() != null) {
                return String.valueOf(checked.getTag());
            }
        }
        return fallback;
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
        card.setCardElevation(dp(1));
        card.setRadius(dp(8));
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(this);
        badge.setText(String.format(Locale.getDefault(), "%02d", index));
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(12);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.bg_plan_step_active);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(42), dp(28));
        badgeParams.rightMargin = dp(10);
        header.addView(badge, badgeParams);

        TextView title = new TextView(this);
        title.setText(item.optString("equipmentName", "器械") + " · "
                + item.optString("actionName", "训练动作"));
        title.setTextColor(Color.parseColor("#FF111827"));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(header);

        TextView prescription = new TextView(this);
        prescription.setText(formatPrescription(item));
        prescription.setTextColor(Color.parseColor("#FF008C7A"));
        prescription.setTextSize(14);
        prescription.setTypeface(null, Typeface.BOLD);
        prescription.setBackgroundResource(R.drawable.bg_sport_card_light);
        prescription.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams prescriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        prescriptionParams.topMargin = dp(12);
        box.addView(prescription, prescriptionParams);

        TextView muscles = new TextView(this);
        muscles.setText("目标肌群: " + formatMuscles(item.optJSONArray("targetMuscles")));
        muscles.setTextColor(Color.parseColor("#FF1E88E5"));
        muscles.setTextSize(13);
        muscles.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams musclesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        musclesParams.topMargin = dp(10);
        box.addView(muscles, musclesParams);

        TextView note = new TextView(this);
        note.setText(item.optString("note", ""));
        note.setTextColor(Color.parseColor("#FF56616D"));
        note.setTextSize(13);
        note.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(6);
        box.addView(note, noteParams);

        Equipment equipment = EquipmentRepository.getEquipmentById(item.optString("equipmentId", ""));
        if (equipment != null) {
            TextView hint = new TextView(this);
            hint.setText("点击查看器械详情");
            hint.setGravity(Gravity.END);
            hint.setTextColor(Color.parseColor("#FF008C7A"));
            hint.setTextSize(12);
            hint.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            hintParams.topMargin = dp(10);
            box.addView(hint, hintParams);
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
            return duration + " 分钟有氧";
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
        btnStepBack.setEnabled(enabled);
        btnStepNext.setEnabled(enabled);
        btnSaveProfile.setEnabled(enabled);
        etHeight.setEnabled(enabled);
        etWeight.setEnabled(enabled);
        setChipGroupEnabled(chipGroupSex, enabled);
        setChipGroupEnabled(chipGroupGoal, enabled);
        setChipGroupEnabled(chipGroupLevel, enabled);
        setChipGroupEnabled(chipGroupWeekly, enabled);
        setChipGroupEnabled(chipGroupEquipment, enabled);
    }

    private void setChipGroupEnabled(ChipGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
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
