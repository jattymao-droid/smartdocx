/** Lightweight entry — heavy libs loaded only when exporting. */
export async function exportPaperClient(vm) {
  if (typeof vm.ensureExportDetails === 'function') {
    await vm.ensureExportDetails()
  }
  if (typeof vm.ensureTeacherDetails === 'function') {
    await vm.ensureTeacherDetails()
  }
  if (vm.exportFormat === 'docx') {
    const mod = await import(/* webpackChunkName: "paper-export-docx-v3" */ '@/utils/paperExportDocx')
    await mod.exportPaperDocxClient(vm)
    return
  }
  const mod = await import(/* webpackChunkName: "paper-export-pdf" */ '@/utils/paperExportPdf')
  await mod.exportPaperPdfClient(vm)
}
