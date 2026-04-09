import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:5173'

// ---------------------------------------------------------------------------
// Tests that PASS — basic smoke tests
// ---------------------------------------------------------------------------

test('test_dashboard_loads', async ({ page }) => {
  await page.goto(BASE_URL)
  await page.waitForLoadState('networkidle')

  const cards = page.getByTestId('stats-cards')
  await expect(cards).toBeVisible()

  const cardItems = cards.locator('.card')
  await expect(cardItems).toHaveCount(3)
})

test('test_product_list', async ({ page }) => {
  await page.goto(`${BASE_URL}/produtos`)
  await page.waitForLoadState('networkidle')

  const table = page.getByTestId('products-table')
  await expect(table).toBeVisible()

  const rows = table.locator('tbody tr')
  await expect(rows.first()).toBeVisible()
})

test('test_navigation', async ({ page }) => {
  await page.goto(BASE_URL)
  await page.waitForLoadState('networkidle')

  await page.getByTestId('nav-produtos').click()
  await expect(page).toHaveURL(`${BASE_URL}/produtos`)

  await page.getByTestId('nav-pedidos').click()
  await expect(page).toHaveURL(`${BASE_URL}/pedidos`)

  await page.getByTestId('nav-clientes').click()
  await expect(page).toHaveURL(`${BASE_URL}/clientes`)

  await page.getByTestId('nav-dashboard').click()
  await expect(page).toHaveURL(`${BASE_URL}/`)
})

// ---------------------------------------------------------------------------
// Tests that FAIL — expose the 5 intentional bugs
// ---------------------------------------------------------------------------

test('test_table_sorting', async ({ page }) => {
  // Bug 1: Sort state updates but data order does not change
  await page.goto(`${BASE_URL}/produtos`)
  await page.waitForLoadState('networkidle')

  const table = page.getByTestId('products-table')

  // Get initial first-row ID value
  const firstCellBefore = await table.locator('tbody tr:first-child td:first-child').textContent()

  // Click the "ID" column header to sort ascending
  await table.locator('thead th:first-child').click()
  await page.waitForTimeout(300)

  // Click again to sort descending
  await table.locator('thead th:first-child').click()
  await page.waitForTimeout(300)

  const firstCellAfter = await table.locator('tbody tr:first-child td:first-child').textContent()

  // After sorting descending, the first cell should differ from the original order
  // This will FAIL because the data is never actually sorted
  expect(firstCellAfter).not.toBe(firstCellBefore)
})

test('test_mobile_layout', async ({ page }) => {
  // Bug 2: Sidebar has no responsive classes, overlaps content on mobile
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(BASE_URL)
  await page.waitForLoadState('networkidle')

  // Check that the page body does not overflow horizontally
  const bodyWidth = await page.evaluate(() => document.body.scrollWidth)
  const viewportWidth = 375

  // This will FAIL because ml-64 on the content pushes it off-screen
  expect(bodyWidth).toBeLessThanOrEqual(viewportWidth)
})

test('test_modal_closes_after_save', async ({ page }) => {
  // Bug 3: Modal stays open after successful save
  await page.goto(`${BASE_URL}/produtos`)
  await page.waitForLoadState('networkidle')

  // Open edit modal for the first product
  const editBtn = page.getByTestId('edit-product-btn').first()
  await editBtn.click()

  // Verify modal opened
  const modal = page.getByRole('dialog')
  await expect(modal).toBeVisible()

  // Click save
  const saveBtn = page.getByTestId('save-product-btn')
  await saveBtn.click()
  await page.waitForTimeout(800)

  // Modal should close after save — this will FAIL because setIsModalOpen(false) is missing
  await expect(modal).not.toBeVisible()
})

test('test_chart_renders', async ({ page }) => {
  // Bug 4: Chart dataKey props don't match the data fields, so bars/lines don't render
  await page.goto(BASE_URL)
  await page.waitForLoadState('networkidle')

  const chartContainer = page.getByTestId('sales-chart')
  await expect(chartContainer).toBeVisible()

  // Recharts renders SVG rectangles for bars and path elements for lines
  const bars = chartContainer.locator('svg .recharts-bar-rectangle')
  const lines = chartContainer.locator('svg .recharts-line-curve')

  // At least one bar should have a non-zero height
  const barCount = await bars.count()
  expect(barCount).toBeGreaterThan(0)

  const firstBarHeight = await bars.first().getAttribute('height')
  // This will FAIL because dataKey="total_vendas" doesn't match field "vendas" in chartData
  expect(Number(firstBarHeight)).toBeGreaterThan(0)
})

test('test_loading_states', async ({ page }) => {
  // Bug 5: Pages never show the LoadingSpinner during API calls
  await page.route('**/api/produtos**', async route => {
    // Delay the response to give the spinner time to appear
    await new Promise(r => setTimeout(r, 600))
    await route.continue()
  })

  await page.goto(`${BASE_URL}/produtos`)

  // Spinner should appear while the API is still in-flight
  // This will FAIL because no page imports or uses LoadingSpinner
  const spinner = page.locator('[role="status"][aria-label="Carregando"]')
  await expect(spinner).toBeVisible()
})
