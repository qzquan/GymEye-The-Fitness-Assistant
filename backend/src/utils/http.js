export class AppError extends Error {
  constructor(statusCode, message, code = 'APP_ERROR', details = null) {
    super(message);
    this.name = 'AppError';
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
  }
}

export function asyncHandler(handler) {
  return async (req, res, next) => {
    try {
      await handler(req, res, next);
    } catch (error) {
      next(error);
    }
  };
}

export function parsePositiveInt(value, fallback, { min = 1, max = Number.MAX_SAFE_INTEGER } = {}) {
  if (value === undefined || value === null || value === '') {
    return fallback;
  }
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < min || parsed > max) {
    return null;
  }
  return parsed;
}

export function requireNonEmptyString(value, fieldName) {
  if (typeof value !== 'string' || !value.trim()) {
    throw new AppError(400, `${fieldName} is required`, 'VALIDATION_ERROR');
  }
  return value.trim();
}

export function optionalString(value, maxLength = 0) {
  if (value === undefined || value === null) {
    return null;
  }
  if (typeof value !== 'string') {
    throw new AppError(400, 'Invalid string field', 'VALIDATION_ERROR');
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  if (maxLength > 0 && trimmed.length > maxLength) {
    throw new AppError(400, `Field must be at most ${maxLength} characters`, 'VALIDATION_ERROR');
  }
  return trimmed;
}

export function buildPagination(query, defaults = {}) {
  const page = parsePositiveInt(query.page, defaults.page || 1, { min: 1, max: 100000 });
  const limit = parsePositiveInt(query.limit, defaults.limit || 20, { min: 1, max: defaults.maxLimit || 100 });
  if (page === null || limit === null) {
    throw new AppError(400, 'Invalid pagination parameters', 'VALIDATION_ERROR');
  }
  return {
    page,
    limit,
    offset: (page - 1) * limit
  };
}

export function toIsoString(value) {
  return value instanceof Date ? value.toISOString() : value;
}

export function sendListResponse(res, data, pagination, total) {
  return res.json({
    ok: true,
    data,
    pagination: {
      page: pagination.page,
      limit: pagination.limit,
      total
    }
  });
}
