import express from 'express';
import pool from '../db.js';

const router = express.Router();

router.post('/add', async (req, res) => {
  const { userId, equipmentName, scannedAt } = req.body || {};
  if (!userId || !equipmentName) {
    return res.status(400).json({ ok: false, message: 'userId and equipmentName are required' });
    }
  try {
    const [eqRows] = await pool.query('SELECT id FROM equipment WHERE name = ?', [equipmentName]);
    if (eqRows.length === 0) {
      return res.status(404).json({ ok: false, message: 'equipment not found' });
    }
    const equipmentId = eqRows[0].id;
    const ts = scannedAt ? new Date(scannedAt) : new Date();
    await pool.query('INSERT INTO history (user_id, equipment_id, scanned_at) VALUES (?, ?, ?)', [
      userId,
      equipmentId,
      ts
    ]);
    return res.json({ ok: true, message: 'saved' });
  } catch (e) {
    return res.status(500).json({ ok: false, message: 'server error' });
  }
});

router.get('/list', async (req, res) => {
  const userId = req.query.userId;
  if (!userId) {
    return res.status(400).json({ ok: false, message: 'userId is required' });
  }
  try {
    const [rows] = await pool.query(
      `SELECT h.id, h.scanned_at, e.name AS equipment_name, e.target_muscle
       FROM history h
       JOIN equipment e ON e.id = h.equipment_id
       WHERE h.user_id = ?
       ORDER BY h.scanned_at DESC`,
      [userId]
    );
    return res.json({ ok: true, data: rows });
  } catch (e) {
    return res.status(500).json({ ok: false, message: 'server error' });
  }
});

export default router;

