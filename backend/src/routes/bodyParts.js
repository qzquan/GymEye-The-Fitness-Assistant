import express from 'express';
import { requireAuth } from '../middleware/auth.js';
import {
  AppError,
  asyncHandler,
  requireNonEmptyString
} from '../utils/http.js';
import {
  listBodyParts,
  getBodyPartById,
  getBodyPartByName,
  createBodyPart,
  updateBodyPart,
  deleteBodyPart
} from '../store.js';

const router = express.Router();

function serializeBodyPart(row) {
  return { id: row.id, name: row.name };
}

router.get('/', asyncHandler(async (_req, res) => {
  const rows = await listBodyParts();
  res.json({ ok: true, data: rows.map(serializeBodyPart) });
}));

router.get('/:id', asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid body part id', 'VALIDATION_ERROR');
  }
  const item = await getBodyPartById(id);
  if (!item) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, data: serializeBodyPart(item) });
}));

router.post('/', requireAuth, asyncHandler(async (req, res) => {
  const name = requireNonEmptyString(req.body?.name, 'name');
  const existing = await getBodyPartByName(name);
  if (existing) {
    throw new AppError(409, 'Resource already exists', 'DUPLICATE_ENTRY');
  }
  const created = await createBodyPart({ name });
  res.status(201).json({ ok: true, data: serializeBodyPart(created) });
}));

router.patch('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid body part id', 'VALIDATION_ERROR');
  }
  const existing = await getBodyPartById(id);
  if (!existing) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  const nextName = req.body?.name === undefined ? existing.name : requireNonEmptyString(req.body?.name, 'name');
  const updated = await updateBodyPart(id, { name: nextName });
  res.json({ ok: true, data: serializeBodyPart(updated) });
}));

router.delete('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid body part id', 'VALIDATION_ERROR');
  }
  const deleted = await deleteBodyPart(id);
  if (!deleted) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, message: 'deleted' });
}));

export default router;
