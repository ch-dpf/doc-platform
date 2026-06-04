import client from './client'

export function parsePreview(file) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/api/v1/documents/parse-preview', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getUploadConstraints(libraryId) {
  return client.get('/api/v1/documents/upload-constraints', { params: { libraryId } })
}

export function uploadDocument(libraryId, tenantId, file, autoIndex = true, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/api/v1/documents/upload', form, {
    params: { libraryId, tenantId, autoIndex },
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function uploadDocumentAsync(libraryId, tenantId, file, autoIndex = true, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/api/v1/documents/upload/async', form, {
    params: { libraryId, tenantId, autoIndex },
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function uploadDocumentsBatch(libraryId, tenantId, files, autoIndex = true, onProgress) {
  const form = new FormData()
  for (const f of files) {
    form.append('files', f)
  }
  return client.post('/api/v1/documents/upload/batch', form, {
    params: { libraryId, tenantId, autoIndex },
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function collectDocument(body) {
  return client.post('/api/v1/documents/collect', body)
}

export function listDocuments(params) {
  return client.get('/api/v1/documents', { params })
}

export function getDocument(docId) {
  return client.get(`/api/v1/documents/${docId}`)
}

export function deleteDocument(docId) {
  return client.delete(`/api/v1/documents/${docId}`)
}

export function purgeDocument(docId) {
  return client.delete(`/api/v1/documents/${docId}/purge`)
}
