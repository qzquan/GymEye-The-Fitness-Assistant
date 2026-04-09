import express from 'express';
import pool from '../db.js';

const router = express.Router();

router.get('/:name', async (req, res) => {
  const { name } = req.params;
  try {
    const [rows] = await pool.query('SELECT id, name, description, target_muscle, tutorial_text, tutorial_url FROM equipment WHERE name = ?', [name]);
    if (rows.length === 0) {
      return res.status(404).json({ ok: false, message: 'not found' });
    }
    return res.json({ ok: true, data: rows[0] });
  } catch (e) {
    return res.status(500).json({ ok: false, message: 'server error' });
  }
});

export default router;

