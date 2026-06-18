import axios from 'axios';
import { applyRequestHeaders } from './context';

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 120000
});

http.interceptors.request.use(applyRequestHeaders);

function unwrap(response) {
  if (!response.data?.success) {
    throw new Error(response.data?.message || '请求失败');
  }
  return response.data.data;
}

export async function listLibraries(params = {}) {
  return unwrap(await http.get('/libraries', { params }));
}

export async function createLibrary(payload) {
  return unwrap(await http.post('/libraries', payload));
}

export async function listLibraryTypePresets() {
  return unwrap(await http.get('/presets/library-types'));
}

export async function listSceneRulePresets() {
  return unwrap(await http.get('/presets/scene-rules'));
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
  return unwrap(await http.get(`/libraries/${libraryId}/documents`, { params }));
}

export async function getDocument(libraryId, documentId) {
  return unwrap(await http.get(`/libraries/${libraryId}/documents/${documentId}`));
}

export async function listDocumentChunks(libraryId, documentId) {
  return unwrap(await http.get(`/libraries/${libraryId}/documents/${documentId}/chunks`));
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

export async function uploadFile(file, bucket) {
  const formData = new FormData();
  formData.append('file', file);
  if (bucket) {
    formData.append('bucket', bucket);
  }
  return unwrap(await http.post('/storage/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
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
