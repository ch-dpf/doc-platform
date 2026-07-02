import { ref } from 'vue';
import { getIngestionCatalog } from '../api';

const catalogRef = ref(null);
let loadingPromise = null;

export function useIngestionCatalog() {
  async function ensureCatalog() {
    if (catalogRef.value) {
      return catalogRef.value;
    }
    if (!loadingPromise) {
      loadingPromise = getIngestionCatalog()
        .then((data) => {
          catalogRef.value = data;
          return data;
        })
        .finally(() => {
          loadingPromise = null;
        });
    }
    return loadingPromise;
  }

  function parserLabel(code) {
    const item = catalogRef.value?.parsers?.find((p) => p.code === code);
    return item?.nameZh || code;
  }

  function profileLabel(code) {
    const item = catalogRef.value?.documentProfiles?.find((p) => p.code === code);
    return item?.nameZh || code;
  }

  function chunkingLabel(strategy) {
    const item = catalogRef.value?.documentProfiles?.find((p) => p.defaultChunkingStrategy === strategy);
    return item?.chunkingStrategyLabelZh || strategy;
  }

  function findParser(code) {
    return catalogRef.value?.parsers?.find((p) => p.code === code) || null;
  }

  function findProfileTemplate(code) {
    return catalogRef.value?.documentProfiles?.find((p) => p.code === code) || null;
  }

  return {
    catalog: catalogRef,
    ensureCatalog,
    parserLabel,
    profileLabel,
    chunkingLabel,
    findParser,
    findProfileTemplate
  };
}
