import axios from 'axios';
import { applyRequestHeaders } from './context';

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 120000
});

http.interceptors.request.use(applyRequestHeaders);

http.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

function unwrap(response) {
  if (!response.data?.success) {
    throw new Error(response.data?.message || '请求失败');
  }
  return response.data.data;
}

export async function pageLibraries(params = {}) {
  return unwrap(await http.get('/libraries', { params }));
}

export async function listLibraries(params = {}) {
  const data = await pageLibraries({ page: 1, size: 200, ...params });
  return data.items ?? [];
}

export async function deleteLibrary(libraryId) {
  return unwrap(await http.delete(`/libraries/${libraryId}`));
}

export async function createLibrary(payload) {
  return unwrap(await http.post('/libraries', payload));
}

export async function getLibrary(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}`));
}

export async function pageLibraryTypePresets(params = {}) {
  return unwrap(await http.get('/presets/library-types', { params }));
}

export async function listLibraryTypePresets(params = {}) {
  const data = await pageLibraryTypePresets({ page: 1, size: 200, ...params });
  return data.items ?? [];
}

export async function getLibraryTypePreset(code, params = {}) {
  return unwrap(await http.get(`/presets/library-types/${code}`, { params }));
}

export async function getIngestionCatalog() {
  return unwrap(await http.get('/presets/ingestion-catalog'));
}

export async function getLibraryTypePresetGuide(code, params = {}) {
  return unwrap(await http.get(`/presets/library-types/${code}/product-guide`, { params }));
}

export async function createLibraryTypePreset(payload) {
  return unwrap(await http.post('/presets/library-types', payload));
}

export async function deleteLibraryTypePreset(code, tenantId) {
  return unwrap(await http.delete(`/presets/library-types/${code}`, { params: { tenantId } }));
}

export async function pageSceneRulePresets(params = {}) {
  return unwrap(await http.get('/presets/scene-rules', { params }));
}

export async function listSceneRulePresets(params = {}) {
  const data = await pageSceneRulePresets({ page: 1, size: 200, ...params });
  return data.items ?? [];
}

export async function getSceneRulePreset(code, params = {}) {
  return unwrap(await http.get(`/presets/scene-rules/${code}`, { params }));
}

export async function createSceneRulePreset(payload) {
  return unwrap(await http.post('/presets/scene-rules', payload));
}

export async function deleteSceneRulePreset(code, tenantId) {
  return unwrap(await http.delete(`/presets/scene-rules/${code}`, { params: { tenantId } }));
}

export async function listTokenizerProfiles(params = {}) {
  return unwrap(await http.get('/tokenizer-profiles', { params }));
}

export async function listIndexVersions(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/index-versions`));
}

export async function getIndexVersion(libraryId, indexVersionId) {
  return unwrap(await http.get(`/libraries/${libraryId}/index-versions/${indexVersionId}`));
}

export async function listDocuments(libraryId, params = {}) {
  const data = await pageDocuments(libraryId, params);
  return data.items ?? [];
}

export async function pageDocuments(libraryId, params = {}) {
  return unwrap(await http.get(`/libraries/${libraryId}/documents`, {
    params: { page: 1, size: 20, ...params }
  }));
}

export async function getDocument(libraryId, documentId) {
  return unwrap(await http.get(`/libraries/${libraryId}/documents/${documentId}`));
}

export async function listDocumentChunks(libraryId, documentId, limit = 8) {
  const data = await pageDocumentChunks(libraryId, documentId, { page: 1, size: limit });
  return data.items ?? [];
}

export async function pageDocumentChunks(libraryId, documentId, params = {}) {
  return unwrap(await http.get(`/libraries/${libraryId}/documents/${documentId}/chunks`, {
    params: { page: 1, size: 20, ...params }
  }));
}

export async function getDocumentPipelineTrace(libraryId, documentId) {
  return unwrap(await http.get(`/libraries/${libraryId}/documents/${documentId}/pipeline-trace`));
}

function parseContentDisposition(header) {
  if (!header) {
    return 'document';
  }
  const utf8Match = header.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }
  const plainMatch = header.match(/filename="([^"]+)"/i);
  return plainMatch ? plainMatch[1] : 'document';
}

async function parseBlobError(blob, fallback = '预览失败') {
  try {
    const text = await blob.text();
    const json = JSON.parse(text);
    return json.message || fallback;
  } catch {
    return fallback;
  }
}

export async function fetchDocumentPreview(libraryId, documentId) {
  try {
    const response = await http.get(`/libraries/${libraryId}/documents/${documentId}/preview`, {
      responseType: 'blob'
    });
    const contentType = response.headers['content-type'] || 'application/octet-stream';
    const filename = parseContentDisposition(response.headers['content-disposition']);
    return { blob: response.data, contentType, filename };
  } catch (error) {
    if (error.response?.data instanceof Blob) {
      throw new Error(await parseBlobError(error.response.data));
    }
    throw error;
  }
}

export async function updateDocumentChunk(libraryId, documentId, chunkId, payload) {
  return unwrap(await http.put(`/libraries/${libraryId}/documents/${documentId}/chunks/${chunkId}`, payload));
}

