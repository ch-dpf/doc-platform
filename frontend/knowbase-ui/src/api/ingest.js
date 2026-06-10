import client from './client'

export function parsePreview(file, libraryId) {
  const form = new FormData()
  form.append('file', file)
  const params = libraryId ? { libraryId } : undefined
  return client.post('/api/v1/documents/parse-preview', form, {
    params,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getUploadConstraints(libraryId) {
  return client.get('/api/v1/documents/upload-constraints', { params: { libraryId } })
}

function uploadParams(libraryId, tenantId, autoIndex, documentMetadata) {
  const params = { libraryId, tenantId, autoIndex }
  if (documentMetadata?.trim()) params.documentMetadata = documentMetadata.trim()
  return params
}

export function uploadDocument(libraryId, tenantId, file, autoIndex = true, onProgress, documentMetadata) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/api/v1/documents/upload', form, {
    params: uploadParams(libraryId, tenantId, autoIndex, documentMetadata),
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function uploadDocumentAsync(libraryId, tenantId, file, autoIndex = true, onProgress, documentMetadata) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/api/v1/documents/upload/async', form, {
    params: uploadParams(libraryId, tenantId, autoIndex, documentMetadata),
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function uploadDocumentsBatch(libraryId, tenantId, files, autoIndex = true, onProgress, documentMetadata) {
  const form = new FormData()
  for (const f of files) {
    const name = f.webkitRelativePath || f.name
    form.append('files', f, name)
  }
  return client.post('/api/v1/documents/upload/batch', form, {
    params: uploadParams(libraryId, tenantId, autoIndex, documentMetadata),
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function listDocuments(params) {
  return client.get('/api/v1/documents', { params })
}

export function getDocument(docId) {
  return client.get(`/api/v1/documents/${docId}`)
}

export function getDocumentChunks(docId, params) {
  return client.get(`/api/v1/documents/${docId}/chunks`, { params })
}

export function deleteDocument(docId) {
  return client.delete(`/api/v1/documents/${docId}`)
}

export function purgeDocument(docId) {
  return client.delete(`/api/v1/documents/${docId}/purge`)
}

export function approveDocumentIndex(docId) {
  return client.post(`/api/v1/documents/${docId}/approve-index`)
}
