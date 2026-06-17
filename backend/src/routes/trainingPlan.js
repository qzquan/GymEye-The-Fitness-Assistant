import express from 'express';
import { requireAuth } from '../middleware/auth.js';
import { AppError, asyncHandler } from '../utils/http.js';
import {
  createTrainingPlan,
  findUserById,
  getCurrentTrainingPlan,
  getTrainingProfileByUserId,
  saveTrainingProfile
} from '../store.js';
import { TRAINING_EQUIPMENT, TRAINING_EQUIPMENT_BY_ID } from '../training-catalog.js';

const router = express.Router();

const VALID_SEX = new Set(['male', 'female', 'other']);
const VALID_GOALS = new Set(['muscle_gain', 'fat_loss', 'body_shaping', 'rehab', 'strength']);
const VALID_LEVELS = new Set(['beginner', 'normal', 'advanced']);

const GOAL_CONFIG = {
  muscle_gain: {
    title: '今日训练计划：胸 + 三头',
    preferredIds: ['bench_press', 'pec_deck', 'triceps_pushdown', 'shoulder_press', 'lat_pulldown'],
    weekly: sessions => `建议每周训练 ${sessions} 次，按胸推、背拉、腿肩循环安排。`,
    rationale: '增肌目标优先选择胸、肩、手臂等推类力量动作，并按训练水平调整组数和次数。'
  },
  fat_loss: {
    title: '今日训练计划：全身循环 + 有氧',
    preferredIds: ['leg_press', 'lat_pulldown', 'bench_press', 'seated_row', 'shoulder_press', 'treadmill'],
    weekly: sessions => `建议每周训练 ${sessions} 次，力量循环和有氧交替，提高总消耗。`,
    rationale: '减脂目标优先组合大肌群动作和有氧项目，提升训练消耗并兼顾基础力量。'
  },
  body_shaping: {
    title: '今日训练计划：胸背腿肩塑形',
    preferredIds: ['pec_deck', 'lat_pulldown', 'leg_press', 'shoulder_press', 'biceps_curl', 'triceps_pushdown'],
    weekly: sessions => `建议每周训练 ${sessions} 次，围绕胸、背、腿、肩做均衡塑形。`,
    rationale: '塑形目标强调肌群覆盖和动作控制，因此选择中等强度、多部位组合。'
  },
  rehab: {
    title: '今日训练计划：低强度康复训练',
    preferredIds: ['leg_extension', 'leg_curl', 'seated_row', 'shoulder_press', 'treadmill'],
    weekly: sessions => `建议每周训练 ${sessions} 次，采用低强度动作并保留恢复日。`,
    rationale: '康复目标优先低风险、轨迹稳定的器械动作，并控制训练量。'
  },
  strength: {
    title: '今日训练计划：下肢 + 推拉力量',
    preferredIds: ['leg_press', 'bench_press', 'lat_pulldown', 'seated_row', 'shoulder_press'],
    weekly: sessions => `建议每周训练 ${sessions} 次，以大肌群力量训练为主，逐步增加负荷。`,
    rationale: '提高力量目标优先大肌群和复合动作，使用较低次数和更多组数。'
  }
};

