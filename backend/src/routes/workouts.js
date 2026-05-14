import express from 'express';
import { requireAuth } from '../middleware/auth.js';
import {
  AppError,
  asyncHandler,
  buildPagination,
  parsePositiveInt,
  requireNonEmptyString,
  sendListResponse
} from '../utils/http.js';
import {
  createWorkoutLog,
  deleteWorkoutLog,
  findUserById,
  getWorkoutStats,
  listWorkoutLogs
} from '../store.js';

const router = express.Router();

const FEELINGS = new Set(['easy', 'moderate', 'hard']);

function parsePerformedAt(raw) {
  if (raw === undefined || raw === null || raw === '') {
    return new Date().toISOString();
  }
  const d = new Date(raw);
  if (Number.isNaN(d.getTime())) {
    throw new AppError(400, 'performedAt invalid', 'VALIDATION_ERROR');
  }
  return d.toISOString();
}

function parseOptionalNumber(value, field, { min, max, integer }) {
  if (value === undefined || value === null || value === '') {
    return null;
  }
  const n = Number(value);
  if (!Number.isFinite(n)) {
    throw new AppError(400, `${field} must be a number`, 'VALIDATION_ERROR');
  }
  if (integer && !Number.isInteger(n)) {
    throw new AppError(400, `${field} must be an integer`, 'VALIDATION_ERROR');
  }
  if (n < min || n > max) {
    throw new AppError(400, `${field} out of range`, 'VALIDATION_ERROR');
  }
  return n;
}

function serializeLog(row) {
  return {
    id: row.id,
    exerciseName: row.exercise_name,
    sets: row.sets,
    repsPerSet: row.reps_per_set,
    weightKg: row.weight_kg,
    durationMinutes: row.duration_minutes,
    feeling: row.feeling,
    bodyPart: row.body_part,
    performedAt: row.performed_at,
    createdAt: row.created_at
  };
}

router.post('/', requireAuth, asyncHandler(async (req, res) => {
  const user = await findUserById(req.auth.id);
  if (!user) {
    throw new AppError(404, 'user not found', 'USER_NOT_FOUND');
  }
  const exercise_name = requireNonEmptyString(
    req.body?.exerciseName ?? req.body?.exercise_name,
    'exerciseName'
  );
  const sets = parseOptionalNumber(req.body?.sets, 'sets', { min: 1, max: 99, integer: true });
  const reps_per_set = parseOptionalNumber(req.body?.repsPerSet ?? req.body?.reps_per_set, 'repsPerSet', {
    min: 1,
    max: 200,
    integer: true
  });
  const weight_kg = parseOptionalNumber(req.body?.weightKg ?? req.body?.weight_kg, 'weightKg', {
    min: 0,
    max: 500,
    integer: false
  });
  const duration_minutes = parseOptionalNumber(
    req.body?.durationMinutes ?? req.body?.duration_minutes,
    'durationMinutes',
    { min: 0, max: 1440, integer: true }
  );
  const feeling = req.body?.feeling;
  if (!FEELINGS.has(feeling)) {
    throw new AppError(400, 'feeling must be easy | moderate | hard', 'VALIDATION_ERROR');
  }
  const body_part =
    req.body?.bodyPart === undefined || req.body?.bodyPart === null
      ? null
      : typeof req.body.bodyPart === 'string' && req.body.bodyPart.trim()
        ? req.body.bodyPart.trim().slice(0, 64)
        : null;

  if (sets === null && reps_per_set !== null) {
    throw new AppError(400, 'sets is required when repsPerSet is set', 'VALIDATION_ERROR');
  }
  if (reps_per_set === null && sets !== null) {
    throw new AppError(400, 'repsPerSet is required when sets is set', 'VALIDATION_ERROR');
  }

  const hasSetsReps = sets !== null && reps_per_set !== null;
  const hasDuration = duration_minutes !== null;
  const hasWeight = weight_kg !== null;
  if (!hasSetsReps && !hasDuration && !hasWeight) {
    throw new AppError(
      400,
      'Provide (sets + repsPerSet) and/or durationMinutes and/or weightKg',
      'VALIDATION_ERROR'
    );
  }

  const performed_at = parsePerformedAt(req.body?.performedAt ?? req.body?.performed_at);

  const saved = await createWorkoutLog(req.auth.id, {
    exercise_name,
    sets,
    reps_per_set,
    weight_kg,
    duration_minutes,
    feeling,
    body_part,
    performed_at
  });
  res.status(201).json({ ok: true, log: serializeLog(saved) });
}));

router.get('/', requireAuth, asyncHandler(async (req, res) => {
  const user = await findUserById(req.auth.id);
  if (!user) {
    throw new AppError(404, 'user not found', 'USER_NOT_FOUND');
  }
  const pagination = buildPagination(req.query, { page: 1, limit: 20, maxLimit: 200 });
  const from = req.query?.from;
  const to = req.query?.to;
  if (from && typeof from === 'string' && !/^\d{4}-\d{2}-\d{2}$/.test(from)) {
    throw new AppError(400, 'from must be YYYY-MM-DD', 'VALIDATION_ERROR');
  }
  if (to && typeof to === 'string' && !/^\d{4}-\d{2}-\d{2}$/.test(to)) {
    throw new AppError(400, 'to must be YYYY-MM-DD', 'VALIDATION_ERROR');
  }
  const result = await listWorkoutLogs({
    userId: req.auth.id,
    limit: pagination.limit,
    offset: pagination.offset,
    from: from || null,
    to: to || null
  });
  sendListResponse(res, result.rows.map(serializeLog), pagination, result.total);
}));

router.get('/stats/summary', requireAuth, asyncHandler(async (req, res) => {
  const user = await findUserById(req.auth.id);
  if (!user) {
    throw new AppError(404, 'user not found', 'USER_NOT_FOUND');
  }
  const stats = await getWorkoutStats(req.auth.id);
  res.json({ ok: true, data: stats });
}));

router.delete('/:id', requireAuth, asyncHandler(async (req, res) => {
  const id = parsePositiveInt(req.params.id, null);
  if (id === null) {
    throw new AppError(400, 'Invalid id', 'VALIDATION_ERROR');
  }
  const result = await deleteWorkoutLog(id, req.auth.id);
  if (!result.found) {
    throw new AppError(404, 'workout log not found', 'NOT_FOUND');
  }
  if (!result.deleted) {
    throw new AppError(403, 'Forbidden', 'FORBIDDEN');
  }
  res.json({ ok: true, message: 'deleted' });
}));

export default router;
