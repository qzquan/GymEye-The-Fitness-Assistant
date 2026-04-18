import { AppError } from '../utils/http.js';

export function notFoundHandler(req, _res, next) {
  next(new AppError(404, `Route not found: ${req.method} ${req.originalUrl}`, 'NOT_FOUND'));
}

export function errorHandler(error, _req, res, _next) {
  if (error instanceof AppError) {
    return res.status(error.statusCode).json({
      ok: false,
      code: error.code,
      message: error.message,
      details: error.details
    });
  }

  if (error && error.code === 'ER_DUP_ENTRY') {
    return res.status(409).json({
      ok: false,
      code: 'DUPLICATE_ENTRY',
      message: 'Resource already exists'
    });
  }

  console.error(error);
  return res.status(500).json({
    ok: false,
    code: 'INTERNAL_ERROR',
    message: 'Server error'
  });
}
