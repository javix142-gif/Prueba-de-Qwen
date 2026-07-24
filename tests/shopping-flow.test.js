import { describe, it, expect } from 'vitest'

/**
 * Test del flujo de compras para la aplicación Primer Año
 * Verifica la lógica de: compra -> gasto -> actualización de stock
 */

describe('Flujo de compras', () => {
  // Estado inicial del inventario
  const initialInventory = [
    { id: '1', name: 'Pañales', currentStock: 10, minimumStock: 5 },
    { id: '2', name: 'Fórmula', currentStock: 3, minimumStock: 2 },
    { id: '3', name: 'Toallitas', currentStock: 8, minimumStock: 4 }
  ]

  // Lista de compras inicial
  const initialShoppingList = [
    { 
      id: 's1', 
      productId: '1', 
      description: 'Pañales talla M', 
      quantity: 2, 
      status: 'pendiente' 
    },
    { 
      id: 's2', 
      productId: '2', 
      description: 'Fórmula etapa 1', 
      quantity: 1, 
      status: 'pendiente' 
    }
  ]

  // Simulación de marcar compra como completada
  const markShoppingItemAsPurchased = (shoppingItem, purchasedAmount) => {
    return {
      ...shoppingItem,
      status: 'comprado',
      purchasedAmount: purchasedAmount,
      purchasedAt: new Date().toISOString()
    }
  }

  // Crear gasto desde una compra
  const createExpenseFromPurchase = (shoppingItem, amount, paidBy) => {
    if (shoppingItem.status !== 'comprado') {
      throw new Error('Solo se pueden crear gastos desde compras completadas')
    }
    
    return {
      expenseDate: new Date().toISOString().split('T')[0],
      description: shoppingItem.description,
      category: 'panales',
      amount: amount,
      paidBy: paidBy,
      isPlanned: true
    }
  }

  // Actualizar stock después de una compra
  const updateStockAfterPurchase = (inventory, shoppingItem, quantity) => {
    return inventory.map(product => {
      if (product.id === shoppingItem.productId) {
        return {
          ...product,
          currentStock: product.currentStock + quantity
        }
      }
      return product
    })
  }

  // Verificar si un producto tiene stock bajo
  const getLowStockProducts = (inventory) => {
    return inventory.filter(product => product.currentStock <= product.minimumStock)
  }

  describe('markShoppingItemAsPurchased', () => {
    it('marca correctamente un ítem como comprado', () => {
      const item = initialShoppingList[0]
      const updated = markShoppingItemAsPurchased(item, 5000)
      
      expect(updated.status).toBe('comprado')
      expect(updated.purchasedAmount).toBe(5000)
      expect(updated.purchasedAt).toBeDefined()
    })
  })

  describe('createExpenseFromPurchase', () => {
    it('crea un gasto válido desde una compra completada', () => {
      const purchasedItem = markShoppingItemAsPurchased(initialShoppingList[0], 5000)
      const expense = createExpenseFromPurchase(purchasedItem, 5000, 'javier')
      
      expect(expense.description).toBe('Pañales talla M')
      expect(expense.amount).toBe(5000)
      expect(expense.paidBy).toBe('javier')
    })

    it('lanza error si el ítem no está marcado como comprado', () => {
      const pendingItem = initialShoppingList[0]
      
      expect(() => createExpenseFromPurchase(pendingItem, 5000, 'javier'))
        .toThrow('Solo se pueden crear gastos desde compras completadas')
    })
  })

  describe('updateStockAfterPurchase', () => {
    it('incrementa el stock correctamente después de una compra', () => {
      const purchasedItem = markShoppingItemAsPurchased(initialShoppingList[0], 5000)
      const updatedInventory = updateStockAfterPurchase(
        initialInventory, 
        purchasedItem, 
        2 // cantidad comprada
      )
      
      const pañales = updatedInventory.find(p => p.id === '1')
      expect(pañales.currentStock).toBe(12) // 10 + 2
    })

    it('no modifica otros productos del inventario', () => {
      const purchasedItem = markShoppingItemAsPurchased(initialShoppingList[0], 5000)
      const updatedInventory = updateStockAfterPurchase(
        initialInventory, 
        purchasedItem, 
        2
      )
      
      const formula = updatedInventory.find(p => p.id === '2')
      expect(formula.currentStock).toBe(3) // sin cambios
    })
  })

  describe('getLowStockProducts', () => {
    it('identifica productos con stock bajo', () => {
      const lowStockInventory = [
        { id: '1', name: 'Pañales', currentStock: 3, minimumStock: 5 },
        { id: '2', name: 'Fórmula', currentStock: 10, minimumStock: 2 }
      ]
      
      const lowStock = getLowStockProducts(lowStockInventory)
      expect(lowStock.length).toBe(1)
      expect(lowStock[0].name).toBe('Pañales')
    })

    it('retorna lista vacía si no hay stock bajo', () => {
      const healthyInventory = [
        { id: '1', name: 'Pañales', currentStock: 10, minimumStock: 5 },
        { id: '2', name: 'Fórmula', currentStock: 10, minimumStock: 2 }
      ]
      
      const lowStock = getLowStockProducts(healthyInventory)
      expect(lowStock.length).toBe(0)
    })
  })

  describe('Flujo completo: compra -> gasto -> stock', () => {
    it('ejecuta correctamente todo el flujo de compra', () => {
      // 1. Ítem pendiente en lista de compras
      const shoppingItem = initialShoppingList[0]
      expect(shoppingItem.status).toBe('pendiente')
      
      // 2. Marcar como comprado con monto pagado
      const purchasedItem = markShoppingItemAsPurchased(shoppingItem, 5000)
      expect(purchasedItem.status).toBe('comprado')
      
      // 3. Crear gasto asociado
      const expense = createExpenseFromPurchase(purchasedItem, 5000, 'josefina')
      expect(expense.amount).toBe(5000)
      expect(expense.paidBy).toBe('josefina')
      
      // 4. Actualizar inventario
      const updatedInventory = updateStockAfterPurchase(
        initialInventory, 
        purchasedItem, 
        shoppingItem.quantity
      )
      
      const pañales = updatedInventory.find(p => p.id === '1')
      expect(pañales.currentStock).toBe(12) // 10 + 2
      
      // 5. Verificar que ya no está en stock bajo (si lo estaba)
      const lowStock = getLowStockProducts(updatedInventory)
      expect(lowStock.some(p => p.id === '1')).toBe(false)
    })
  })
})
