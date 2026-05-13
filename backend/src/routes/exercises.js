import express from 'express';
import { requireAuth } from '../middleware/auth.js';
import {
  AppError,
  asyncHandler,
  buildPagination,
  optionalString,
  requireNonEmptyString,
  sendListResponse
} from '../utils/http.js';
import {
  listExercises,
  getExerciseById,
  getExercisesByEquipmentId,
  createExercise,
  updateExercise,
  deleteExercise,
  listBodyParts,
  listDifficultyLevels
} from '../store.js';

const router = express.Router();

function serializeExercise(row, bodyPartMap, difficultyMap) {
  const bodyParts = (row.body_part_ids || [])
    .map(id => bodyPartMap.get(id))
    .filter(Boolean)
    .map(bp => ({ id: bp.id, name: bp.name }));

  const difficulty = row.difficulty_level_id ? difficultyMap.get(row.difficulty_level_id) : null;

  return {
    id: row.id,
    equipmentId: row.equipment_id,
    name: row.name,
    steps: row.steps,
    commonMistakes: row.common_mistakes,
    safetyTips: row.safety_tips,
    targetAudience: row.target_audience,
    difficultyLevel: difficulty ? { id: difficulty.id, name: difficulty.name } : null,
    bodyParts
  };
}

async function buildLookupMaps() {
  const [bodyParts, difficultyLevels] = await Promise.all([listBodyParts(), listDifficultyLevels()]);
  const bodyPartMap = new Map(bodyParts.map(bp => [bp.id, bp]));
  const difficultyMap = new Map(difficultyLevels.map(dl => [dl.id, dl]));
  return { bodyPartMap, difficultyMap };
}

router.get('/', asyncHandler(async (req, res) => {
  const pagination = buildPagination(req.query, { page: 1, limit: 20, maxLimit: 100 });
  const keyword = optionalString(req.query.q);
  const equipmentId = req.query.equipmentId ? Number(req.query.equipmentId) : undefined;
  const difficultyLevelId = req.query.difficultyLevelId ? Number(req.query.difficultyLevelId) : undefined;
  const bodyPartId = req.query.bodyPartId ? Number(req.query.bodyPartId) : undefined;

  const result = await listExercises({
    q: keyword,
    equipmentId,
    difficultyLevelId,
    bodyPartId,
    limit: pagination.limit,
    offset: pagination.offset
  });

  const { bodyPartMap, difficultyMap } = await buildLookupMaps();
  sendListResponse(res, result.rows.map(row => serializeExercise(row, bodyPartMap, difficultyMap)), pagination, result.total);
}));

router.get('/equipment/:equipmentId', asyncHandler(async (req, res) => {
  const equipmentId = Number(req.params.equipmentId);
  if (!Number.isInteger(equipmentId) || equipmentId <= 0) {
    throw new AppError(400, 'Invalid equipment id', 'VALIDATION_ERROR');
  }
  const rows = await getExercisesByEquipmentId(equipmentId);
  const { bodyPartMap, difficultyMap } = await buildLookupMaps();
  res.json({ ok: true, data: rows.map(row => serializeExercise(row, bodyPartMap, difficultyMap)) });
}));

router.get('/:id', asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid exercise id', 'VALIDATION_ERROR');
  }
  const item = await getExerciseById(id);
  if (!item) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  const { bodyPartMap, difficultyMap } = await buildLookupMaps();
  res.json({ ok: true, data: serializeExercise(item, bodyPartMap, difficultyMap) });
}));

router.post('/', requireAuth, asyncHandler(async (req, res) => {
  const name = requireNonEmptyString(req.body?.name, 'name');
  const equipmentId = Number(req.body?.equipment_id ?? req.body?.equipmentId);
  if (!Number.isInteger(equipmentId) || equipmentId <= 0) {
    throw new AppError(400, 'equipment_id is required', 'VALIDATION_ERROR');
  }
  const steps = requireNonEmptyString(req.body?.steps, 'steps');
  const commonMistakes = optionalString(req.body?.common_mistakes ?? req.body?.commonMistakes);
  const safetyTips = optionalString(req.body?.safety_tips ?? req.body?.safetyTips);
  const targetAudience = optionalString(req.body?.target_audience ?? req.body?.targetAudience);
  const difficultyLevelId = req.body?.difficulty_level_id ?? req.body?.difficultyLevelId;
  const bodyPartIds = req.body?.body_part_ids ?? req.body?.bodyPartIds;

  const created = await createExercise({
    equipment_id: equipmentId,
    name,
    steps,
    common_mistakes: commonMistakes,
    safety_tips: safetyTips,
    target_audience: targetAudience,
    difficulty_level_id: difficultyLevelId != null ? Number(difficultyLevelId) : null,
    body_part_ids: Array.isArray(bodyPartIds) ? bodyPartIds.map(Number) : []
  });

  const { bodyPartMap, difficultyMap } = await buildLookupMaps();
  res.status(201).json({ ok: true, data: serializeExercise(created, bodyPartMap, difficultyMap) });
}));

router.patch('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid exercise id', 'VALIDATION_ERROR');
  }
  const existing = await getExerciseById(id);
  if (!existing) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }

  const patch = {};
  if (req.body?.name !== undefined) patch.name = requireNonEmptyString(req.body.name, 'name');
  if (req.body?.equipment_id !== undefined || req.body?.equipmentId !== undefined) {
    patch.equipment_id = Number(req.body.equipment_id ?? req.body.equipmentId);
  }
  if (req.body?.steps !== undefined) patch.steps = requireNonEmptyString(req.body.steps, 'steps');
  if (req.body?.common_mistakes !== undefined || req.body?.commonMistakes !== undefined) {
    patch.common_mistakes = optionalString(req.body.common_mistakes ?? req.body.commonMistakes);
  }
  if (req.body?.safety_tips !== undefined || req.body?.safetyTips !== undefined) {
    patch.safety_tips = optionalString(req.body.safety_tips ?? req.body.safetyTips);
  }
  if (req.body?.target_audience !== undefined || req.body?.targetAudience !== undefined) {
    patch.target_audience = optionalString(req.body.target_audience ?? req.body.targetAudience);
  }
  if (req.body?.difficulty_level_id !== undefined || req.body?.difficultyLevelId !== undefined) {
    const val = req.body.difficulty_level_id ?? req.body.difficultyLevelId;
    patch.difficulty_level_id = val != null ? Number(val) : null;
  }
  if (req.body?.body_part_ids !== undefined || req.body?.bodyPartIds !== undefined) {
    const val = req.body.body_part_ids ?? req.body.bodyPartIds;
    patch.body_part_ids = Array.isArray(val) ? val.map(Number) : [];
  }

  const updated = await updateExercise(id, patch);
  const { bodyPartMap, difficultyMap } = await buildLookupMaps();
  res.json({ ok: true, data: serializeExercise(updated, bodyPartMap, difficultyMap) });
}));

router.delete('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid exercise id', 'VALIDATION_ERROR');
  }
  const deleted = await deleteExercise(id);
  if (!deleted) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, message: 'deleted' });
}));

export default router;
