import { ref, computed } from 'vue'

export const DEFAULT_LIBRARY_ID = '00000000-0000-0000-0000-000000000001'

const libraryId = ref(localStorage.getItem('libraryId') || DEFAULT_LIBRARY_ID)
const tenantId = ref(localStorage.getItem('tenantId') || 'demo')

export function useLibraryContext() {
  function persist() {
    localStorage.setItem('libraryId', libraryId.value)
    localStorage.setItem('tenantId', tenantId.value)
  }

  const libraryIdModel = computed({
    get: () => libraryId.value,
    set: (v) => {
      libraryId.value = v
      persist()
    }
  })

  const tenantIdModel = computed({
    get: () => tenantId.value,
    set: (v) => {
      tenantId.value = v
      persist()
    }
  })

  return { libraryId: libraryIdModel, tenantId: tenantIdModel, persist }
}
