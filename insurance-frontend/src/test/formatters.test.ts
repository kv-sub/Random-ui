import { describe, it, expect } from 'vitest';
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  dateToInputFormat,
} from '../utils/formatters';

describe('formatCurrency', () => {
  it('formats a whole number with two decimal places', () => {
    expect(formatCurrency(1000)).toBe('$1,000.00');
  });

  it('formats a decimal amount correctly', () => {
    expect(formatCurrency(1234.56)).toBe('$1,234.56');
  });

  it('formats zero correctly', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });

  it('formats a small amount', () => {
    expect(formatCurrency(0.5)).toBe('$0.50');
  });
});

describe('formatDate', () => {
  it('formats a Date object', () => {
    const date = new Date(2024, 0, 15); // Jan 15, 2024
    expect(formatDate(date)).toMatch(/Jan/);
    expect(formatDate(date)).toMatch(/15/);
    expect(formatDate(date)).toMatch(/2024/);
  });

  it('formats a date string', () => {
    const result = formatDate('2024-06-01');
    expect(result).toMatch(/2024/);
    expect(result).toMatch(/Jun/);
  });
});

describe('formatDateTime', () => {
  it('formats a Date object with time', () => {
    const dt = new Date(2024, 5, 1, 14, 30); // Jun 1 2024 14:30
    const result = formatDateTime(dt);
    expect(result).toMatch(/2024/);
    expect(result).toMatch(/Jun/);
  });

  it('formats a datetime string', () => {
    const result = formatDateTime('2024-12-25T10:00:00');
    expect(result).toMatch(/2024/);
    expect(result).toMatch(/Dec/);
  });
});

describe('dateToInputFormat', () => {
  it('formats date as yyyy-mm-dd', () => {
    const date = new Date(2024, 0, 5); // Jan 5, 2024
    expect(dateToInputFormat(date)).toBe('2024-01-05');
  });

  it('pads single-digit month and day', () => {
    const date = new Date(2024, 8, 9); // Sep 9, 2024
    expect(dateToInputFormat(date)).toBe('2024-09-09');
  });

  it('handles double-digit month and day', () => {
    const date = new Date(2024, 11, 31); // Dec 31, 2024
    expect(dateToInputFormat(date)).toBe('2024-12-31');
  });
});
