import html2pdf from 'html2pdf.js'
import { buildPaperExportElement, exportPaperFilename, iterPaperQuestions, waitForImages } from '@/utils/paperExportCommon'

function waitForLayout() {
  return new Promise(resolve => {
    requestAnimationFrame(() => requestAnimationFrame(resolve))
  })
}

export async function exportPaperPdfClient(vm) {
  const { root, pageSize, cleanup } = buildPaperExportElement(vm)
  root.style.position = 'fixed'
  root.style.left = '0'
  root.style.top = '0'
  root.style.width = '794px'
  root.style.maxWidth = '794px'
  root.style.zIndex = '99999'
  root.style.pointerEvents = 'none'
  root.style.background = '#fff'
  document.body.appendChild(root)
  try {
    await waitForLayout()
    await waitForImages(root)
    if (document.fonts && document.fonts.ready) {
      await document.fonts.ready
    }
    const questionCount = iterPaperQuestions(vm).length
    const text = (root.textContent || '').replace(/\s+/g, ' ').trim()
    if (!text || (questionCount > 0 && text.length < 8)) {
      throw new Error('export content empty')
    }
    const filename = exportPaperFilename(vm, 'pdf')
    await html2pdf().set({
      margin: [12, 12, 12, 12],
      filename,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: {
        scale: 2,
        useCORS: true,
        allowTaint: true,
        logging: false,
        backgroundColor: '#ffffff',
        scrollX: 0,
        scrollY: -window.scrollY,
        windowWidth: root.scrollWidth || 794,
        width: root.scrollWidth || 794
      },
      jsPDF: { unit: 'mm', format: pageSize === 'A3' ? 'a3' : 'a4', orientation: 'portrait' },
      pagebreak: { mode: ['css', 'legacy'] }
    }).from(root).save()
  } finally {
    if (root.parentNode) root.parentNode.removeChild(root)
    if (cleanup) cleanup()
  }
}
