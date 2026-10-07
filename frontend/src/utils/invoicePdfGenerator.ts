/**
 * NexusRigs invoice / receipt PDF generator
 * Mirrors the on-screen "Digital Tax Receipt" design:
 *  - High-fidelity html2canvas capture of #receipt-paper when available
 *  - Resilient vector jsPDF engine as reliable fallback
 *  - Guaranteed mathematically accurate totals (Subtotal - Voucher + Shipping + Estimated Tax)
 *  - Official NexusRigs logo & branding
 */
import { jsPDF } from 'jspdf'
import html2canvas from 'html2canvas'
import logoPng from '../assets/icons/logoIMG-removebg-preview PNG.png'
import logoUrl from '../assets/icons/logoIMG-removebg-preview.svg'

/* =========================================================
   TYPES (compatible with the checkout page)
========================================================= */

export interface InvoiceItem {
  productId?: string | number
  name?: string
  productName?: string
  quantity?: number
  unitPrice?: number
  price?: number
}

export interface InvoiceShippingInfo {
  firstName?: string
  lastName?: string
  email?: string
  phone?: string
  address?: string
  city?: string
  postalCode?: string
  country?: string
}

export interface InvoiceOrderData {
  orderId: string | number
  orderDate?: string
  customerId?: string | number
  items: InvoiceItem[]
  subtotal?: number
  shipping?: number
  tax?: number
  discount?: number
  dealDiscount?: number
  totalAmount?: number
  paymentMethod?: string
  paymentStatus?: string
  shippingInfo?: InvoiceShippingInfo
}

/* =========================================================
   DESIGN TOKENS (same palette as the Tailwind receipt)
========================================================= */

type RGB = [number, number, number]

const C = {
  slate900: [15, 23, 42] as RGB,
  slate800: [30, 41, 59] as RGB,
  slate700: [51, 65, 85] as RGB,
  slate600: [71, 85, 105] as RGB,
  slate500: [100, 116, 139] as RGB,
  slate400: [148, 163, 184] as RGB,
  slate200: [226, 232, 240] as RGB,
  slate100: [241, 245, 249] as RGB,
  slate50: [248, 250, 252] as RGB,
  blue600: [37, 99, 235] as RGB,
  indigo600: [79, 70, 229] as RGB,
  cyan500: [6, 182, 212] as RGB,
  cyan600: [8, 145, 178] as RGB,
  emerald500: [16, 185, 129] as RGB,
  emerald600: [5, 150, 105] as RGB,
  emerald700: [4, 120, 87] as RGB,
  emerald200: [167, 243, 208] as RGB,
  emerald50: [236, 253, 245] as RGB,
  rose600: [225, 29, 72] as RGB,
  white: [255, 255, 255] as RGB
}

const BRAND_GRADIENT: RGB[] = [C.blue600, C.indigo600, C.cyan500]

const PAGE_W = 210
const PAGE_H = 297
const M = 14
const CW = PAGE_W - M * 2
const RIGHT = PAGE_W - M

/* =========================================================
   SMALL HELPERS
========================================================= */

const fill = (d: jsPDF, c: RGB) => d.setFillColor(c[0], c[1], c[2])
const stroke = (d: jsPDF, c: RGB) => d.setDrawColor(c[0], c[1], c[2])
const ink = (d: jsPDF, c: RGB) => d.setTextColor(c[0], c[1], c[2])

/** Standard PDF fonts only support Latin-1, so strip anything else */
const clean = (v: unknown): string =>
  String(v ?? '')
    .replace(/[^\x00-\xFF]/g, '')
    .trim()

function money(n: unknown): string {
  const value = Number(n)
  const safe = Number.isFinite(value) ? value : 0
  return (
    'LKR ' +
    safe.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })
  )
}

function formatDate(iso?: string): string {
  const d = iso ? new Date(iso) : new Date()
  const date = Number.isNaN(d.getTime()) ? new Date() : d
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  })
}

function lerpColor(c1: RGB, c2: RGB, t: number): RGB {
  return [
    Math.round(c1[0] + (c2[0] - c1[0]) * t),
    Math.round(c1[1] + (c2[1] - c1[1]) * t),
    Math.round(c1[2] + (c2[2] - c1[2]) * t)
  ]
}

