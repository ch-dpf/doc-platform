import { ref } from 'vue'

const pageTitleOverride = ref(null)

export function usePageTitle() {
  function setPageTitle(title) {
    pageTitleOverride.value = title
  }

  function clearPageTitle() {
    pageTitleOverride.value = null
  }

  return {
    pageTitleOverride,
    setPageTitle,
    clearPageTitle
  }
}
