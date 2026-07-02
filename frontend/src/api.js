export const API_GATEWAY = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
export const API_KEY = import.meta.env.VITE_API_KEY || 'dev-local-key'

const USE_MOCK_DATA = String(import.meta.env.VITE_USE_MOCK_DATA ?? 'true').toLowerCase() !== 'false'

const mockSeries = [
  {
    seriesId: 'series-aurora',
    type: 'series',
    metaData: {
      title: 'Aurora Nights',
      plotSummary: 'A hopeful sci-fi drama following a family navigating a city lit by the northern lights.',
      releaseDate: '2019-03-15',
      rating: 8.4,
      posterUrl: 'https://images.unsplash.com/photo-1517602302552-471fe67acf66?auto=format&fit=crop&w=800&q=80'
    }
  },
  {
    seriesId: 'series-harbor',
    type: 'series',
    metaData: {
      title: 'Harbor Stories',
      plotSummary: 'Interwoven tales of fishermen, chefs, and dreamers along a quiet coastal town.',
      releaseDate: '2021-07-22',
      rating: 7.9,
      posterUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80'
    }
  }
]

const mockMovies = [
  {
    mediaId: 'movie-echo',
    type: 'movie',
    metaData: {
      title: 'Echoes of Summer',
      plotSummary: 'A nostalgic road trip through bright landscapes and old friendships.',
      releaseDate: '2020-06-10',
      rating: 7.6,
      posterUrl: 'https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=800&q=80'
    }
  },
  {
    mediaId: 'movie-velvet',
    type: 'movie',
    metaData: {
      title: 'Velvet Horizon',
      plotSummary: 'An astronaut returns home to discover the stars have changed the way people dream.',
      releaseDate: '2023-11-02',
      rating: 8.2,
      posterUrl: 'https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?auto=format&fit=crop&w=800&q=80'
    }
  }
]

const mockSeasons = {
  'series-aurora': [
    {
      seasonId: 'season-1',
      seasonNumber: 1,
      seriesId: 'series-aurora',
      metaData: {
        title: 'Season One',
        plotSummary: 'A city of neon and memory opens its secrets.',
        releaseDate: '2019-03-15',
        rating: 8.4,
        posterUrl: 'https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=800&q=80'
      }
    },
    {
      seasonId: 'season-2',
      seasonNumber: 2,
      seriesId: 'series-aurora',
      metaData: {
        title: 'Season Two',
        plotSummary: 'The lights grow brighter as loyalties are tested.',
        releaseDate: '2020-01-09',
        rating: 8.7,
        posterUrl: 'https://images.unsplash.com/photo-1482192596544-9eb780fc7f66?auto=format&fit=crop&w=800&q=80'
      }
    }
  ],
  'series-harbor': [
    {
      seasonId: 'season-1',
      seasonNumber: 1,
      seriesId: 'series-harbor',
      metaData: {
        title: 'Season One',
        plotSummary: 'The town wakes up to a storm and a surprising visitor.',
        releaseDate: '2021-07-22',
        rating: 7.9,
        posterUrl: 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=800&q=80'
      }
    }
  ]
}

const mockEpisodes = {
  'series-aurora': {
    'season-1': [
      {
        mediaId: 'ep-aurora-1',
        id: 'ep-aurora-1',
        seriesId: 'series-aurora',
        seasonId: 'season-1',
        seriesName: 'Aurora Nights',
        seasonName: 'Season One',
        seasonNumber: 1,
        episodeNumber: 1,
        type: 'episode',
        metaData: {
          title: 'The First Light',
          plotSummary: 'A new dawn reveals a hidden map beneath the city grid.',
          releaseDate: '2019-03-15',
          rating: 8.1,
          posterUrl: 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=800&q=80'
        }
      },
      {
        mediaId: 'ep-aurora-2',
        id: 'ep-aurora-2',
        seriesId: 'series-aurora',
        seasonId: 'season-1',
        seriesName: 'Aurora Nights',
        seasonName: 'Season One',
        seasonNumber: 1,
        episodeNumber: 2,
        type: 'episode',
        metaData: {
          title: 'Glass Harbor',
          plotSummary: 'A late-night call draws the family toward an abandoned observatory.',
          releaseDate: '2019-03-22',
          rating: 8.3,
          posterUrl: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=800&q=80'
        }
      }
    ],
    'season-2': [
      {
        mediaId: 'ep-aurora-3',
        id: 'ep-aurora-3',
        seriesId: 'series-aurora',
        seasonId: 'season-2',
        seriesName: 'Aurora Nights',
        seasonName: 'Season Two',
        seasonNumber: 2,
        episodeNumber: 1,
        type: 'episode',
        metaData: {
          title: 'After the Storm',
          plotSummary: 'A storm breaks the city apart, and the family must decide what to save.',
          releaseDate: '2020-01-09',
          rating: 8.6,
          posterUrl: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=800&q=80'
        }
      }
    ]
  },
  'series-harbor': {
    'season-1': [
      {
        mediaId: 'ep-harbor-1',
        id: 'ep-harbor-1',
        seriesId: 'series-harbor',
        seasonId: 'season-1',
        seriesName: 'Harbor Stories',
        seasonName: 'Season One',
        seasonNumber: 1,
        episodeNumber: 1,
        type: 'episode',
        metaData: {
          title: 'The Lantern Market',
          plotSummary: 'The town gathers for the annual market, but one secret stalls the celebration.',
          releaseDate: '2021-07-22',
          rating: 7.9,
          posterUrl: 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=800&q=80'
        }
      }
    ]
  }
}