export async function deleteDocument(libraryId, documentId) {
  return unwrap(await http.delete(`/libraries/${libraryId}/documents/${documentId}`));
}

export async function batchDeleteDocuments(libraryId, documentIds) {
  return unwrap(await http.post(`/libraries/${libraryId}/documents/batch-delete`, { documentIds }));
}

export async function reindexDocument(libraryId, documentId) {
  return unwrap(await http.post(`/libraries/${libraryId}/documents/${documentId}/reindex`));
}

export async function uploadDocuments(libraryId, files, options = {}) {
  const formData = new FormData();
  for (const file of files) {
    formData.append('files', file, file.name);
  }
  if (options.documentProfileCode) {
    formData.append('documentProfileCode', options.documentProfileCode);
  }
  formData.append('publishIndexOnSuccess', String(options.publishIndexOnSuccess !== false));
  formData.append('autoStart', String(options.autoStart !== false));
  return unwrap(await http.post(`/libraries/${libraryId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  }));
}

export async function rebuildIndexGeneration(libraryId, autoPromote = false) {
  return unwrap(await http.post(`/libraries/${libraryId}/index-generations/rebuild`, null, {
    params: { autoPromote }
  }));
}

export async function promoteIndexGeneration(libraryId, indexGenerationId, force = false) {
  return unwrap(await http.post(`/libraries/${libraryId}/index-generations/${indexGenerationId}/promote`, null, {
    params: { force }
  }));
}

export async function getIndexHealth(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/index-health`));
}

export async function getPromoteReadiness(libraryId, indexGenerationId) {
  return unwrap(await http.get(`/libraries/${libraryId}/index-generations/${indexGenerationId}/promote-readiness`));
}

export async function getPromoteEvalGate(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/index-generations/promote-eval-gate`));
}

export async function getLibraryProfile(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/profile`));
}

export async function listLibraryProfileVersions(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/profiles`));
}

export async function createLibraryProfileVersion(libraryId, payload) {
  return unwrap(await http.post(`/libraries/${libraryId}/profiles`, payload));
}

export async function listDocumentProfiles(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/document-profiles`));
}

export async function createDocumentProfile(libraryId, payload) {
  return unwrap(await http.post(`/libraries/${libraryId}/document-profiles`, payload));
}

export async function updateDocumentProfile(libraryId, code, payload) {
  return unwrap(await http.put(`/libraries/${libraryId}/document-profiles/${encodeURIComponent(code)}`, payload));
}

export async function deleteDocumentProfile(libraryId, code) {
  return unwrap(await http.delete(`/libraries/${libraryId}/document-profiles/${encodeURIComponent(code)}`));
}

export async function importRetrievalEvalSamples(libraryId, payload) {
  return unwrap(await http.post(`/libraries/${libraryId}/retrieval-eval-samples/import`, payload));
}

export async function bootstrapRetrievalEvalSamples(libraryId, replaceExisting = false) {
  return unwrap(await http.post(
    `/libraries/${libraryId}/retrieval-eval-samples/bootstrap-sample-documents`,
    null,
    { params: { replaceExisting } }
  ));
}

export async function generateRetrievalEvalDrafts(libraryId, payload = {}) {
  return unwrap(await http.post(`/libraries/${libraryId}/retrieval-eval-samples/generate-drafts`, payload));
}

