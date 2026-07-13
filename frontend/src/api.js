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

function normalizePosterImageData(item) {
  const imageData = item?.poster?.imageData
  if (imageData === null || imageData === undefined) {
    return item?.posterUrl || ''
  }

  if (typeof imageData === 'string') {
    return imageData
  }

  if (Array.isArray(imageData)) {
    try {
      const binary = String.fromCharCode(...imageData)
      return btoa(binary)
    } catch {
      return item?.posterUrl || ''
    }
  }

  return item?.posterUrl || ''
}

function normalizeCatalogMovie(item) {
  const metadata = item?.metaData || item?.metadata || item?.meta || {}
  const mediaItem = item?.mediaItem || item?.media || {}
  const posterImageData = normalizePosterImageData(item)

  return {
    ...item,
    id: item?.id || item?.movieId || item?.mediaId || mediaItem?.mediaId,
    mediaId: item?.mediaId || mediaItem?.mediaId || item?.movieId || item?.id,
    movieId: item?.movieId || item?.id || null,
    type: item?.type || 'movie',
    title: metadata?.title || item?.title || item?.name || 'Movie',
    plotSummary: metadata?.plotSummary || item?.plotSummary || item?.description || '',
    description: metadata?.plotSummary || item?.plotSummary || item?.description || '',
    releaseDate: metadata?.airDate || metadata?.releaseDate || item?.releaseDate || null,
    rating: metadata?.rating ?? item?.rating ?? null,
    streamUrl: item?.streamUrl || item?.filePath || mediaItem?.filePath || '',
    posterUrl: item?.posterUrl || metadata?.posterUrl || posterImageData,
    metaData: metadata,
    metadata,
    mediaItem,
    media: mediaItem,
    poster: item?.poster || null
  }
}

function normalizeCatalogEpisode(item) {
  const metadata = item?.metaData || item?.metadata || item?.meta || {}
  const mediaItem = item?.media || item?.mediaItem || {}
  const posterImageData = normalizePosterImageData(item)

  return {
    ...item,
    id: item?.id || item?.episodeId || item?.mediaId || mediaItem?.mediaId,
    mediaId: item?.mediaId || mediaItem?.mediaId || item?.episodeId || item?.id,
    episodeId: item?.episodeId || item?.id || null,
    type: item?.type || 'episode',
    title: metadata?.title || item?.title || item?.name || 'Episode',
    plotSummary: metadata?.plotSummary || item?.plotSummary || item?.description || '',
    description: metadata?.plotSummary || item?.plotSummary || item?.description || '',
    releaseDate: metadata?.airDate || metadata?.releaseDate || item?.releaseDate || null,
    rating: metadata?.rating ?? item?.rating ?? null,
    episodeNumber: item?.episodeNumber ?? null,
    seasonId: item?.seasonId || null,
    seriesId: item?.seriesId || null,
    streamUrl: item?.streamUrl || item?.filePath || mediaItem?.filePath || '',
    posterUrl: item?.posterUrl || metadata?.posterUrl || posterImageData,
    metaData: metadata,
    metadata,
    media: mediaItem,
    mediaItem,
    poster: item?.poster || null
  }
}

export function searchCatalogEpisodes(seriesId, seasonId, query) {
  const suffix = buildCatalogSuffix({ seriesId, seasonId, query })
  return request(`${API_GATEWAY}/api/media/episodes${suffix}`)
    .then((results) => (Array.isArray(results) ? results.map(normalizeCatalogEpisode) : []))
}

export function searchCatalogMovies(query) {
  const suffix = buildCatalogSuffix({ query })
  return request(`${API_GATEWAY}/api/media/movies${suffix}`)
    .then((results) => (Array.isArray(results) ? results.map(normalizeCatalogMovie) : []))
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

export function getTorrentInfo() {
  return request(`${API_GATEWAY}/api/stream/torrent/info`)
}

export function pauseTorrent(infoHash) {
  return request(`${API_GATEWAY}/api/stream/torrent/pause/${encodeURIComponent(infoHash)}`, {
    method: 'POST'
  })
}

export function resumeTorrent(infoHash) {
  return request(`${API_GATEWAY}/api/stream/torrent/resume/${encodeURIComponent(infoHash)}`, {
    method: 'POST'
  })
}

export function deleteTorrent(infoHash) {
  return request(`${API_GATEWAY}/api/stream/torrent/delete/${encodeURIComponent(infoHash)}`, {
    method: 'POST'
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
