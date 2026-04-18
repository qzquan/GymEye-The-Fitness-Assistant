import express from 'express';
import { optionalAuth, requireAuth } from '../middleware/auth.js';
import {
  AppError,
  asyncHandler,
  buildPagination,
  parsePositiveInt,
  requireNonEmptyString,
  sendListResponse
} from '../utils/http.js';
import {
  createHistory,
  deleteHistory,
  findUserById,
  getEquipmentById,
  getEquipmentByName,
  getHistoryStats,
  listHistory,
  listHistoryAll
} from '../store.js';

const router = express.Router();

async function assertUserExists(userId) {
  const user = await findUserById(userId);
  if (!user) {
    throw new AppError(404, 'user not found', 'USER_NOT_FOUND');
  }
}

async function resolveEquipmentId({ equipmentId, equipmentName }) {
  if (equipmentId !== undefined) {
    const parsedId = parsePositiveInt(equipmentId, null);
    if (parsedId === null) {
      throw new AppError(400, 'Invalid equipmentId', 'VALIDATION_ERROR');
    }
    const equipment = await getEquipmentById(parsedId);
    if (!equipment) {
      throw new AppError(404, 'equipment not found', 'EQUIPMENT_NOT_FOUND');
    }
    return parsedId;
  }

  const name = requireNonEmptyString(equipmentName, 'equipmentName');
  const equipment = await getEquipmentByName(name);
  if (!equipment) {
    throw new AppError(404, 'equipment not found', 'EQUIPMENT_NOT_FOUND');
  }
  return equipment.id;
}

function normalizeScannedAt(scannedAt) {
  if (!scannedAt) {
    return new Date();
  }
  const date = new Date(scannedAt);
  if (Number.isNaN(date.getTime())) {
    throw new AppError(400, 'Invalid scannedAt value', 'VALIDATION_ERROR');
  }
  return date;
}

async function createHistoryRecord({ userId, equipmentId, scannedAt }) {
  await createHistory({
    user_id: userId,
    equipment_id: equipmentId,
    scanned_at: scannedAt
  });
}

function resolveRequestedUserId(req) {
  const bodyUserId = req.body?.userId;
  const queryUserId = req.query?.userId;
  const rawUserId = bodyUserId ?? queryUserId ?? req.auth?.id;
  const userId = parsePositiveInt(rawUserId, null);
  if (userId === null) {
    throw new AppError(400, 'userId is required', 'VALIDATION_ERROR');
  }
  if (req.auth && req.auth.id !== userId) {
    throw new AppError(403, 'Cannot access another user history', 'FORBIDDEN');
  }
  return userId;
}

router.post('/', optionalAuth, asyncHandler(async (req, res) => {
  const userId = resolveRequestedUserId(req);
  const equipmentId = await resolveEquipmentId({
    equipmentId: req.body?.equipmentId,
    equipmentName: req.body?.equipmentName
  });
  const scannedAt = normalizeScannedAt(req.body?.scannedAt);

  await assertUserExists(userId);
  await createHistoryRecord({ userId, equipmentId, scannedAt });

  res.status(201).json({ ok: true, message: 'saved' });
}));

router.post('/add', optionalAuth, asyncHandler(async (req, res) => {
  const userId = resolveRequestedUserId(req);
  const equipmentId = await resolveEquipmentId({
    equipmentId: req.body?.equipmentId,
    equipmentName: req.body?.equipmentName
  });
  const scannedAt = normalizeScannedAt(req.body?.scannedAt);

  await assertUserExists(userId);
  await createHistoryRecord({ userId, equipmentId, scannedAt });

  res.json({ ok: true, message: 'saved' });
}));

router.get('/', optionalAuth, asyncHandler(async (req, res) => {
  const userId = resolveRequestedUserId(req);
  await assertUserExists(userId);

  const pagination = buildPagination(req.query, { page: 1, limit: 20, maxLimit: 100 });
  const result = await listHistory({
    userId,
    limit: pagination.limit,
    offset: pagination.offset
  });

  sendListResponse(
    res,
    result.rows.map(row => ({
      id: row.id,
      scannedAt: row.scanned_at,
      equipment: {
        id: row.equipment?.id ?? null,
        name: row.equipment?.name ?? null,
        targetMuscle: row.equipment?.target_muscle ?? null
      }
    })),
    pagination,
    result.total
  );
}));

router.get('/list', optionalAuth, asyncHandler(async (req, res) => {
  const userId = resolveRequestedUserId(req);
  await assertUserExists(userId);

  const rows = await listHistoryAll(userId);

  res.json({
    ok: true,
    data: rows.map(row => ({
      id: row.id,
      scannedAt: row.scanned_at,
      equipmentName: row.equipment?.name ?? null,
      targetMuscle: row.equipment?.target_muscle ?? null
    }))
  });
}));

router.get('/stats/summary', requireAuth, asyncHandler(async (req, res) => {
  const userId = resolveRequestedUserId(req);
  await assertUserExists(userId);

  const stats = await getHistoryStats(userId);

  res.json({
    ok: true,
    data: stats
  });
}));

router.delete('/:id', requireAuth, asyncHandler(async (req, res) => {
  const recordId = parsePositiveInt(req.params.id, null);
  if (recordId === null) {
    throw new AppError(400, 'Invalid history id', 'VALIDATION_ERROR');
  }

  const result = await deleteHistory(recordId, req.auth.id);
  if (!result.found) {
    throw new AppError(404, 'history not found', 'NOT_FOUND');
  }
  if (!result.deleted) {
    throw new AppError(403, 'Cannot delete another user history', 'FORBIDDEN');
  }
  res.json({ ok: true, message: 'deleted' });
}));

export default router;
