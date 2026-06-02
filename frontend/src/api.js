const API_BASE = {
  catalog: 'http://localhost:8081',
  acquisition: 'http://localhost:8082',
  stream: 'http://localhost:8083'
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed: ${response.status}`)
  }

  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }
  return response.text()
}

export function searchMedia(query) {
  const suffix = query ? `?query=${encodeURIComponent(query)}` : ''
  return request(`${API_BASE.catalog}/api/media${suffix}`)
}

export function createMedia(payload) {
  return request(`${API_BASE.catalog}/api/media`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function importMedia(payload) {
  return request(`${API_BASE.acquisition}/api/acquisition/import`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function streamManifest(mediaId) {
  return request(`${API_BASE.stream}/api/stream/${mediaId}/manifest`)
}

export function streamCaptionsUrl(mediaId, lang = 'en') {
  return `${API_BASE.stream}/api/stream/${mediaId}/captions?lang=${encodeURIComponent(lang)}`
}
