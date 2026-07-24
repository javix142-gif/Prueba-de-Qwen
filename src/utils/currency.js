/**
 * Format a number as Chilean Pesos (CLP)
 * @param {number} amount - Amount in CLP
 * @returns {string} Formatted string like "$1.234.567"
 */
export function formatCLP(amount) {
  if (typeof amount !== 'number' || amount < 0 || !Number.isInteger(amount)) {
    throw new Error('Amount must be a non-negative integer');
  }
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

/**
 * Calculate the age of a baby in months and days
 * @param {string} birthDateStr - Birth date in ISO format (YYYY-MM-DD)
 * @param {string} [referenceDate] - Reference date in ISO format (defaults to today)
 * @returns {{ months: number, days: number }} Age in months and remaining days
 */
export function calculateBabyAge(birthDateStr, referenceDate) {
  const birthDate = new Date(birthDateStr);
  const refDate = referenceDate ? new Date(referenceDate) : new Date();
  
  if (isNaN(birthDate.getTime())) {
    throw new Error('Invalid birth date format');
  }
  
  if (birthDate > refDate) {
    throw new Error('Birth date cannot be in the future');
  }
  
  // Calculate difference in months
  let months = (refDate.getFullYear() - birthDate.getFullYear()) * 12;
  months -= birthDate.getMonth();
  months += refDate.getMonth();
  
  // Calculate remaining days
  const tempDate = new Date(birthDate);
  tempDate.setMonth(tempDate.getMonth() + months);
  
  let days;
  if (tempDate <= refDate) {
    days = Math.floor((refDate - tempDate) / (1000 * 60 * 60 * 24));
  } else {
    months--;
    tempDate.setMonth(tempDate.getMonth() - 1);
    days = Math.floor((refDate - tempDate) / (1000 * 60 * 60 * 24));
  }
  
  return { months, days };
}

/**
 * Check if a stock level is below minimum
 * @param {number} currentStock - Current stock level
 * @param {number} minimumStock - Minimum stock threshold
 * @returns {boolean} True if stock is below minimum
 */
export function isStockLow(currentStock, minimumStock) {
  if (typeof currentStock !== 'number' || typeof minimumStock !== 'number') {
    throw new Error('Stock values must be numbers');
  }
  if (currentStock < 0 || minimumStock < 0) {
    throw new Error('Stock values cannot be negative');
  }
  return currentStock <= minimumStock && currentStock !== minimumStock;
}

/**
 * Validate expense amount
 * @param {number} amount - Amount to validate
 * @returns {boolean} True if valid
 */
export function isValidExpenseAmount(amount) {
  return typeof amount === 'number' && Number.isInteger(amount) && amount >= 0;
}

/**
 * Calculate total expenses from an array
 * @param {Array<{amount_clp: number}>} expenses - Array of expense objects
 * @returns {number} Total amount in CLP
 */
export function calculateTotalExpenses(expenses) {
  if (!Array.isArray(expenses)) {
    throw new Error('Expenses must be an array');
  }
  return expenses.reduce((total, expense) => {
    if (!isValidExpenseAmount(expense.amount_clp)) {
      throw new Error('Invalid expense amount');
    }
    return total + expense.amount_clp;
  }, 0);
}

/**
 * Calculate remaining budget
 * @param {number} budget - Monthly budget
 * @param {number} spent - Amount already spent
 * @returns {number} Remaining budget
 */
export function calculateRemainingBudget(budget, spent) {
  if (!isValidExpenseAmount(budget) || !isValidExpenseAmount(spent)) {
    throw new Error('Budget and spent amounts must be non-negative integers');
  }
  return budget - spent;
}