function serializeProfile(row) {
  if (!row) return null;
  return {
    id: row.id,
    sex: row.sex,
    heightCm: row.height_cm,
    weightKg: row.weight_kg,
    goal: row.goal,
    level: row.level,
    weeklySessions: row.weekly_sessions,
    availableEquipmentIds: row.available_equipment_ids || [],
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function serializePlan(row) {
  if (!row) return null;
  return {
    id: row.id,
    title: row.title,
    weeklySummary: row.weekly_summary,
    rationale: row.rationale,
    items: row.items || [],
    profileSnapshot: row.profile_snapshot || null,
    createdAt: row.created_at
  };
}

function parseNumber(value, field, { min, max, integer = false }) {
  const n = Number(value);
  if (!Number.isFinite(n) || n < min || n > max || (integer && !Number.isInteger(n))) {
    throw new AppError(400, `${field} is invalid`, 'VALIDATION_ERROR');
  }
  return n;
}

function normalizeProfilePayload(body) {
  const sex = String(body?.sex || '').trim();
  const goal = String(body?.goal || '').trim();
  const level = String(body?.level || '').trim();
  if (!VALID_SEX.has(sex)) {
    throw new AppError(400, 'sex is invalid', 'VALIDATION_ERROR');
  }
  if (!VALID_GOALS.has(goal)) {
    throw new AppError(400, 'goal is invalid', 'VALIDATION_ERROR');
  }
  if (!VALID_LEVELS.has(level)) {
    throw new AppError(400, 'level is invalid', 'VALIDATION_ERROR');
  }

  const rawEquipmentIds = Array.isArray(body?.availableEquipmentIds)
    ? body.availableEquipmentIds
    : Array.isArray(body?.available_equipment_ids)
      ? body.available_equipment_ids
      : [];
  const availableEquipmentIds = [...new Set(rawEquipmentIds.map(value => String(value).trim()).filter(Boolean))]
    .filter(id => TRAINING_EQUIPMENT_BY_ID.has(id));
  if (availableEquipmentIds.length === 0) {
    throw new AppError(400, 'availableEquipmentIds must include at least one supported item', 'VALIDATION_ERROR');
  }

  return {
    sex,
    height_cm: parseNumber(body?.heightCm ?? body?.height_cm, 'heightCm', { min: 100, max: 230 }),
    weight_kg: parseNumber(body?.weightKg ?? body?.weight_kg, 'weightKg', { min: 30, max: 250 }),
    goal,
    level,
    weekly_sessions: parseNumber(body?.weeklySessions ?? body?.weekly_sessions, 'weeklySessions', {
      min: 1,
      max: 7,
      integer: true
    }),
    available_equipment_ids: availableEquipmentIds
  };
}

async function assertUserExists(userId) {
  const user = await findUserById(userId);
  if (!user) {
    throw new AppError(404, 'user not found', 'USER_NOT_FOUND');
  }
}

function desiredItemCount(profile) {
  if (profile.goal === 'rehab') return 3;
  if (profile.goal === 'fat_loss') return 4;
  return profile.level === 'beginner' ? 3 : 4;
}

function pickEquipment(profile) {
  const available = TRAINING_EQUIPMENT.filter(item => profile.available_equipment_ids.includes(item.id));
  if (available.length === 0) {
    throw new AppError(400, 'No available equipment can be used for this plan', 'VALIDATION_ERROR');
  }

  const availableById = new Map(available.map(item => [item.id, item]));
  const config = GOAL_CONFIG[profile.goal];
  const selected = [];
  const add = item => {
    if (item && !selected.some(existing => existing.id === item.id)) {
      selected.push(item);
    }
  };

  if (profile.goal === 'fat_loss') {
    for (const id of config.preferredIds.filter(id => id !== 'treadmill')) {
      if (selected.length >= desiredItemCount(profile) - 1) break;
      add(availableById.get(id));
    }
    add(availableById.get('treadmill'));
  } else {
    for (const id of config.preferredIds) {
      if (selected.length >= desiredItemCount(profile)) break;
      add(availableById.get(id));
    }
  }

  for (const item of available) {
    if (selected.length >= Math.min(desiredItemCount(profile), available.length)) break;
    add(item);
  }

  return selected.slice(0, Math.min(desiredItemCount(profile), selected.length));
}

function strengthMetrics(profile) {
  if (profile.goal === 'rehab') {
    return { sets: 2, reps: 12 };
  }
  if (profile.goal === 'strength') {
    return profile.level === 'advanced' ? { sets: 5, reps: 5 } : { sets: 4, reps: 6 };
  }
  if (profile.goal === 'muscle_gain') {
    if (profile.level === 'beginner') return { sets: 3, reps: 12 };
    if (profile.level === 'advanced') return { sets: 4, reps: 8 };
    return { sets: 4, reps: 10 };
  }
  if (profile.goal === 'fat_loss') {
    return { sets: 3, reps: 15 };
  }
  return profile.level === 'advanced' ? { sets: 4, reps: 12 } : { sets: 3, reps: 12 };
}

function durationMinutes(profile) {
  if (profile.goal === 'rehab') return 15;
  if (profile.level === 'advanced') return 25;
  if (profile.level === 'beginner') return 15;
  return 20;
}

function itemNote(profile, item) {
  if (item.durationOnly) {
    return profile.goal === 'fat_loss' ? '放在力量训练后完成，保持中等强度。' : '作为热身或收尾有氧，注意控制心率。';
  }
  if (profile.goal === 'rehab') {
    return '轻重量、慢速度，动作范围以无痛为准。';
  }
  if (profile.goal === 'strength') {
    return '选择可控重量，保留 1-2 次余力，重点保持动作稳定。';
  }
  if (profile.goal === 'muscle_gain') {
    return '每组最后 2 次应有挑战感，组间休息 60-90 秒。';
  }
  if (profile.goal === 'fat_loss') {
    return '组间休息控制在 45-60 秒，保持训练密度。';
  }
  return '控制节奏，关注目标肌群发力。';
}

function buildPlan(profile) {
  const config = GOAL_CONFIG[profile.goal];
  const selected = pickEquipment(profile);
  const fallbackUsed = selected.some(item => !config.preferredIds.includes(item.id));
  const metrics = strengthMetrics(profile);
  const items = selected.map(item => ({
    equipmentId: item.id,
    equipmentName: item.name,
    actionName: item.actionName,
    sets: item.durationOnly ? null : metrics.sets,
    reps: item.durationOnly ? null : metrics.reps,
    durationMinutes: item.durationOnly ? durationMinutes(profile) : null,
    targetMuscles: item.targetMuscles,
    note: itemNote(profile, item)
  }));

  const fallbackText = fallbackUsed
    ? '部分目标器械不可用，系统已从当前可用器械中选择最接近的替代动作。'
    : '当前可用器械能够覆盖本次训练目标。';

  return {
    title: config.title,
    weekly_summary: config.weekly(profile.weekly_sessions),
    rationale: `${config.rationale}${fallbackText}`,
    items
  };
}

router.get('/profile', requireAuth, asyncHandler(async (req, res) => {
  await assertUserExists(req.auth.id);
  const profile = await getTrainingProfileByUserId(req.auth.id);
  res.json({ ok: true, profile: serializeProfile(profile) });
}));

router.put('/profile', requireAuth, asyncHandler(async (req, res) => {
  await assertUserExists(req.auth.id);
  const payload = normalizeProfilePayload(req.body || {});
  const profile = await saveTrainingProfile(req.auth.id, payload);
  res.json({ ok: true, profile: serializeProfile(profile) });
}));

router.post('/generate', requireAuth, asyncHandler(async (req, res) => {
  await assertUserExists(req.auth.id);
  const profile = await getTrainingProfileByUserId(req.auth.id);
  if (!profile) {
    throw new AppError(404, 'training profile not found', 'PROFILE_NOT_FOUND');
  }
  const generated = buildPlan(profile);
  const saved = await createTrainingPlan(req.auth.id, {
    ...generated,
    profile_snapshot: serializeProfile(profile)
  });
  res.status(201).json({ ok: true, plan: serializePlan(saved) });
}));

router.get('/current', requireAuth, asyncHandler(async (req, res) => {
  await assertUserExists(req.auth.id);
  const plan = await getCurrentTrainingPlan(req.auth.id);
  res.json({ ok: true, plan: serializePlan(plan) });
}));

export default router;
