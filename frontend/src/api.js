const API_GATEWAY = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const API_KEY = import.meta.env.VITE_API_KEY || 'dev-local-key'

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': API_KEY,
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
  return request(`${API_GATEWAY}/api/media${suffix}`)
}

export function createMedia(payload) {
  return request(`${API_GATEWAY}/api/media`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function importMedia(payload) {
  return request(`${API_GATEWAY}/api/acquisition/import`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function streamManifest(mediaId) {
  return request(`${API_GATEWAY}/api/stream/${mediaId}/manifest`)
}

export function streamCaptionsUrl(mediaId, lang = 'en') {
  return `${API_GATEWAY}/api/stream/${mediaId}/captions?lang=${encodeURIComponent(lang)}&api_key=${encodeURIComponent(API_KEY)}`
}
