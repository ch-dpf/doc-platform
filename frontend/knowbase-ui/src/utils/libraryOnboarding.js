const DISMISS_KEY_PREFIX = 'kb-onboarding-dismissed:'
const CONFIG_SAVED_KEY_PREFIX = 'kb-onboarding-config-saved:'

export function isOnboardingDismissed(libraryId) {
  if (!libraryId) return false
  return sessionStorage.getItem(`${DISMISS_KEY_PREFIX}${libraryId}`) === '1'
}

export function dismissLibraryOnboarding(libraryId) {
  if (!libraryId) return
  sessionStorage.setItem(`${DISMISS_KEY_PREFIX}${libraryId}`, '1')
}

export function isOnboardingConfigSaved(libraryId) {
  if (!libraryId) return false
  return sessionStorage.getItem(`${CONFIG_SAVED_KEY_PREFIX}${libraryId}`) === '1'
}

export function markOnboardingConfigSaved(libraryId) {
  if (!libraryId) return
  sessionStorage.setItem(`${CONFIG_SAVED_KEY_PREFIX}${libraryId}`, '1')
}

export function clearOnboardingSession(libraryId) {
  if (!libraryId) return
  sessionStorage.removeItem(`${DISMISS_KEY_PREFIX}${libraryId}`)
  sessionStorage.removeItem(`${CONFIG_SAVED_KEY_PREFIX}${libraryId}`)
}

/** 快速创建后进入设置页 */
export function librarySettingsRoute(libraryId, { onboarding = false, tab = '' } = {}) {
  const query = {}
  if (onboarding) query.onboarding = '1'
  if (tab) query.tab = tab
  return {
    name: 'librarySettings',
    params: { libraryId },
    query: Object.keys(query).length ? query : undefined
  }
}

export function isOnboardingActive(route) {
  return route?.query?.onboarding === '1'
}

/** 新建库引导进行中（保存配置后即使离开设置页也保持，直至跳过） */
export function isOnboardingInProgress(route, libraryId) {
  if (!libraryId || isOnboardingDismissed(libraryId)) return false
  if (isOnboardingActive(route)) return true
  return isOnboardingConfigSaved(libraryId)
}
