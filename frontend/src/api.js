export const API_GATEWAY = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080'
export const API_KEY = import.meta.env.VITE_GATEWAY_API_KEY || 'dev-local-key'

async function request(url, options = {}) {
  const hasFormDataBody = typeof FormData !== 'undefined' && options.body instanceof FormData

  const response = await fetch(url, {
    headers: {
      ...(hasFormDataBody ? {} : { 'Content-Type': 'application/json' }),
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

function buildCatalogSuffix(params = {}) {
  const search = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, value)
    }
  })

  const suffix = search.toString()
  return suffix ? `?${suffix}` : ''
}

export function searchCatalogSeries(query) {
  const suffix = buildCatalogSuffix({ query })
  return request(`${API_GATEWAY}/api/media/series${suffix}`)
}

export function searchCatalogSeasons(seriesId, query) {
  const suffix = buildCatalogSuffix({ seriesId, query })
  return request(`${API_GATEWAY}/api/media/seasons${suffix}`)
}

export function searchCatalogEpisodes(seriesId, seasonId, query) {
  const suffix = buildCatalogSuffix({ seriesId, seasonId, query })
  return request(`${API_GATEWAY}/api/media/episodes${suffix}`)
}

export function searchCatalogMovies(query) {
  const suffix = buildCatalogSuffix({ query })
  return request(`${API_GATEWAY}/api/media/movies${suffix}`)
}

export function createMedia(payload) {
  return request(`${API_GATEWAY}/api/media`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function importMedia(payload) {
  return request(`${API_GATEWAY}/api/acquisition/importRequest`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function importStreamMedia(payload) {
  return request(`${API_GATEWAY}/api/stream/importRequest`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

function buildAcquisitionSearchSuffix(query, category) {
  const params = new URLSearchParams()

  if (query) {
    params.set('query', query)
  }

  if (category) {
    params.set('category', category)
  }

  const suffix = params.toString()
  return suffix ? `?${suffix}` : ''
}

export function searchAcquisition(query, category = 'MOVIE') {
  const suffix = buildAcquisitionSearchSuffix(query, category)
  return request(`${API_GATEWAY}/api/acquisition/search${suffix}`)
}

function parseSseEvent(eventChunk) {
  const lines = eventChunk.split('\n')
  let eventName = 'message'
  const dataLines = []

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    if (!line || line.startsWith(':')) {
      continue
    }

    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim() || 'message'
      continue
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  if (!dataLines.length) {
    return null
  }

  const rawData = dataLines.join('\n')
  let data = rawData

  try {
    data = JSON.parse(rawData)
  } catch {
    // Non-JSON data payloads are still valid SSE and are passed through as strings.
  }

  return { eventName, data }
}

export async function searchAcquisitionStream(query, category = 'MOVIE', options = {}) {
  const { onItem, onError, onDone, signal } = options
  const suffix = buildAcquisitionSearchSuffix(query, category)

  const response = await fetch(`${API_GATEWAY}/api/acquisition/search${suffix}`, {
    method: 'GET',
    headers: {
      'Accept': 'text/event-stream',
      'X-API-Key': API_KEY
    },
    signal
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed: ${response.status}`)
  }

  if (!response.body) {
    throw new Error('Streaming response body is not available.')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const normalized = buffer.replace(/\r\n/g, '\n')
      const events = normalized.split('\n\n')
      buffer = events.pop() || ''

      for (const eventChunk of events) {
        const parsed = parseSseEvent(eventChunk)
        if (!parsed) {
          continue
        }

        if (parsed.eventName === 'error') {
          if (onError) {
            onError(parsed.data)
          }
          continue
        }

        if (onItem) {
          onItem(parsed.data)
        }
      }
    }

    if (onDone) {
      onDone()
    }
  } finally {
    reader.releaseLock()
  }
}

export function uploadMediaFile(file, payload) {
  const formData = new FormData()
  formData.append('file', file)
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      formData.append(key, value)
    }
  })  

  return request(`${API_GATEWAY}/api/stream/upload`, {
    method: 'POST',
    body: formData
  }).then((result) => ({
    ...result,
    playbackUrl: result.playbackUrl
      ? `${API_GATEWAY}${result.playbackUrl}`
      : result.playbackUrl
  }))
}

export function streamManifest(mediaId, playbackUrl) {
  const suffix = playbackUrl ? `?playbackUrl=${encodeURIComponent(playbackUrl)}` : ''
  return request(`${API_GATEWAY}/api/stream/${mediaId}/manifest${suffix}`)
}

export function streamCaptionsUrl(mediaId, lang = 'en') {
  return `${API_GATEWAY}/api/stream/${mediaId}/captions?lang=${encodeURIComponent(lang)}&api_key=${encodeURIComponent(API_KEY)}`
}