function multiGradientColor(stops: RGB[], t: number): RGB {
  const clamped = Math.max(0, Math.min(1, t))
  const segs = stops.length - 1
  if (segs <= 0) return stops[0] || [0, 0, 0]
  const scaled = clamped * segs
  const idx = Math.min(Math.floor(scaled), segs - 1)
  const localT = scaled - idx
  return lerpColor(stops[idx], stops[idx + 1], localT)
}

function gradientRect(
  d: jsPDF,
  x: number,
  y: number,
  w: number,
  h: number,
  stops: RGB[] = BRAND_GRADIENT,
  steps = 60
) {
  const stepW = w / steps
  for (let i = 0; i < steps; i++) {
    const col = multiGradientColor(stops, i / (steps - 1))
    fill(d, col)
    d.rect(x + i * stepW, y, stepW + 0.1, h, 'F')
  }
}

function gradientTextRight(
  d: jsPDF,
  text: string,
  rightX: number,
  y: number,
  stops: RGB[] = BRAND_GRADIENT
) {
  const chars = text.split('')
  const totalW = d.getTextWidth(text)
  let curX = rightX - totalW

  for (let i = 0; i < chars.length; i++) {
    const ch = chars[i]
    const chW = d.getTextWidth(ch)
    const t = chars.length > 1 ? i / (chars.length - 1) : 0
    const col = multiGradientColor(stops, t)
    ink(d, col)
    d.text(ch, curX, y)
    curX += chW
  }
}

function spaced(d: jsPDF, text: string, x: number, y: number, space = 0.4) {
  d.text(text, x, y, { charSpace: space } as any)
}

/* =========================================================
   LOGO LOADER (PNG first, fallback to SVG)
========================================================= */

interface LogoData {
  data: string
  ratio: number
}

let logoCache: LogoData | null | undefined

async function loadLogo(): Promise<LogoData | null> {
  if (logoCache !== undefined) return logoCache

  logoCache = await new Promise<LogoData | null>(resolve => {
    try {
      if (typeof Image === 'undefined' || typeof document === 'undefined') {
        resolve(null)
        return
      }

      const img = new Image()
      img.crossOrigin = 'anonymous'
      img.onload = () => {
        try {
          const nw = img.naturalWidth || 256
          const nh = img.naturalHeight || 256
          const scale = 512 / Math.max(nw, nh)
          const cw = Math.max(1, Math.round(nw * scale))
          const ch = Math.max(1, Math.round(nh * scale))
          const canvas = document.createElement('canvas')
          canvas.width = cw
          canvas.height = ch
          const ctx = canvas.getContext('2d')
          if (!ctx) {
            resolve(null)
            return
          }
          ctx.drawImage(img, 0, 0, cw, ch)
          resolve({ data: canvas.toDataURL('image/png'), ratio: cw / ch })
        } catch {
          resolve(null)
        }
      }

      img.onerror = () => {
        // Fallback to SVG if PNG fails
        const svgImg = new Image()
        svgImg.crossOrigin = 'anonymous'
        svgImg.onload = () => {
          try {
            const canvas = document.createElement('canvas')
            canvas.width = 300
            canvas.height = 300
            const ctx = canvas.getContext('2d')
            if (ctx) {
              ctx.drawImage(svgImg, 0, 0, 300, 300)
              resolve({ data: canvas.toDataURL('image/png'), ratio: 1 })
              return
            }
          } catch {}
          resolve(null)
        }
        svgImg.onerror = () => resolve(null)
        svgImg.src = logoUrl
      }

      img.src = logoPng
    } catch {
      resolve(null)
    }
  })

  return logoCache
}

function drawFallbackLogo(d: jsPDF, x: number, y: number, size: number) {
  fill(d, C.blue600)
  d.roundedRect(x, y, size, size, 3.5, 3.5, 'F')
  ink(d, C.white)
  d.setFont('helvetica', 'bold')
  d.setFontSize(10)
  d.text('NX', x + size / 2, y + size / 2 + 1.6, { align: 'center' })
}

/* =========================================================
   PURE VECTOR PDF GENERATOR (FALLBACK & STANDALONE)
========================================================= */

