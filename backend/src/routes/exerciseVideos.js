import express from 'express';
import { requireAuth } from '../middleware/auth.js';
import {
  AppError,
  asyncHandler,
  optionalString,
  requireNonEmptyString
} from '../utils/http.js';
import {
  listExerciseVideos,
  getExerciseVideoById,
  createExerciseVideo,
  updateExerciseVideo,
  deleteExerciseVideo
} from '../store.js';

const router = express.Router();

function serializeVideo(row) {
  return {
    id: row.id,
    exerciseId: row.exercise_id,
    title: row.title,
    url: row.url,
    duration: row.duration
  };
}

router.get('/exercise/:exerciseId', asyncHandler(async (req, res) => {
  const exerciseId = Number(req.params.exerciseId);
  if (!Number.isInteger(exerciseId) || exerciseId <= 0) {
    throw new AppError(400, 'Invalid exercise id', 'VALIDATION_ERROR');
  }
  const rows = await listExerciseVideos(exerciseId);
  res.json({ ok: true, data: rows.map(serializeVideo) });
}));

router.get('/:id', asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid video id', 'VALIDATION_ERROR');
  }
  const item = await getExerciseVideoById(id);
  if (!item) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, data: serializeVideo(item) });
}));

router.post('/', requireAuth, asyncHandler(async (req, res) => {
  const exerciseId = Number(req.body?.exercise_id ?? req.body?.exerciseId);
  if (!Number.isInteger(exerciseId) || exerciseId <= 0) {
    throw new AppError(400, 'exercise_id is required', 'VALIDATION_ERROR');
  }
  const title = requireNonEmptyString(req.body?.title, 'title');
  const url = requireNonEmptyString(req.body?.url, 'url');
  const duration = optionalString(req.body?.duration);

  const created = await createExerciseVideo({
    exercise_id: exerciseId,
    title,
    url,
    duration
  });
  res.status(201).json({ ok: true, data: serializeVideo(created) });
}));

router.patch('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid video id', 'VALIDATION_ERROR');
  }
  const existing = await getExerciseVideoById(id);
  if (!existing) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }

  const patch = {};
  if (req.body?.exercise_id !== undefined || req.body?.exerciseId !== undefined) {
    patch.exercise_id = Number(req.body.exercise_id ?? req.body.exerciseId);
  }
  if (req.body?.title !== undefined) patch.title = requireNonEmptyString(req.body.title, 'title');
  if (req.body?.url !== undefined) patch.url = requireNonEmptyString(req.body.url, 'url');
  if (req.body?.duration !== undefined) patch.duration = optionalString(req.body.duration);

  const updated = await updateExerciseVideo(id, patch);
  res.json({ ok: true, data: serializeVideo(updated) });
}));

router.delete('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    throw new AppError(400, 'Invalid video id', 'VALIDATION_ERROR');
  }
  const deleted = await deleteExerciseVideo(id);
  if (!deleted) {
    throw new AppError(404, 'not found', 'NOT_FOUND');
  }
  res.json({ ok: true, message: 'deleted' });
}));

export default router;
