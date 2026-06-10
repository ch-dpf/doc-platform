const EXT_MAP = {
  pdf: ['pdf'],
  word: ['doc', 'docx'],
  txt: ['txt'],
  markdown: ['md', 'markdown'],
  excel: ['xls', 'xlsx']
}

export function fileExtension(name) {
  if (!name) return ''
  const base = name.includes('/') ? name.split('/').pop() : name
  const dot = base.lastIndexOf('.')
  if (dot < 0) return ''
  return base.slice(dot + 1).toLowerCase()
}

export function matchesSupportedType(fileName, supportedTypes = []) {
  if (!supportedTypes.length) return true
  const ext = fileExtension(fileName)
  if (!ext) return false
  return supportedTypes.some((type) => (EXT_MAP[type] || [type]).includes(ext))
}

export function filterFolderFiles(fileList, supportedTypes = []) {
  const accepted = []
  const skipped = []
  for (const file of fileList) {
    const name = file.webkitRelativePath || file.name
    if (matchesSupportedType(name, supportedTypes)) {
      accepted.push(file)
    } else {
      skipped.push(name)
    }
  }
  return { accepted, skipped }
}
