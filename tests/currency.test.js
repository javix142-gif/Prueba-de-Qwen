import { describe, it, expect } from 'vitest';
import { 
  formatCLP, 
  calculateBabyAge, 
  isStockLow, 
  isValidExpenseAmount,
  calculateTotalExpenses,
  calculateRemainingBudget
} from '../src/utils/currency.js';

describe('formatCLP', () => {
  it('formats zero correctly', () => {
    expect(formatCLP(0)).toBe('$0');
  });

  it('formats positive integers correctly', () => {
    expect(formatCLP(1000)).toBe('$1.000');
    expect(formatCLP(1234567)).toBe('$1.234.567');
  });

  it('throws error for negative amounts', () => {
    expect(() => formatCLP(-100)).toThrow('Amount must be a non-negative integer');
  });

  it('throws error for non-integer numbers', () => {
    expect(() => formatCLP(100.50)).toThrow('Amount must be a non-negative integer');
  });

  it('throws error for non-number types', () => {
    expect(() => formatCLP('100')).toThrow('Amount must be a non-negative integer');
    expect(() => formatCLP(null)).toThrow('Amount must be a non-negative integer');
  });
});

describe('calculateBabyAge', () => {
  it('calculates age in months correctly', () => {
    const result = calculateBabyAge('2024-01-15', '2024-03-15');
    expect(result.months).toBe(2);
    expect(result.days).toBe(0);
  });

  it('calculates age with remaining days', () => {
    const result = calculateBabyAge('2024-01-15', '2024-03-20');
    expect(result.months).toBe(2);
    expect(result.days).toBe(5);
  });

  it('handles year boundary correctly', () => {
    const result = calculateBabyAge('2023-11-15', '2024-01-15');
    expect(result.months).toBe(2);
  });

  it('throws error for future birth dates', () => {
    expect(() => calculateBabyAge('2099-01-01')).toThrow('Birth date cannot be in the future');
  });

  it('throws error for invalid date format', () => {
    expect(() => calculateBabyAge('invalid-date')).toThrow('Invalid birth date format');
  });
});

describe('isStockLow', () => {
  it('returns true when current stock is below minimum', () => {
    expect(isStockLow(2, 5)).toBe(true);
  });

  it('returns false when current stock equals minimum', () => {
    expect(isStockLow(5, 5)).toBe(false);
  });

  it('returns false when current stock is above minimum', () => {
    expect(isStockLow(10, 5)).toBe(false);
  });

  it('throws error for negative stock values', () => {
    expect(() => isStockLow(-1, 5)).toThrow('Stock values cannot be negative');
    expect(() => isStockLow(5, -1)).toThrow('Stock values cannot be negative');
  });

  it('throws error for non-number inputs', () => {
    expect(() => isStockLow('5', 5)).toThrow('Stock values must be numbers');
  });
});

describe('isValidExpenseAmount', () => {
  it('returns true for valid positive integers', () => {
    expect(isValidExpenseAmount(0)).toBe(true);
    expect(isValidExpenseAmount(1000)).toBe(true);
    expect(isValidExpenseAmount(1234567)).toBe(true);
  });

  it('returns false for negative numbers', () => {
    expect(isValidExpenseAmount(-100)).toBe(false);
  });

  it('returns false for non-integers', () => {
    expect(isValidExpenseAmount(100.50)).toBe(false);
  });

  it('returns false for non-numbers', () => {
    expect(isValidExpenseAmount('100')).toBe(false);
    expect(isValidExpenseAmount(null)).toBe(false);
  });
});

describe('calculateTotalExpenses', () => {
  it('returns 0 for empty array', () => {
    expect(calculateTotalExpenses([])).toBe(0);
  });

  it('sums expenses correctly', () => {
    const expenses = [
      { amount_clp: 1000 },
      { amount_clp: 2000 },
      { amount_clp: 3000 }
    ];
    expect(calculateTotalExpenses(expenses)).toBe(6000);
  });

  it('throws error for invalid expense amounts', () => {
    const expenses = [
      { amount_clp: 1000 },
      { amount_clp: -500 }
    ];
    expect(() => calculateTotalExpenses(expenses)).toThrow('Invalid expense amount');
  });

  it('throws error for non-array input', () => {
    expect(() => calculateTotalExpenses(null)).toThrow('Expenses must be an array');
  });
});

describe('calculateRemainingBudget', () => {
  it('calculates remaining budget correctly', () => {
    expect(calculateRemainingBudget(10000, 3000)).toBe(7000);
  });

  it('returns negative when overspent', () => {
    expect(calculateRemainingBudget(5000, 7000)).toBe(-2000);
  });

  it('returns full budget when nothing spent', () => {
    expect(calculateRemainingBudget(10000, 0)).toBe(10000);
  });

  it('throws error for invalid inputs', () => {
    expect(() => calculateRemainingBudget(-100, 50)).toThrow();
    expect(() => calculateRemainingBudget(100, -50)).toThrow();
    expect(() => calculateRemainingBudget('100', 50)).toThrow();
  });
});
