import { describe, it, expect, beforeEach } from 'vitest';
import { ApiError, getErrorMessage, mapHttpErrorToMessage } from '../api/errors';

describe('ApiError', () => {
  it('creates error with correct properties', () => {
    const error = new ApiError(404, 'Not found', { field: 'value' });

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe('ApiError');
    expect(error.status).toBe(404);
    expect(error.message).toBe('Not found');
    expect(error.details).toEqual({ field: 'value' });
  });

  it('creates error without details', () => {
    const error = new ApiError(400, 'Bad request');

    expect(error.status).toBe(400);
    expect(error.details).toBeUndefined();
  });

  it('is an instance of Error', () => {
    const error = new ApiError(500, 'Server error');
    expect(error instanceof Error).toBe(true);
  });
});

describe('getErrorMessage', () => {
  it('returns ApiError message', () => {
    const error = new ApiError(404, 'Policy not found');
    expect(getErrorMessage(error)).toBe('Policy not found');
  });

  it('returns generic Error message', () => {
    const error = new Error('Generic error message');
    expect(getErrorMessage(error)).toBe('Generic error message');
  });

  it('returns fallback for string errors', () => {
    expect(getErrorMessage('some string')).toBe('An unexpected error occurred');
  });

  it('returns fallback for null', () => {
    expect(getErrorMessage(null)).toBe('An unexpected error occurred');
  });

  it('returns fallback for undefined', () => {
    expect(getErrorMessage(undefined)).toBe('An unexpected error occurred');
  });
});

describe('mapHttpErrorToMessage', () => {
  it('returns default message for 400 without custom message', () => {
    expect(mapHttpErrorToMessage(400)).toBe('Invalid request. Please check your input.');
  });

  it('returns custom message for 400 with custom message', () => {
    expect(mapHttpErrorToMessage(400, 'Custom validation error')).toBe('Custom validation error');
  });

  it('returns fixed message for 401 regardless of custom message', () => {
    expect(mapHttpErrorToMessage(401)).toBe('Unauthorized. Please log in again.');
    expect(mapHttpErrorToMessage(401, 'ignored')).toBe('Unauthorized. Please log in again.');
  });

  it('returns fixed message for 403', () => {
    expect(mapHttpErrorToMessage(403)).toBe('Forbidden. You do not have permission.');
  });

  it('returns default message for 404', () => {
    expect(mapHttpErrorToMessage(404)).toBe('Resource not found.');
  });

  it('returns custom message for 404 with override', () => {
    expect(mapHttpErrorToMessage(404, 'Policy not found')).toBe('Policy not found');
  });

  it('returns duplicate claim message for 409', () => {
    expect(mapHttpErrorToMessage(409)).toBe('Duplicate claim detected within 24 hours.');
  });

  it('returns server error message for 500', () => {
    expect(mapHttpErrorToMessage(500)).toBe('Server error. Please try again later.');
  });

  it('returns default message for unknown status codes', () => {
    expect(mapHttpErrorToMessage(418)).toBe('An error occurred. Please try again.');
    expect(mapHttpErrorToMessage(503)).toBe('An error occurred. Please try again.');
  });
});