async function generateVectorInvoicePdf(order: InvoiceOrderData): Promise<Blob> {
  const doc = new jsPDF({
    unit: 'mm',
    format: 'a4',
    orientation: 'portrait',
    compress: true
  })

  doc.setProperties({
    title: `NexusRigs Receipt ${order.orderId}`,
    subject: 'Official Tax & Hardware Sales Receipt',
    author: 'NexusRigs'
  })

  const logo = await loadLogo()
  const ship = order.shippingInfo || {}
  const items = Array.isArray(order.items) ? order.items : []

  const startPage = () => {
    gradientRect(doc, 0, 0, PAGE_W, 3)
  }

  startPage()

  /* ---------------------------------------------------------
     HEADER
  --------------------------------------------------------- */

  let y = 15

  const logoH = 15
  const logoW = logo ? Math.min(24, logoH * logo.ratio) : logoH

  if (logo) {
    try {
      doc.addImage(logo.data, 'PNG', M, y, logoW, logoH)
    } catch {
      drawFallbackLogo(doc, M, y, logoH)
    }
  } else {
    drawFallbackLogo(doc, M, y, logoH)
  }

  const textX = M + logoW + 4

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(18)
  ink(doc, C.slate900)

  doc.text('NEXUSRIGS', textX, y + 8)

  doc.setFontSize(6.5)
  ink(doc, C.slate400)
  spaced(doc, 'PREMIUM COMPUTER & GAMING HUB', textX, y + 13, 0.3)

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(8.5)
  ink(doc, C.slate500)
  doc.text('Galle Road, Colombo 03, Sri Lanka  |  www.nexusrigs.com', M, y + 24)
  doc.text('Official Tax & Hardware Sales Receipt', M, y + 29)

  // Status pill
  const isPaid = String(order.paymentStatus || '').toLowerCase() === 'paid'
  const pillLabel = isPaid ? 'PAID & VERIFIED' : 'COD CONFIRMED'

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(7)
  const pillTextW = doc.getTextWidth(pillLabel) + pillLabel.length * 0.3
  const pillW = pillTextW + 14
  const pillH = 7
  const pillX = RIGHT - pillW

  fill(doc, C.emerald50)
  stroke(doc, C.emerald200)
  doc.setLineWidth(0.25)
  doc.roundedRect(pillX, y, pillW, pillH, 3.5, 3.5, 'FD')

  fill(doc, C.emerald500)
  doc.circle(pillX + 5.2, y + pillH / 2, 0.95, 'F')

  ink(doc, C.emerald700)
  spaced(doc, pillLabel, pillX + 8, y + 4.6, 0.3)

  // Receipt ref
  const refValue = `#${clean(order.orderId)}`
  doc.setFont('courier', 'bold')
  doc.setFontSize(9)
  const refW = doc.getTextWidth(refValue)
  ink(doc, C.slate900)
  doc.text(refValue, RIGHT - refW, y + 14)

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(8.5)
  ink(doc, C.slate400)
  const refLabel = 'Receipt Ref:'
  doc.text(refLabel, RIGHT - refW - 2.5 - doc.getTextWidth(refLabel), y + 14)

  // Date
  doc.setFont('helvetica', 'normal')
  doc.setFontSize(8.5)
  ink(doc, C.slate500)
  doc.text(`Date: ${formatDate(order.orderDate)}`, RIGHT, y + 20, { align: 'right' })

  // Divider
  y += 34
  stroke(doc, C.slate100)
  doc.setLineWidth(0.3)
  doc.line(M, y, RIGHT, y)

  /* ---------------------------------------------------------
     INFO PANEL (Billed to / Transaction specs)
  --------------------------------------------------------- */

  y += 6
  const boxTop = y
  const midX = M + CW / 2
  const leftX = M + 6
  const leftW = CW / 2 - 12

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(8.5)

  const fullName = clean(`${ship.firstName || ''} ${ship.lastName || ''}`) || 'Valued Customer'
  const addrLines: string[] = [
    ...(doc.splitTextToSize(clean(ship.address), leftW) as string[]),
    clean(`${ship.city || ''} ${ship.postalCode || ''}`),
    clean(ship.country || 'Sri Lanka')
  ].filter(Boolean)

  const contactLines: string[] = [
    ...(doc.splitTextToSize(`Phone: ${clean(ship.phone)}`, leftW) as string[]),
    ...(doc.splitTextToSize(`Email: ${clean(ship.email)}`, leftW) as string[])
  ]

  const leftHeight = 18.5 + addrLines.length * 4.6 + 2 + contactLines.length * 4.2 + 3
  const boxH = Math.max(leftHeight, 42)

  fill(doc, C.slate50)
  stroke(doc, C.slate200)
  doc.setLineWidth(0.25)
  doc.roundedRect(M, boxTop, CW, boxH, 4.5, 4.5, 'FD')

  // vertical divider
  stroke(doc, C.slate200)
  doc.line(midX, boxTop + 6, midX, boxTop + boxH - 6)

  // left column
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(6.5)
  ink(doc, C.blue600)
  spaced(doc, 'BILLED & DELIVERED TO:', leftX, boxTop + 7.5, 0.25)

  doc.setFontSize(11)
  ink(doc, C.slate900)
  doc.text(fullName, leftX, boxTop + 14)

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(8.5)
  ink(doc, C.slate600)
  let ly = boxTop + 19.5
  addrLines.forEach(line => {
    doc.text(line, leftX, ly)
    ly += 4.6
  })

  ly += 1.6
  ink(doc, C.slate500)
  contactLines.forEach(line => {
    doc.text(line, leftX, ly)
    ly += 4.2
  })

  // right column
  const rightX = midX + 7
  const rightEnd = RIGHT - 6

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(6.5)
  ink(doc, C.cyan600)
  spaced(doc, 'TRANSACTION SPECIFICATIONS:', rightX, boxTop + 7.5, 0.25)

  const specs: Array<[string, string, RGB]> = [
    ['Payment Channel:', clean(order.paymentMethod) || 'Card', C.slate900],
    ['Payment Status:', clean(order.paymentStatus) || (isPaid ? 'Paid' : 'Pending'), C.emerald600],
    ['Dispatch Courier:', 'Insured Island-wide Courier', C.slate900],
    ['Hardware Warranty:', '2-Year Official Guarantee', C.blue600]
  ]

  let ry = boxTop + 14
  specs.forEach(([label, value, color]) => {
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8.5)
    ink(doc, C.slate500)
    doc.text(label, rightX, ry)

    doc.setFont('helvetica', 'bold')
    ink(doc, color)
    doc.text(value, rightEnd, ry, { align: 'right' })
    ry += 6.4
  })

  y = boxTop + boxH + 9

  /* ---------------------------------------------------------
     ITEMS TABLE
  --------------------------------------------------------- */

  const colNo = M + 4
  const colName = M + 15
  const nameW = 86
  const colQty = RIGHT - 72
  const colUnit = RIGHT - 38
  const colTotal = RIGHT - 4

  const drawTableHeader = () => {
    fill(doc, C.slate50)
    stroke(doc, C.slate200)
    doc.setLineWidth(0.25)
    doc.rect(M, y, CW, 8, 'FD')

    doc.setFont('helvetica', 'bold')
    doc.setFontSize(6.5)
    ink(doc, C.slate500)

    doc.text('#', colNo, y + 5.2)
    spaced(doc, 'HARDWARE COMPONENT', colName, y + 5.2, 0.2)
    doc.text('QTY', colQty, y + 5.2, { align: 'center' })
    doc.text('UNIT PRICE', colUnit, y + 5.2, { align: 'right' })
    doc.text('TOTAL', colTotal, y + 5.2, { align: 'right' })

    y += 8
  }

  drawTableHeader()

  items.forEach((item, idx) => {
    const rawName = clean(item.name || item.productName || 'Hardware Component')
    const sku = clean(item.productId || `P00${idx + 1}`)
    const qty = Number(item.quantity) || 1
    const price = Number(item.unitPrice ?? item.price ?? 0)
    const lineTotal = price * qty

    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8.5)
    const nameLines = doc.splitTextToSize(rawName, nameW) as string[]

    const rowH = Math.max(12, nameLines.length * 4.4 + 6)

    // Check pagination room
    if (y + rowH > PAGE_H - 70) {
      doc.addPage()
      startPage()
      y = 15
      drawTableHeader()
    }

    // Index #
    doc.setFont('courier', 'normal')
    doc.setFontSize(8)
    ink(doc, C.slate400)
    doc.text(String(idx + 1), colNo, y + 5)

    // Name & SKU
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8.5)
    ink(doc, C.slate900)
    let ny = y + 5
    nameLines.forEach(line => {
      doc.text(line, colName, ny)
      ny += 4.4
    })

    doc.setFont('courier', 'normal')
    doc.setFontSize(7)
    ink(doc, C.slate400)
    doc.text(`SKU: ${sku}`, colName, ny)

    // Qty
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8.5)
    ink(doc, C.slate800)
    doc.text(String(qty), colQty, y + 5.2, { align: 'center' })

    // Unit price
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    ink(doc, C.slate600)
    doc.text(money(price), colUnit, y + 5.2, { align: 'right' })

    // Total
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8.5)
    ink(doc, C.slate900)
    doc.text(money(lineTotal), colTotal, y + 5.2, { align: 'right' })

    // Row divider
    stroke(doc, C.slate100)
    doc.setLineWidth(0.2)
    doc.line(M, y + rowH, RIGHT, y + rowH)

    y += rowH
  })

  /* ---------------------------------------------------------
     BOTTOM SUMMARY: WARRANTY & CORRECT FINANCIAL TOTALS
  --------------------------------------------------------- */

  y += 8
  if (y > PAGE_H - 65) {
    doc.addPage()
    startPage()
    y = 15
  }

  const sectionTop = y

  // Left column: Warranty certificate card + barcode
  const certW = 86
  fill(doc, C.slate50)
  stroke(doc, C.slate200)
  doc.setLineWidth(0.25)
  doc.roundedRect(M, sectionTop, certW, 23, 3, 3, 'FD')

  fill(doc, C.emerald500)
  doc.circle(M + 5, sectionTop + 5.5, 1.2, 'F')

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(7)
  ink(doc, C.slate700)
  spaced(doc, 'OFFICIAL WARRANTY CERTIFICATE', M + 8, sectionTop + 6.2, 0.25)

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(7)
  ink(doc, C.slate500)
  const certLines = doc.splitTextToSize(
    'All components included in this receipt are covered under our 2-Year Official NexusRigs hardware warranty and 14-day replacement policy.',
    certW - 8
  ) as string[]
  let cy = sectionTop + 11.5
  certLines.forEach(line => {
    doc.text(line, M + 4, cy)
    cy += 3.6
  })

  // Barcode
  const barY = sectionTop + 27
  const barH = 7
  const pattern = [1.5, 0.5, 1, 2, 0.5, 1.5, 0.5, 2.5, 1, 0.5, 2, 1.5, 0.5, 1, 2.5, 1.5, 0.5, 1.5, 2]
  let bx = M
  fill(doc, C.slate900)
  pattern.forEach(w => {
    doc.rect(bx, barY, w * 0.7, barH, 'F')
    bx += w * 0.7 + 0.55
  })

  doc.setFont('courier', 'normal')
  doc.setFontSize(7)
  ink(doc, C.slate400)
  spaced(doc, `AUTH-VERIFY-NR-${clean(order.orderId)}`, M, barY + barH + 4, 0.35)

  const leftBottom = barY + barH + 7

  // Right column: Financial breakdown with MATHEMATICALLY CORRECT TAX
  const subtotal = Number(order.subtotal || 0)
  const discount = Number(order.discount || 0)
  const shippingCost = Number(order.shipping || 0)
  // Tax is 8% of taxable subtotal
  const tax = Number(order.tax !== undefined && order.tax !== null ? order.tax : Math.round((subtotal - discount) * 0.08))
  
  // Mathematical Truth: Total = Subtotal - Voucher Discount + Shipping + Tax
  const computedTotal = Math.max(0, subtotal - discount + shippingCost + tax)
  const finalTotalAmount = (order.totalAmount && Math.abs(order.totalAmount - computedTotal) < 0.01)
    ? order.totalAmount
    : computedTotal

  const tx = RIGHT - 78
  const tEnd = RIGHT - 4
  let ty = sectionTop + 3.5

  const rows: Array<{ label: string; value: string; labelColor: RGB; valueColor: RGB }> = [
    {
      label: 'Subtotal:',
      value: money(subtotal),
      labelColor: C.slate600,
      valueColor: C.slate800
    }
  ]

  if (discount > 0) {
    rows.push({
      label: 'Voucher Discount:',
      value: `- ${money(discount)}`,
      labelColor: C.emerald600,
      valueColor: C.emerald600
    })
  }

  rows.push({
    label: 'Insured Courier Shipping:',
    value: shippingCost === 0 ? 'FREE' : money(shippingCost),
    labelColor: C.slate600,
    valueColor: shippingCost === 0 ? C.emerald600 : C.slate800
  })

  rows.push({
    label: 'Estimated Tax & VAT (8%):',
    value: money(tax),
    labelColor: C.slate600,
    valueColor: C.slate800
  })

  rows.forEach(row => {
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8.5)
    ink(doc, row.labelColor)
    doc.text(row.label, tx, ty)

    doc.setFont('helvetica', 'bold')
    ink(doc, row.valueColor)
    doc.text(row.value, tEnd, ty, { align: 'right' })
    ty += 6.2
  })

  ty += 0.5
  stroke(doc, C.slate200)
  doc.setLineWidth(0.3)
  doc.line(tx, ty, tEnd, ty)
  ty += 6

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(6.5)
  ink(doc, C.slate400)
  doc.text('TOTAL AMOUNT PAID', tx, ty)
  doc.setFont('helvetica', 'normal')
  doc.text('All duties included', tx, ty + 3.8)

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(16)
  gradientTextRight(doc, money(finalTotalAmount), tEnd, ty + 4.6)

  const totalsBottom = ty + 8

  /* ---------------------------------------------------------
     FOOTER
  --------------------------------------------------------- */

  y = Math.max(leftBottom, totalsBottom) + 10
  stroke(doc, C.slate100)
  doc.setLineWidth(0.3)
  doc.line(M, y, RIGHT, y)

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(7)
  ink(doc, C.slate400)
  doc.text(
    'NexusRigs Sri Lanka  |  Customer Support: support@nexusrigs.com  |  Hotline: +94 11 234 5678',
    PAGE_W / 2,
    y + 6,
    { align: 'center' }
  )
  doc.text('Thank you for shopping with NexusRigs!', PAGE_W / 2, y + 10.5, { align: 'center' })

  const pages = doc.getNumberOfPages()
  if (pages > 1) {
    for (let i = 1; i <= pages; i++) {
      doc.setPage(i)
      doc.setFontSize(7)
      ink(doc, C.slate400)
      doc.text(`Page ${i} of ${pages}`, RIGHT, PAGE_H - 8, { align: 'right' })
    }
  }

  return doc.output('blob')
}

