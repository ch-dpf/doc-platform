export const DEFAULT_LINE_DROP_PATTERNS = [
  '^\\d{1,4}$',
  '^第\\s*\\d+\\s*页$',
  '^Page\\s+\\d+\\s+of\\s+\\d+$',
  '^-{3,}$',
  '^_{3,}$'
]

export function patternsToText(patterns) {
  return (patterns || []).join('\n')
}

export function textToPatterns(text) {
  return (text || '')
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}
