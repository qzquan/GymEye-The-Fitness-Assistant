import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import pool from '../db.js';

const router = express.Router();

router.post('/login', async (req, res) => {
  const { email, password } = req.body || {};
  if (!email || !password) {
    return res.status(400).json({ ok: false, message: 'email and password are required' });
  }
  try {
    const [rows] = await pool.query('SELECT * FROM users WHERE email = ?', [email]);
    if (rows.length === 0) {
      const hash = await bcrypt.hash(password, 10);
      const nickname = email.split('@')[0];
      const [result] = await pool.query('INSERT INTO users (email, password_hash, nickname) VALUES (?, ?, ?)', [
        email,
        hash,
        nickname
      ]);
      const id = result.insertId;
      const token = jwt.sign({ id, email }, process.env.JWT_SECRET || 'secret', { expiresIn: '7d' });
      return res.json({ ok: true, mode: 'registered', userId: id, token });
    }
    const user = rows[0];
    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) {
      return res.status(401).json({ ok: false, message: 'invalid credentials' });
    }
    const token = jwt.sign({ id: user.id, email: user.email }, process.env.JWT_SECRET || 'secret', { expiresIn: '7d' });
    return res.json({ ok: true, mode: 'login', userId: user.id, token });
  } catch (e) {
    return res.status(500).json({ ok: false, message: 'server error' });
  }
});

export default router;