/* =========================================================
   PUBLIC HIGH-RESOLUTION HTML / CANVAS PDF GENERATOR
========================================================= */

export async function generateInvoicePdf(
  order: InvoiceOrderData,
  element?: HTMLElement | null
): Promise<Blob> {
  if (!order || !order.orderId) {
    throw new Error('Order data is missing or incomplete.')
  }

  // 1. Primary Strategy: Capture live #receipt-paper DOM card using html2canvas
  // This guarantees 100% exact visual match with the user's design on screen!
  const targetEl =
    element ||
    (typeof document !== 'undefined' ? document.getElementById('receipt-paper') : null)

  if (targetEl && typeof window !== 'undefined') {
    try {
      // Ensure all images in target element are loaded
      const images = Array.from(targetEl.querySelectorAll('img'))
      await Promise.all(
        images.map(img => {
          if (img.complete) return Promise.resolve(true)
          return new Promise(res => {
            img.onload = () => res(true)
            img.onerror = () => res(true)
          })
        })
      )

      const canvas = await html2canvas(targetEl, {
        scale: 2, // 2x high resolution
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff'
      })

      const doc = new jsPDF({
        unit: 'mm',
        format: 'a4',
        orientation: 'portrait',
        compress: true
      })

      const pageWidth = 210
      const pageHeight = 297
      const margin = 10
      const printWidth = pageWidth - margin * 2 // 190mm
      const printHeight = (canvas.height * printWidth) / canvas.width

      // Center vertically if it fits on page comfortably
      const startY =
        printHeight < pageHeight - margin * 2
          ? Math.max(margin, (pageHeight - printHeight) / 2)
          : margin

      doc.addImage(
        canvas.toDataURL('image/png'),
        'PNG',
        margin,
        startY,
        printWidth,
        printHeight
      )

      return doc.output('blob')
    } catch (captureErr) {
      console.warn('html2canvas capture notice, using vector generator fallback:', captureErr)
    }
  }

  // 2. Secondary Strategy: High-fidelity vector generator
  return generateVectorInvoicePdf(order)
}