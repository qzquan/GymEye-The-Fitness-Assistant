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
  createEquipment,
  deleteEquipment,
  getEquipmentById,
  getEquipmentByName,
  listEquipment,
  listEquipmentTargets,
  updateEquipment,
  getExercisesByEquipmentId,
  listBodyParts,
  listDifficultyLevels,
  listExerciseVideos
} from '../store.js';

const router = express.Router();

function serializeEquipment(row, options = {}) {
  const base = {
    id: row.id,
    name: row.name,
    description: row.description,
    targetMuscle: row.target_muscle,
    tutorialText: row.tutorial_text,
    tutorialUrl: row.tutorial_url,
    category: row.category,
    suitableFor: row.suitable_for
  };
  if (options.exercises) {
    base.exercises = options.exercises;
  }
  return base;
}

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

router.get('/', asyncHandler(async (req, res) => {
  const pagination = buildPagination(req.query, { page: 1, limit: 20, maxLimit: 100 });
  const keyword = optionalString(req.query.q);
  const targetMuscle = optionalString(req.query.targetMuscle || req.query.target_muscle, 255);
  const result = await listEquipment({
    q: keyword,
    targetMuscle,
    limit: pagination.limit,
    offset: pagination.offset
  });
  sendListResponse(res, result.rows.map(serializeEquipment), pagination, result.total);
}));

router.get('/targets', asyncHandler(async (_req, res) => {
  const targets = await listEquipmentTargets();
  res.json({ ok: true, data: targets });
}));

router.get('/id/:id', asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid equipment id', 'VALIDATION_ERROR');
  }

  const equipment = await getEquipmentById(id);
  if (!equipment) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }

  const includeExercises = req.query.include === 'exercises';
  if (includeExercises) {
    const exercises = await getExercisesByEquipmentId(id);
    const [bodyParts, difficultyLevels] = await Promise.all([listBodyParts(), listDifficultyLevels()]);
    const bodyPartMap = new Map(bodyParts.map(bp => [bp.id, bp]));
    const difficultyMap = new Map(difficultyLevels.map(dl => [dl.id, dl]));

    const enrichedExercises = await Promise.all(exercises.map(async ex => {
      const videos = await listExerciseVideos(ex.id);
      return {
        ...serializeExercise(ex, bodyPartMap, difficultyMap),
        videos: videos.map(v => ({ id: v.id, title: v.title, url: v.url, duration: v.duration }))
      };
    }));

    res.json({ ok: true, data: serializeEquipment(equipment, { exercises: enrichedExercises }) });
  } else {
    res.json({ ok: true, data: serializeEquipment(equipment) });
  }
}));

router.post('/', requireAuth, asyncHandler(async (req, res) => {
  const name = requireNonEmptyString(req.body?.name, 'name');
  const description = optionalString(req.body?.description);
  const targetMuscle = optionalString(req.body?.targetMuscle || req.body?.target_muscle, 255);
  const tutorialText = optionalString(req.body?.tutorialText || req.body?.tutorial_text);
  const tutorialUrl = optionalString(req.body?.tutorialUrl || req.body?.tutorial_url, 1024);
  const category = optionalString(req.body?.category);
  const suitableFor = optionalString(req.body?.suitable_for || req.body?.suitableFor);

  const existing = await getEquipmentByName(name);
  if (existing) {
    throw new AppError(409, 'Resource already exists', 'DUPLICATE_ENTRY');
  }
  const equipment = await createEquipment({
    name,
    description,
    target_muscle: targetMuscle,
    tutorial_text: tutorialText,
    tutorial_url: tutorialUrl,
    category,
    suitable_for: suitableFor
  });
  res.status(201).json({ ok: true, data: serializeEquipment(equipment) });
}));

router.patch('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid equipment id', 'VALIDATION_ERROR');
  }

  const equipment = await getEquipmentById(id);
  if (!equipment) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }

  const nextName = req.body?.name === undefined ? equipment.name : requireNonEmptyString(req.body?.name, 'name');
  const nextDescription = req.body?.description === undefined ? equipment.description : optionalString(req.body?.description);
  const nextTargetMuscle = req.body?.targetMuscle === undefined && req.body?.target_muscle === undefined
    ? equipment.target_muscle
    : optionalString(req.body?.targetMuscle || req.body?.target_muscle, 255);
  const nextTutorialText = req.body?.tutorialText === undefined && req.body?.tutorial_text === undefined
    ? equipment.tutorial_text
    : optionalString(req.body?.tutorialText || req.body?.tutorial_text);
  const nextTutorialUrl = req.body?.tutorialUrl === undefined && req.body?.tutorial_url === undefined
    ? equipment.tutorial_url
    : optionalString(req.body?.tutorialUrl || req.body?.tutorial_url, 1024);

  const duplicate = await getEquipmentByName(nextName);
  if (duplicate && duplicate.id !== id) {
    throw new AppError(409, 'Resource already exists', 'DUPLICATE_ENTRY');
  }
  const nextCategory = req.body?.category === undefined ? equipment.category : optionalString(req.body?.category);
  const nextSuitableFor = req.body?.suitable_for === undefined && req.body?.suitableFor === undefined
    ? equipment.suitable_for
    : optionalString(req.body?.suitable_for || req.body?.suitableFor);

  const updatedEquipment = await updateEquipment(id, {
    name: nextName,
    description: nextDescription,
    target_muscle: nextTargetMuscle,
    tutorial_text: nextTutorialText,
    tutorial_url: nextTutorialUrl,
    category: nextCategory,
    suitable_for: nextSuitableFor
  });
  res.json({ ok: true, data: serializeEquipment(updatedEquipment) });
}));

router.delete('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid equipment id', 'VALIDATION_ERROR');
  }

  const deleted = await deleteEquipment(id);
  if (!deleted) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }

  res.json({ ok: true, message: 'deleted' });
}));

router.get('/:name', asyncHandler(async (req, res) => {
  const name = requireNonEmptyString(req.params.name, 'name');
  const equipment = await getEquipmentByName(name);
  if (!equipment) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, data: serializeEquipment(equipment) });
}));

export default router;
