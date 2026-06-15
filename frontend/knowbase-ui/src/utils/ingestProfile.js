/**
 * v2 采集级 ingest profile — 与 documentMetadata（语义标签）分离。
 * 持久化到 doc_metadata.ingest_profile_json；可覆盖分块数值，覆盖后进入非主档。
 */

const CHUNK_SIZE_MIN = 100
const CHUNK_SIZE_MAX = 8000
const CHUNK_OVERLAP_MAX = 2000

export function emptyIngestProfileForm() {
  return {
    enabled: false,
    chunkSize: null,
    chunkOverlap: null
  }
}

/** 是否允许本批使用非库默认的分块数值覆盖 */
export function canUseIngestProfileOverride(constraints) {
  if (constraints == null) return true
  return constraints.chunkOverrideAllowed !== false
}

/**
 * @param {ReturnType<typeof emptyIngestProfileForm>} form
 * @param {{ chunkSize?: number, chunkOverlap?: number }} libraryDefaults
 * @returns {string|null} JSON 或 null（无有效覆盖）
 */
export function buildIngestProfileJson(form, libraryDefaults = {}) {
  if (!form?.enabled) {
    return null
  }
  const libSize = libraryDefaults.chunkSize
  const libOverlap = libraryDefaults.chunkOverlap
  const profile = {}
  if (
    form.chunkSize != null &&
    form.chunkSize >= CHUNK_SIZE_MIN &&
    form.chunkSize <= CHUNK_SIZE_MAX &&
    form.chunkSize !== libSize
  ) {
    profile.chunkSize = form.chunkSize
  }
  if (
    form.chunkOverlap != null &&
    form.chunkOverlap >= 0 &&
    form.chunkOverlap <= CHUNK_OVERLAP_MAX &&
    form.chunkOverlap !== libOverlap
  ) {
    profile.chunkOverlap = form.chunkOverlap
  }
  if (!Object.keys(profile).length) return null
  return JSON.stringify(profile)
}

/**
 * @param {string|null|undefined} json
 * @returns {{ chunkSize?: number, chunkOverlap?: number }|null}
 */
export function parseIngestProfileJson(json) {
  if (!json?.trim()) return null
  try {
    const profile = JSON.parse(json)
    const out = {}
    if (typeof profile.chunkSize === 'number' && profile.chunkSize > 0) {
      out.chunkSize = profile.chunkSize
    }
    if (typeof profile.chunkOverlap === 'number' && profile.chunkOverlap >= 0) {
      out.chunkOverlap = profile.chunkOverlap
    }
    return Object.keys(out).length ? out : null
  } catch {
    return null
  }
}

/**
 * @param {{ chunkSize?: number, chunkOverlap?: number }|null} profile
 * @param {{ chunkSize?: number, chunkOverlap?: number }} libraryDefaults
 */
export function formatIngestProfileSummary(profile, libraryDefaults = {}) {
  if (!profile) return null
  if (profile.chunkSize == null && profile.chunkOverlap == null) return null
  const parts = []
  if (profile.chunkSize != null) {
    const lib = libraryDefaults.chunkSize
    parts.push(
      lib != null && profile.chunkSize !== lib
        ? `分块大小 ${lib} → ${profile.chunkSize}`
        : `分块大小 ${profile.chunkSize}`
    )
  }
  if (profile.chunkOverlap != null) {
    const lib = libraryDefaults.chunkOverlap
    parts.push(
      lib != null && profile.chunkOverlap !== lib
        ? `分块重叠 ${lib} → ${profile.chunkOverlap}`
        : `分块重叠 ${profile.chunkOverlap}`
    )
  }
  return parts.length ? parts.join(' · ') : null
}

/** @param {ReturnType<typeof emptyIngestProfileForm>} form */
export function validateIngestProfileForm(form) {
  if (!form?.enabled) return null
  if (form.chunkSize != null) {
    if (form.chunkSize < CHUNK_SIZE_MIN || form.chunkSize > CHUNK_SIZE_MAX) {
      return `分块大小须在 ${CHUNK_SIZE_MIN}–${CHUNK_SIZE_MAX} 之间`
    }
  }
  if (form.chunkOverlap != null) {
    if (form.chunkOverlap < 0 || form.chunkOverlap > CHUNK_OVERLAP_MAX) {
      return `分块重叠须在 0–${CHUNK_OVERLAP_MAX} 之间`
    }
  }
  if (form.chunkSize == null && form.chunkOverlap == null) {
    return '请至少填写一项与库默认不同的分块数值'
  }
  return null
}
