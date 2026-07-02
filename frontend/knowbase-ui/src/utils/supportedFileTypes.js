export const FILE_TYPE_OPTIONS = [
  { value: 'pdf', label: 'PDF' },
  { value: 'word', label: 'Word' },
  { value: 'txt', label: 'TXT' },
  { value: 'markdown', label: 'Markdown' },
  { value: 'excel', label: 'Excel' },
  { value: 'image', label: '图片' },
  { value: 'zip', label: 'ZIP' }
];

export const SYSTEM_SUPPORTED_FILE_TYPES = FILE_TYPE_OPTIONS.map((o) => o.value);

const EXT_MAP = {
  pdf: ['pdf'],
  word: ['doc', 'docx'],
  txt: ['txt'],
  markdown: ['md', 'markdown'],
  excel: ['xls', 'xlsx', 'csv'],
  image: ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'tiff', 'tif'],
  zip: ['zip']
};

export function fileExtension(name) {
  if (!name) return '';
  const base = name.includes('/') ? name.split('/').pop() : name;
  const dot = base.lastIndexOf('.');
  if (dot < 0) return '';
  return base.slice(dot + 1).toLowerCase();
}

export function matchesSupportedType(fileName, supportedTypes = []) {
  if (!supportedTypes.length) return true;
  const ext = fileExtension(fileName);
  if (!ext) return false;
  return supportedTypes.some((type) => (EXT_MAP[type] || [type]).includes(ext));
}

export function filterFolderFiles(fileList, supportedTypes = []) {
  const accepted = [];
  const skipped = [];
  for (const file of fileList) {
    const name = file.webkitRelativePath || file.name;
    if (matchesSupportedType(name, supportedTypes)) {
      accepted.push(file);
    } else {
      skipped.push(name);
    }
  }
  return { accepted, skipped };
}

export function supportedTypesLabel(supportedTypes = SYSTEM_SUPPORTED_FILE_TYPES) {
  if (!supportedTypes.length) return '—';
  return supportedTypes
    .map((t) => FILE_TYPE_OPTIONS.find((o) => o.value === t)?.label || t)
    .join('、');
}
