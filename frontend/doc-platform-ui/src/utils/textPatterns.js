export function patternsToText(patterns) {
  return (patterns || []).join('\n')
}

export function textToPatterns(text) {
  return (text || '')
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}
