import express from 'express';
import { requireAuth } from '../middleware/auth.js';
import {
  AppError,
  asyncHandler,
  optionalString,
  requireNonEmptyString
} from '../utils/http.js';
import {
  listDifficultyLevels,
  getDifficultyLevelById,
  getDifficultyLevelByName,
  createDifficultyLevel,
  updateDifficultyLevel,
  deleteDifficultyLevel
} from '../store.js';

const router = express.Router();

function serializeDifficultyLevel(row) {
  return { id: row.id, name: row.name, description: row.description };
}

router.get('/', asyncHandler(async (_req, res) => {
  const rows = await listDifficultyLevels();
  res.json({ ok: true, data: rows.map(serializeDifficultyLevel) });
}));

router.get('/:id', asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid difficulty level id', 'VALIDATION_ERROR');
  }
  const item = await getDifficultyLevelById(id);
  if (!item) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, data: serializeDifficultyLevel(item) });
}));

router.post('/', requireAuth, asyncHandler(async (req, res) => {
  const name = requireNonEmptyString(req.body?.name, 'name');
  const description = optionalString(req.body?.description);
  const existing = await getDifficultyLevelByName(name);
  if (existing) {
    throw new AppError(409, 'Resource already exists', 'DUPLICATE_ENTRY');
  }
  const created = await createDifficultyLevel({ name, description });
  res.status(201).json({ ok: true, data: serializeDifficultyLevel(created) });
}));

router.patch('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid difficulty level id', 'VALIDATION_ERROR');
  }
  const existing = await getDifficultyLevelById(id);
  if (!existing) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  const nextName = req.body?.name === undefined ? existing.name : requireNonEmptyString(req.body?.name, 'name');
  const nextDescription = req.body?.description === undefined ? existing.description : optionalString(req.body?.description);
  const updated = await updateDifficultyLevel(id, { name: nextName, description: nextDescription });
  res.json({ ok: true, data: serializeDifficultyLevel(updated) });
}));

router.delete('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid difficulty level id', 'VALIDATION_ERROR');
  }
  const deleted = await deleteDifficultyLevel(id);
  if (!deleted) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, message: 'deleted' });
}));

export default router;