const mockAcquisitionResults = [
  {
    title: 'Aurora Nights 1080p',
    source: 'TorrentGalaxy',
    size: '2.1 GB',
    seeders: 182,
    leechers: 17,
    magnetLink: 'magnet:?xt=urn:btih:demo-aurora',
    category: 'SERIES'
  },
  {
    title: 'Echoes of Summer 2160p',
    source: 'RARBG',
    size: '4.8 GB',
    seeders: 97,
    leechers: 8,
    magnetLink: 'magnet:?xt=urn:btih:demo-echo',
    category: 'MOVIE'
  },
  {
    title: 'Velvet Horizon BluRay',
    source: '1337x',
    size: '1.3 GB',
    seeders: 214,
    leechers: 11,
    magnetLink: 'magnet:?xt=urn:btih:demo-velvet',
    category: 'MOVIE'
  }
]

function cloneData(value) {
  return JSON.parse(JSON.stringify(value))
}

function shouldUseMockData() {
  return USE_MOCK_DATA
}

function matchesQuery(item, query) {
  const term = String(query || '').trim().toLowerCase()
  if (!term) {
    return true
  }

  const haystack = [
    item.title,
    item.metaData?.title,
    item.metaData?.plotSummary,
    item.description,
    item.plotSummary
  ].filter(Boolean).join(' ').toLowerCase()

  return haystack.includes(term)
}

function buildCaptionUrl(language, label) {
  const content = `WEBVTT\n\n00:00:00.000 --> 00:00:03.000\n${label} sample subtitle.\n`
  return `data:text/vtt;charset=utf-8,${encodeURIComponent(content)}`
}

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
  if (shouldUseMockData()) {
    return Promise.resolve([...mockSeries, ...mockMovies].filter((item) => matchesQuery(item, query)))
  }

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
  if (shouldUseMockData()) {
    return Promise.resolve(cloneData(mockSeries.filter((item) => matchesQuery(item, query))))
  }

  const suffix = buildCatalogSuffix({ query })
  return request(`${API_GATEWAY}/api/media/series${suffix}`)
}

export function searchCatalogSeasons(seriesId, query) {
  if (shouldUseMockData()) {
    const seasons = (mockSeasons[seriesId] || []).filter((item) => matchesQuery(item, query))
    return Promise.resolve(cloneData(seasons))
  }

  const suffix = buildCatalogSuffix({ seriesId, query })
  return request(`${API_GATEWAY}/api/media/seasons${suffix}`)
}

export function searchCatalogEpisodes(seriesId, seasonId, query) {
  if (shouldUseMockData()) {
    const episodes = (mockEpisodes[seriesId]?.[seasonId] || []).filter((item) => matchesQuery(item, query))
    return Promise.resolve(cloneData(episodes))
  }

  const suffix = buildCatalogSuffix({ seriesId, seasonId, query })
  return request(`${API_GATEWAY}/api/media/episodes${suffix}`)
}

export function searchCatalogMovies(query) {
  if (shouldUseMockData()) {
    return Promise.resolve(cloneData(mockMovies.filter((item) => matchesQuery(item, query))))
  }

  const suffix = buildCatalogSuffix({ query })
  return request(`${API_GATEWAY}/api/media/movies${suffix}`)
}

export function createMedia(payload) {
  if (shouldUseMockData()) {
    return Promise.resolve({
      id: `mock-media-${Date.now()}`,
      title: payload?.title || 'Demo media',
      status: 'created',
      payload
    })
  }

  return request(`${API_GATEWAY}/api/media`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function importMedia(payload) {
  if (shouldUseMockData()) {
    return Promise.resolve(`Mock import request created for ${payload?.title || 'media'}.`)
  }

  return request(`${API_GATEWAY}/api/acquisition/importRequest`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function importStreamMedia(payload) {
  if (shouldUseMockData()) {
    return Promise.resolve(`Mock stream import request created for ${payload?.title || 'media'}.`)
  }

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
  if (shouldUseMockData()) {
    return Promise.resolve(cloneData(mockAcquisitionResults.filter((item) => {
      if (category && item.category !== category) {
        return false
      }
      return matchesQuery(item, query)
    })))
  }

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
  if (shouldUseMockData()) {
    const { onItem, onError, onDone, signal } = options
    const filtered = mockAcquisitionResults.filter((item) => {
      if (category && item.category !== category) {
        return false
      }
      return matchesQuery(item, query)
    })

    for (const item of filtered) {
      if (signal?.aborted) {
        break
      }
      onItem?.(item)
    }

    if (signal?.aborted) {
      return
    }

    if (onError) {
      onError?.(null)
    }

    if (onDone) {
      onDone()
    }

    return
  }

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
  if (shouldUseMockData()) {
    return Promise.resolve({
      id: `mock-upload-${Date.now()}`,
      title: payload?.title || file?.name || 'Demo upload',
      playbackUrl: '/mock/demo-stream.mp4'
    })
  }

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
  if (shouldUseMockData()) {
    return Promise.resolve({
      mediaId,
      title: 'Demo stream',
      playbackUrl: playbackUrl || '/mock/demo-stream.mp4',
      captions: [
        { language: 'en', label: 'English' },
        { language: 'es', label: 'Español' }
      ]
    })
  }

  const suffix = playbackUrl ? `?playbackUrl=${encodeURIComponent(playbackUrl)}` : ''
  return request(`${API_GATEWAY}/api/stream/${mediaId}/manifest${suffix}`)
}

export function streamCaptionsUrl(mediaId, lang = 'en') {
  if (shouldUseMockData()) {
    const label = lang === 'es' ? 'Español' : 'English'
    return buildCaptionUrl(lang, label)
  }

  return `${API_GATEWAY}/api/stream/${mediaId}/captions?lang=${encodeURIComponent(lang)}&api_key=${encodeURIComponent(API_KEY)}`
}
