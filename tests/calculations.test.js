import { describe, it, expect } from 'vitest'

/**
 * Test de cálculos básicos para la aplicación Primer Año
 * Estos tests verifican funciones utilitarias que se usarán en la aplicación
 */

describe('Cálculos básicos', () => {
  // Simulación de formato de moneda chilena
  const formatCLP = (amount) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0
    }).format(amount)
  }

  // Cálculo de edad en meses desde fecha de nacimiento
  const calculateAgeInMonths = (birthDate) => {
    const today = new Date()
    const birth = new Date(birthDate)
    const months = (today.getFullYear() - birth.getFullYear()) * 12 + 
                   (today.getMonth() - birth.getMonth())
    return Math.max(0, months)
  }

  // Cálculo de saldo presupuestario
  const calculateBudgetBalance = (budget, expenses) => {
    const totalExpenses = expenses.reduce((sum, expense) => sum + expense.amount, 0)
    return budget - totalExpenses
  }

  // Verificación de stock bajo
  const isLowStock = (currentStock, minimumStock) => {
    return currentStock <= minimumStock
  }

  describe('formatCLP', () => {
    it('formatea correctamente montos en pesos chilenos', () => {
      expect(formatCLP(1000)).toBe('$1.000')
      expect(formatCLP(400000)).toBe('$400.000')
      expect(formatCLP(286450)).toBe('$286.450')
    })

    it('maneja cero correctamente', () => {
      expect(formatCLP(0)).toBe('$0')
    })
  })

  describe('calculateAgeInMonths', () => {
    it('calcula la edad en meses correctamente', () => {
      // Fecha fija para testing: 3 meses atrás
      const threeMonthsAgo = new Date()
      threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3)
      
      const age = calculateAgeInMonths(threeMonthsAgo.toISOString())
      expect(age).toBeGreaterThanOrEqual(2)
      expect(age).toBeLessThanOrEqual(4)
    })

    it('retorna 0 para fechas futuras', () => {
      const futureDate = new Date()
      futureDate.setFullYear(futureDate.getFullYear() + 1)
      
      const age = calculateAgeInMonths(futureDate.toISOString())
      expect(age).toBe(0)
    })
  })

  describe('calculateBudgetBalance', () => {
    it('calcula el saldo disponible correctamente', () => {
      const budget = 400000
      const expenses = [
        { amount: 100000 },
        { amount: 50000 },
        { amount: 36450 }
      ]
      
      const balance = calculateBudgetBalance(budget, expenses)
      expect(balance).toBe(213550)
    })

    it('maneja lista vacía de gastos', () => {
      const budget = 400000
      const balance = calculateBudgetBalance(budget, [])
      expect(balance).toBe(400000)
    })

    it('retorna negativo cuando se excede el presupuesto', () => {
      const budget = 100000
      const expenses = [{ amount: 150000 }]
      const balance = calculateBudgetBalance(budget, expenses)
      expect(balance).toBe(-50000)
    })
  })

  describe('isLowStock', () => {
    it('detecta stock bajo cuando está en el mínimo', () => {
      expect(isLowStock(1, 1)).toBe(true)
    })

    it('detecta stock bajo cuando está por debajo del mínimo', () => {
      expect(isLowStock(0, 1)).toBe(true)
    })

    it('no detecta stock bajo cuando está sobre el mínimo', () => {
      expect(isLowStock(5, 1)).toBe(false)
    })
  })
})
