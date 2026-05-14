import jwt from 'jsonwebtoken';
import config from '../config.js';
import { AppError } from '../utils/http.js';

function readBearerToken(req) {
  const authorization = req.headers.authorization || '';
  if (!authorization.startsWith('Bearer ')) {
    return null;
  }
  return authorization.slice('Bearer '.length).trim();
}

export function optionalAuth(req, _res, next) {
  const token = readBearerToken(req);
  if (!token) {
    req.auth = null;
    return next();
  }

  try {
    req.auth = jwt.verify(token, config.jwtSecret);
    return next();
  } catch {
    return next(new AppError(401, 'Invalid token', 'UNAUTHORIZED'));
  }
}

export function requireAuth(req, _res, next) {
  const token = readBearerToken(req);
  if (!token) {
    return next(new AppError(401, 'Authorization token is required', 'UNAUTHORIZED'));
  }

  try {
    req.auth = jwt.verify(token, config.jwtSecret);
    return next();
  } catch {
    return next(new AppError(401, 'Invalid token', 'UNAUTHORIZED'));
  }
}