export async function getRetrievalEvalBaseline(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/retrieval-eval-baseline`));
}

export async function pinRetrievalEvalBaseline(libraryId, evalRunId) {
  return unwrap(await http.post(`/libraries/${libraryId}/retrieval-evaluations/${evalRunId}/baseline`));
}

export async function runLibraryRetrievalTest(libraryId, payload) {
  return unwrap(await http.post(`/libraries/${libraryId}/retrieval-tests`, payload));
}

export async function listRetrievalEvalSamples(libraryId, enabledOnly = false) {
  return unwrap(await http.get(`/libraries/${libraryId}/retrieval-eval-samples`, {
    params: { enabledOnly }
  }));
}

export async function createRetrievalEvalSample(libraryId, payload) {
  return unwrap(await http.post(`/libraries/${libraryId}/retrieval-eval-samples`, payload));
}

export async function updateRetrievalEvalSample(libraryId, sampleId, payload) {
  return unwrap(await http.put(`/libraries/${libraryId}/retrieval-eval-samples/${sampleId}`, payload));
}

export async function deleteRetrievalEvalSample(libraryId, sampleId) {
  return unwrap(await http.delete(`/libraries/${libraryId}/retrieval-eval-samples/${sampleId}`));
}

export async function runRetrievalEvaluation(libraryId, payload = {}) {
  return unwrap(await http.post(`/libraries/${libraryId}/retrieval-evaluations`, payload));
}

export async function listRetrievalEvaluations(libraryId, limit = 20) {
  return unwrap(await http.get(`/libraries/${libraryId}/retrieval-evaluations`, { params: { limit } }));
}

export async function getRetrievalEvaluation(libraryId, evalRunId) {
  return unwrap(await http.get(`/libraries/${libraryId}/retrieval-evaluations/${evalRunId}`));
}

export async function listLibraryIngestionRuns(libraryId, limit = 50) {
  return unwrap(await http.get(`/libraries/${libraryId}/ingestion-runs`, { params: { limit } }));
}

export async function listLibraryIngestionErrors(libraryId, runId) {
  return unwrap(await http.get(`/libraries/${libraryId}/ingestion-runs/${runId}/errors`));
}

export async function listLibraryIngestionJobs(libraryId, runId) {
  return unwrap(await http.get(`/libraries/${libraryId}/ingestion-runs/${runId}/jobs`));
}

export async function reindexFailedDocuments(libraryId) {
  return unwrap(await http.post(`/libraries/${libraryId}/documents/reindex-failed`));
}

export async function reindexByDocumentProfile(libraryId, documentProfileCode) {
  return unwrap(await http.post(`/libraries/${libraryId}/documents/reindex-by-profile`, null, {
    params: { documentProfileCode }
  }));
}

export async function listDocumentDuplicates(libraryId) {
  return unwrap(await http.get(`/libraries/${libraryId}/documents/duplicates`));
}

export async function listAcls(params) {
  return unwrap(await http.get('/acls', { params }));
}

export async function grantAcl(payload) {
  return unwrap(await http.post('/acls', payload));
}

export async function revokeAcl(aclId) {
  return unwrap(await http.delete(`/acls/${aclId}`));
}

export async function createIngestionRun(libraryId, payload) {
  return unwrap(await http.post(`/libraries/${libraryId}/ingestion-runs`, payload));
}

export async function getIngestionRun(runId) {
  return unwrap(await http.get(`/ingestion-runs/${runId}`));
}

export async function listIngestionErrors(runId) {
  return unwrap(await http.get(`/ingestion-runs/${runId}/errors`));
}

export async function uploadFile(file) {
  const formData = new FormData();
  formData.append('file', file);
  return unwrap(await http.post('/storage/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }));
}

export async function uploadFiles(files) {
  const formData = new FormData();
  for (const file of files) {
    formData.append('files', file, file.webkitRelativePath || file.name);
  }
  return unwrap(await http.post('/storage/upload-batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }));
}

export async function previewIngestion(libraryId, payload) {
  return unwrap(await http.post(`/libraries/${libraryId}/ingestion/preview`, payload));
}

export async function prepareIngestion(libraryId, payload, stage = 'all') {
  const suffix = stage === 'all' ? 'prepare' : `prepare/${stage}`;
  return unwrap(await http.post(`/libraries/${libraryId}/ingestion/${suffix}`, payload));
}

export async function uploadAndIngest(libraryId, files, options = {}) {
  const formData = new FormData();
  for (const file of files) {
    formData.append('files', file, file.webkitRelativePath || file.name);
  }
  if (options.documentProfileCode) {
    formData.append('documentProfileCode', options.documentProfileCode);
  }
  formData.append('publishIndexOnSuccess', String(options.publishIndexOnSuccess !== false));
  formData.append('autoStart', String(options.autoStart !== false));
  if (options.maxFiles) {
    formData.append('maxFiles', String(options.maxFiles));
  }
  return unwrap(await http.post(`/libraries/${libraryId}/ingestion-runs/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  }));
}

export async function listAgents(params = {}) {
  return unwrap(await http.get('/agents', { params }));
}

export async function createAgent(payload) {
  return unwrap(await http.post('/agents', payload));
}

export async function listAgentVersions(agentId) {
  return unwrap(await http.get(`/agents/${agentId}/versions`));
}

export async function getAgentVersion(agentId, agentVersionId) {
  return unwrap(await http.get(`/agents/${agentId}/versions/${agentVersionId}`));
}

export async function createAgentVersion(agentId, payload) {
  return unwrap(await http.post(`/agents/${agentId}/versions`, payload));
}

export async function publishAgentVersion(agentId, agentVersionId) {
  return unwrap(await http.post(`/agents/${agentId}/versions/${agentVersionId}/publish`));
}

export async function disableAgentVersion(agentId, agentVersionId) {
  return unwrap(await http.post(`/agents/${agentId}/versions/${agentVersionId}/disable`));
}

export async function runRetrievalTest(agentId, payload) {
  return unwrap(await http.post(`/agents/${agentId}/retrieval-tests`, payload));
}

export async function askQuestion(payload) {
  return unwrap(await http.post('/query-runs', payload));
}

export async function listPipelineTrace(traceId) {
  return unwrap(await http.get(`/observability/traces/${traceId}`));
}

export async function listPipelineRun(pipeline, runId) {
  return unwrap(await http.get(`/observability/pipelines/${pipeline}/runs/${runId}`));
}

export async function createEvalRun(payload) {
  return unwrap(await http.post('/observability/eval-runs', payload));
}

export async function listEvalRuns(params = {}) {
  return unwrap(await http.get('/observability/eval-runs', { params }));
}

export async function getEvalRun(evalRunId) {
  return unwrap(await http.get(`/observability/eval-runs/${evalRunId}`));
}
