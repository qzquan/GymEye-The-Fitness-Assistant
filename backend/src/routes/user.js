import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import config from '../config.js';
import { requireAuth } from '../middleware/auth.js';
import { AppError, asyncHandler, optionalString, requireNonEmptyString } from '../utils/http.js';
import { createUser, findUserByEmail, findUserById, updateUser } from '../store.js';

const router = express.Router();

function normalizeEmail(email) {
  return requireNonEmptyString(email, 'email').toLowerCase();
}

function validatePassword(password) {
  const value = requireNonEmptyString(password, 'password');
  if (value.length < 6) {
    throw new AppError(400, 'password must be at least 6 characters', 'VALIDATION_ERROR');
  }
  return value;
}

function serializeUser(user) {
  return {
    id: user.id,
    email: user.email,
    nickname: user.nickname,
    createdAt: user.created_at
  };
}

function signToken(user) {
  return jwt.sign({ id: user.id, email: user.email }, config.jwtSecret, { expiresIn: '7d' });
}

router.post('/register', asyncHandler(async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const password = validatePassword(req.body?.password);
  const nickname = optionalString(req.body?.nickname, 100) || email.split('@')[0];

  const existingUser = await findUserByEmail(email);
  if (existingUser) {
    throw new AppError(409, 'email already exists', 'EMAIL_EXISTS');
  }

  const hash = await bcrypt.hash(password, 10);
  const user = await createUser({ email, password_hash: hash, nickname });
  const token = signToken(user);

  res.status(201).json({
    ok: true,
    mode: 'registered',
    userId: user.id,
    token,
    user: serializeUser(user)
  });
}));

router.post('/login', asyncHandler(async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const password = validatePassword(req.body?.password);

  let user = await findUserByEmail(email);
  let mode = 'login';

  if (!user) {
    const hash = await bcrypt.hash(password, 10);
    const nickname = email.split('@')[0];
    user = await createUser({ email, password_hash: hash, nickname });
    mode = 'registered';
  } else {
    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) {
      throw new AppError(401, 'invalid credentials', 'INVALID_CREDENTIALS');
    }
  }

  const token = signToken(user);
  res.json({
    ok: true,
    mode,
    userId: user.id,
    token,
    user: serializeUser(user)
  });
}));

router.get('/me', requireAuth, asyncHandler(async (req, res) => {
  const user = await findUserById(req.auth.id);
  if (!user) {
    throw new AppError(404, 'user not found', 'USER_NOT_FOUND');
  }
  res.json({ ok: true, user: serializeUser(user) });
}));

router.patch('/me', requireAuth, asyncHandler(async (req, res) => {
  const nickname = optionalString(req.body?.nickname, 100);
  const email = req.body?.email === undefined ? undefined : normalizeEmail(req.body?.email);

  if (nickname === null && email === undefined) {
    throw new AppError(400, 'At least one editable field is required', 'VALIDATION_ERROR');
  }

  const currentUser = await findUserById(req.auth.id);
  if (!currentUser) {
    throw new AppError(404, 'user not found', 'USER_NOT_FOUND');
  }

  const nextEmail = email ?? currentUser.email;
  const nextNickname = nickname ?? currentUser.nickname;

  if (nextEmail !== currentUser.email) {
    const existingUser = await findUserByEmail(nextEmail);
    if (existingUser && existingUser.id !== currentUser.id) {
      throw new AppError(409, 'email already exists', 'EMAIL_EXISTS');
    }
  }

  const updatedUser = await updateUser(currentUser.id, {
    email: nextEmail,
    nickname: nextNickname
  });
  res.json({ ok: true, user: serializeUser(updatedUser) });
}));

export default router;
