<template>
  <div class="page">
    <header class="hero">
      <h1>Home Media Server</h1>
      <p>Search, ingest, and stream your movie and series library with subtitles.</p>
    </header>

    <section class="panel controls">
      <div class="search-row">
        <input v-model="query" placeholder="Search media library" @keyup.enter="runLibrarySearch" />
        <button @click="runLibrarySearch">Search</button>
      </div>

      <div ref="acquisitionSearchContainer" class="acquisition-search">
        <label for="acquisition-search-box">Acquisition Search</label>
        <div class="acquisition-actions">
          <select v-model="acquisitionCategory" aria-label="Select acquisition category">
            <option value="MOVIE">Movies</option>
            <option value="SERIES">Series</option>
          </select>
          <select v-model="acquisitionSortBy" aria-label="Sort acquisition results">
            <option value="seeders">Sort by Seeders</option>
            <option value="size">Sort by Size</option>
          </select>
          <button type="button" @click="startAcquisitionSearch">Search Sources</button>
        </div>
        <div class="acquisition-search-box">
          <input
            id="acquisition-search-box"
            v-model="acquisitionQuery"
            placeholder="Search torrent sources"
            @focus="onAcquisitionFocus"
            @keyup.enter="startAcquisitionSearch"
            @keydown.esc="hideAcquisitionPopup"
          />

          <div v-if="showAcquisitionPopup" class="acquisition-popup">
            <p v-if="acquisitionLoading && !hasAnyAcquisitionResults" class="popup-state">Searching...</p>
            <p v-else-if="!acquisitionLoading && !hasAnyAcquisitionResults" class="popup-state">No results found.</p>

            <div v-else class="group-list">
              <section v-for="group in groupedAcquisitionResults" :key="group.source" class="result-group">
                <h4>{{ group.source }}</h4>
                <ul>
                  <li v-for="(result, index) in group.results" :key="result.magnetLink || result.sourceUrl || `${result.title}-${index}`">
                    <article class="popup-result">
                      <div class="popup-result-main">
                        <span class="popup-title">{{ result.title }}</span>
                        <span class="popup-meta">{{ result.source }}</span>
                        <span class="popup-stats">
                          <span class="popup-stat">Size: {{ result.size || 'N/A' }}</span>
                          <span class="popup-stat">Seeders: {{ result.seeders || 'N/A' }}</span>
                          <span class="popup-stat">Leechers: {{ result.leechers || 'N/A' }}</span>
                        </span>
                      </div>
                      <div class="popup-buttons">
                        <button type="button" class="popup-action" @click="useAcquisitionResult(result)">Use Title</button>
                        <button
                          type="button"
                          class="popup-action"
                          @click="importSearchResult(result)"
                          :disabled="isImportingResult(result.magnetLink)"
                        >
                          {{ isImportingResult(result.magnetLink) ? 'Importing...' : 'Import to Stream' }}
                        </button>
                      </div>
                    </article>
                  </li>
                </ul>
              </section>
            </div>
          </div>
        </div>
      </div>

      <div class="upload-grid">
        <input v-model="uploadRequest.title" placeholder="Upload title" />
        <select v-model="uploadRequest.type">
          <option value="movie">Movie</option>
          <option value="series">Series</option>
        </select>
        <input v-model.number="uploadRequest.year" type="number" placeholder="Year" />
        <input v-model="uploadRequest.description" placeholder="Description" />
        <input type="file" accept="video/*" @change="onUploadFileSelected" />
        <button @click="uploadAndCreateMedia">Upload Video + Add Catalog Item</button>
      </div>

      <p v-if="importStatus" class="status">{{ importStatus }}</p>
      <p v-if="uploadStatus" class="status">{{ uploadStatus }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>

    <main class="layout">
      <section class="panel library">
        <div class="library-header">
          <h2>Media Library</h2>
          <div class="library-breadcrumbs">
            <button v-if="libraryScreen !== 'home'" type="button" class="secondary-button" @click="backToHome">Back to Library</button>
            <nav class="breadcrumb-path" aria-label="Media library breadcrumb">
              <button type="button" class="breadcrumb-link" @click="backToHome">Library</button>
              <template v-if="libraryScreen === 'series'">
                <span class="breadcrumb-separator">/</span>
                <span class="breadcrumb-current">{{ selectedSeries?.title || 'Series' }}</span>
              </template>
              <template v-if="libraryScreen === 'movie'">
                <span class="breadcrumb-separator">/</span>
                <span class="breadcrumb-current">{{ selectedMovie?.title || 'Movie' }}</span>
              </template>
            </nav>
          </div>
        </div>

        <p v-if="libraryLoading">Loading library...</p>

        <div v-else-if="libraryScreen === 'home'" class="library-sections">
          <section class="library-subsection">
            <h3>Series</h3>
            <p v-if="!series.length" class="muted">No series found.</p>
            <div v-else class="cards">
              <MediaCard
                v-for="item in series"
                :key="item.seriesId || item.id"
                :item="item"
                action-label="Open Series"
                @action="openSeries"
              />
            </div>
          </section>

          <section class="library-subsection">
            <h3>Movies</h3>
            <p v-if="!movies.length" class="muted">No movies found.</p>
            <div v-else class="cards">
              <MediaCard
                v-for="item in movies"
                :key="item.mediaId || item.id"
                :item="item"
                action-label="Open Movie"
                @action="openMovie"
              />
            </div>
          </section>
        </div>

        <div v-else-if="libraryScreen === 'series'" class="series-detail">
          <section class="library-subsection">
            <h3>Series</h3>
            <MediaCard v-if="selectedSeries" :item="selectedSeries" :show-action="false" />
          </section>

          <section class="library-subsection">
            <h3>Seasons</h3>
            <p v-if="!seasons.length" class="muted">No seasons found for this series.</p>
            <div v-else class="season-buttons">
              <button
                v-for="season in seasons"
                :key="season.seasonId || season.id"
                type="button"
                class="secondary-button"
                :class="{ 'season-active': selectedSeason?.seasonId === season.seasonId }"
                @click="openSeason(season)"
              >
                {{ season.title || getSeasonDisplayName(season) }}
              </button>
            </div>
          </section>

          <section class="library-subsection">
            <h3>Episodes</h3>
            <p v-if="!episodes.length" class="muted">No episodes found for this season.</p>
            <div v-else class="cards">
              <MediaCard
                v-for="item in episodes"
                :key="item.mediaId || item.id"
                :item="item"
                action-label="Stream Episode"
                @action="startPlayback"
              />
            </div>
          </section>

          <section class="panel player inline-player">
            <h2>Stream Window</h2>
            <p v-if="!activeMedia">Select an episode to begin streaming.</p>
            <div v-else>
              <h3>{{ activeMedia.title }}</h3>
              <p class="player-meta">
                <span v-if="activeMedia.type">Type: {{ activeMedia.type }}</span>
                <span v-if="activeMedia.releaseDate">Release: {{ formatReleaseDate(activeMedia.releaseDate) }}</span>
                <span v-if="activeMedia.rating !== null && activeMedia.rating !== undefined">Rating: {{ formatRating(activeMedia.rating) }}</span>
              </p>
              <p v-if="activeMedia.plotSummary" class="player-summary">{{ activeMedia.plotSummary }}</p>
              <video ref="player" controls preload="metadata" :src="`${API_GATEWAY}/${manifest?.playbackUrl || activeMedia.streamUrl}`">
                <track
                  v-for="track in tracks"
                  :key="track.language"
                  kind="subtitles"
                  :label="track.label"
                  :src="track.url"
                  :srclang="track.language"
                  :default="track.language === selectedCaption"
                />
              </video>
              <div class="caption-controls">
                <label for="caption-select">Closed Captions</label>
                <select id="caption-select" v-model="selectedCaption" @change="applyCaptionTrack">
                  <option value="off">Off</option>
                  <option v-for="track in tracks" :key="track.language" :value="track.language">
                    {{ track.label }}
                  </option>
                </select>
              </div>
            </div>
          </section>
        </div>

        <div v-else class="movie-detail">
          <section class="library-subsection">
            <h3>Movie</h3>
            <MediaCard v-if="selectedMovie" :item="selectedMovie" action-label="Stream Movie" @action="startPlayback" />
          </section>

          <section class="panel player inline-player">
            <h2>Stream Window</h2>
            <p v-if="!activeMedia">Select this movie to begin streaming.</p>
            <div v-else>
              <h3>{{ activeMedia.title }}</h3>
              <p class="player-meta">
                <span v-if="activeMedia.type">Type: {{ activeMedia.type }}</span>
                <span v-if="activeMedia.releaseDate">Release: {{ formatReleaseDate(activeMedia.releaseDate) }}</span>
                <span v-if="activeMedia.rating !== null && activeMedia.rating !== undefined">Rating: {{ formatRating(activeMedia.rating) }}</span>
              </p>
              <p v-if="activeMedia.plotSummary" class="player-summary">{{ activeMedia.plotSummary }}</p>
              <video ref="player" controls preload="metadata" :src="`${API_GATEWAY}/${manifest?.playbackUrl || activeMedia.streamUrl}`">
                <track
                  v-for="track in tracks"
                  :key="track.language"
                  kind="subtitles"
                  :label="track.label"
                  :src="track.url"
                  :srclang="track.language"
                  :default="track.language === selectedCaption"
                />
              </video>
              <div class="caption-controls">
                <label for="caption-select">Closed Captions</label>
                <select id="caption-select" v-model="selectedCaption" @change="applyCaptionTrack">
                  <option value="off">Off</option>
                  <option v-for="track in tracks" :key="track.language" :value="track.language">
                    {{ track.label }}
                  </option>
                </select>
              </div>
            </div>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import MediaCard from './components/MediaCard.vue'
import {
  importStreamMedia,
  searchAcquisitionStream,
  searchCatalogEpisodes,
  searchCatalogMovies,
  searchCatalogSeasons,
  searchCatalogSeries,
  streamCaptionsUrl,
  streamManifest,
  uploadMediaFile,
  API_GATEWAY
} from './api'

const movies = ref([])
const series = ref([])
const seasons = ref([])
const episodes = ref([])
const query = ref('')
const libraryScreen = ref('home')
const libraryLoading = ref(false)
const selectedSeries = ref(null)
const selectedSeason = ref(null)
const selectedMovie = ref(null)
const activeMedia = ref(null)
const manifest = ref(null)
const tracks = ref([])
const selectedCaption = ref('off')
const importStatus = ref('')
const uploadStatus = ref('')
const error = ref('')
const player = ref(null)
const selectedUploadFile = ref(null)

const acquisitionSearchContainer = ref(null)
const acquisitionQuery = ref('')
const acquisitionResultsBySource = ref({})
const acquisitionLoading = ref(false)
const showAcquisitionPopup = ref(false)
const acquisitionCategory = ref('MOVIE')
const acquisitionSortBy = ref('seeders')
const importInFlightByMagnet = ref({})
let acquisitionSearchAbortController = null

const uploadRequest = ref({
  title: '',
  type: 'movie',
  year: new Date().getFullYear(),
  description: ''
})

function resetAcquisitionResults() {
  acquisitionResultsBySource.value = {}
}

function isDuplicateAcquisitionResult(result) {
  return Object.values(acquisitionResultsBySource.value).some((group) => group.some((existing) => (
    (result.magnetLink && result.magnetLink === existing.magnetLink)
    || (result.sourceUrl && result.sourceUrl === existing.sourceUrl)
  )))
}

function parseSeeders(value) {
  const parsed = Number.parseInt(String(value || '').replace(/[^\d]/g, ''), 10)
  return Number.isFinite(parsed) ? parsed : 0
}

function parseSizeToBytes(size) {
  if (!size) {
    return 0
  }

  const match = String(size).trim().match(/([\d.]+)\s*([kmgtp]?b)/i)
  if (!match) {
    return 0
  }

  const value = Number.parseFloat(match[1])
  if (!Number.isFinite(value)) {
    return 0
  }

  const unit = match[2].toUpperCase()
  const multipliers = {
    B: 1,
    KB: 1024,
    MB: 1024 ** 2,
    GB: 1024 ** 3,
    TB: 1024 ** 4,
    PB: 1024 ** 5
  }

  return value * (multipliers[unit] || 1)
}

function compareAcquisitionResults(left, right) {
  if (acquisitionSortBy.value === 'size') {
    return parseSizeToBytes(right.size) - parseSizeToBytes(left.size)
  }
  return parseSeeders(right.seeders) - parseSeeders(left.seeders)
}

const groupedAcquisitionResults = computed(() => Object.entries(acquisitionResultsBySource.value)
  .map(([source, results]) => ({
    source,
    results: [...results].sort(compareAcquisitionResults)
  }))
  .sort((a, b) => a.source.localeCompare(b.source)))

const hasAnyAcquisitionResults = computed(() => groupedAcquisitionResults.value.some((group) => group.results.length > 0))

function getSeriesDisplayName(seriesItem) {
  return seriesItem?.metaData?.title
    || seriesItem?.metadata?.title
    || seriesItem?.name
    || 'Series'
}

function getSeasonDisplayName(season) {
  return season?.metaData?.title
    || season?.metadata?.title
    || season?.name
    || `Season ${season?.seasonNumber || ''}`.trim()
    || 'Season'
}

function pickMetaBlock(item) {
  return item?.metaData || item?.metadata || item?.meta || {}
}

function normalizeSeriesItem(item) {
  const metadata = pickMetaBlock(item)
  const releaseDate = metadata.firstAirDate || metadata.releaseDate || item.releaseDate || null
  const numericRating = Number.parseFloat(metadata.rating ?? item.rating)

  return {
    id: item.id || item.seriesId,
    mediaId: item.mediaId || item.seriesId || item.id,
    seriesId: item.seriesId || item.id,
    type: item.type || 'series',
    title: metadata.title || item.title || item.name || 'Series',
    plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
    description: metadata.plotSummary || item.plotSummary || item.description || '',
    releaseDate,
    year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : 0),
    rating: Number.isFinite(numericRating) ? numericRating : null,
    posterUrl: metadata.posterUrl || item.posterUrl || '',
    streamUrl: item.streamUrl || item.filePath || ''
  }
}

function normalizeSeasonItem(item, parentSeries) {
  const metadata = pickMetaBlock(item)
  return {
    ...item,
    id: item.id || item.seasonId,
    seasonId: item.seasonId || item.id,
    seriesId: item.seriesId || parentSeries?.seriesId,
    type: 'season',
    title: metadata.title || item.title || item.name || getSeasonDisplayName(item),
    plotSummary: metadata.plotSummary || item.plotSummary || '',
    description: metadata.plotSummary || item.plotSummary || '',
    seasonName: metadata.title || item.name || getSeasonDisplayName(item),
    seasonNumber: item.seasonNumber,
    releaseDate: metadata.releaseDate || metadata.firstAirDate || item.releaseDate || null,
    rating: Number.isFinite(Number.parseFloat(metadata.rating ?? item.rating))
      ? Number.parseFloat(metadata.rating ?? item.rating)
      : null,
    posterUrl: metadata.posterUrl || item.posterUrl || '',
    streamUrl: item.streamUrl || item.filePath || ''
  }
}

function normalizeMediaItem(item, fallbackType) {
  const metadata = pickMetaBlock(item)
  const releaseDate = metadata.releaseDate
    || metadata.airDate
    || metadata.firstAirDate
    || item.releaseDate
    || null
  const numericRating = Number.parseFloat(metadata.rating ?? item.rating)

  return {
    mediaId: item.mediaId || item.id,
    id: item.id || item.mediaId,
    seriesId: item.seriesId || null,
    seasonId: item.seasonId || null,
    title: metadata.title || item.title || item.name || 'Untitled',
    type: item.type || fallbackType,
    seriesName: item.seriesName || item.series || null,
    seasonName: item.seasonName || null,
    seasonNumber: item.seasonNumber ?? null,
    episodeNumber: item.episodeNumber ?? null,
    releaseDate,
    year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : 0),
    rating: Number.isFinite(numericRating) ? numericRating : null,
    plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
    description: metadata.plotSummary || item.plotSummary || item.description || '',
    posterUrl: metadata.posterUrl || item.posterUrl || '',
    streamUrl: item.streamUrl || item.filePath || ''
  }
}

function formatReleaseDate(value) {
  if (!value) {
    return ''
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return String(value)
  }

  return parsed.toLocaleDateString()
}

function formatRating(value) {
  const parsed = Number.parseFloat(value)
  if (!Number.isFinite(parsed)) {
    return 'N/A'
  }
  return parsed.toFixed(1)
}

async function loadLibraryHome() {
  error.value = ''
  libraryLoading.value = true

  try {
    const [seriesResults, movieResults] = await Promise.all([
      searchCatalogSeries(query.value),
      searchCatalogMovies(query.value)
    ])

    series.value = (Array.isArray(seriesResults) ? seriesResults : [])
      .map((item) => normalizeSeriesItem(item))
    movies.value = (Array.isArray(movieResults) ? movieResults : [])
      .map((item) => normalizeMediaItem(item, 'movie'))
  } catch (err) {
    error.value = err.message || 'Failed to load media library.'
  } finally {
    libraryLoading.value = false
  }
}

async function loadSeriesSeasons() {
  if (!selectedSeries.value?.seriesId) {
    return
  }

  error.value = ''
  libraryLoading.value = true

  try {
    const result = await searchCatalogSeasons(selectedSeries.value.seriesId, query.value)
    seasons.value = Array.isArray(result)
      ? [...result]
        .map((season) => normalizeSeasonItem(season, selectedSeries.value))
        .sort((left, right) => (left.seasonNumber || 0) - (right.seasonNumber || 0))
      : []

    if (!selectedSeason.value && seasons.value.length) {
      selectedSeason.value = seasons.value[0]
      await loadSeasonEpisodes()
    }
  } catch (err) {
    error.value = err.message || 'Failed to load seasons.'
  } finally {
    libraryLoading.value = false
  }
}

async function loadSeasonEpisodes() {
  if (!selectedSeries.value?.seriesId || !selectedSeason.value?.seasonId) {
    return
  }

  error.value = ''
  libraryLoading.value = true

  try {
    const result = await searchCatalogEpisodes(selectedSeries.value.seriesId, selectedSeason.value.seasonId, query.value)
    episodes.value = (Array.isArray(result) ? result : [])
      .map((item) => normalizeMediaItem(item, 'episode'))
  } catch (err) {
    error.value = err.message || 'Failed to load episodes.'
  } finally {
    libraryLoading.value = false
  }
}

async function runLibrarySearch() {
  if (libraryScreen.value === 'series') {
    await loadSeriesSeasons()
    if (selectedSeason.value) {
      await loadSeasonEpisodes()
    }
    return
  }

  if (libraryScreen.value === 'movie') {
    await loadLibraryHome()
    if (selectedMovie.value) {
      const refreshed = movies.value.find((item) => item.mediaId === selectedMovie.value.mediaId)
      selectedMovie.value = refreshed || selectedMovie.value
    }
    return
  }

  await loadLibraryHome()
}

async function openSeries(seriesItem) {
  selectedSeries.value = normalizeSeriesItem(seriesItem)
  selectedSeason.value = null
  selectedMovie.value = null
  seasons.value = []
  episodes.value = []
  activeMedia.value = null
  manifest.value = null
  tracks.value = []
  libraryScreen.value = 'series'
  await loadSeriesSeasons()
}

function openMovie(movieItem) {
  selectedMovie.value = normalizeMediaItem(movieItem, 'movie')
  selectedSeries.value = null
  selectedSeason.value = null
  seasons.value = []
  episodes.value = []
  activeMedia.value = null
  manifest.value = null
  tracks.value = []
  libraryScreen.value = 'movie'
}

async function openSeason(season) {
  selectedSeason.value = normalizeSeasonItem(season, selectedSeries.value)
  episodes.value = []
  await loadSeasonEpisodes()
}

function backToHome() {
  libraryScreen.value = 'home'
  selectedSeries.value = null
  selectedSeason.value = null
  selectedMovie.value = null
  seasons.value = []
  episodes.value = []
  activeMedia.value = null
  manifest.value = null
  tracks.value = []
}

function startAcquisitionSearch() {
  const term = acquisitionQuery.value.trim()
  const category = acquisitionCategory.value

  if (!term) {
    resetAcquisitionResults()
    showAcquisitionPopup.value = false
    return
  }

  fetchAcquisitionResults(term, category)
}

async function fetchAcquisitionResults(term, category) {
  if (acquisitionSearchAbortController) {
    acquisitionSearchAbortController.abort()
  }

  acquisitionSearchAbortController = new AbortController()
  acquisitionLoading.value = true
  error.value = ''
  showAcquisitionPopup.value = true
  resetAcquisitionResults()

  try {
    await searchAcquisitionStream(term, category, {
      signal: acquisitionSearchAbortController.signal,
      onItem: (item) => {
        if (!item || !item.title) {
          return
        }

        const searchResult = {
          ...item,
          category
        }

        if (!isDuplicateAcquisitionResult(searchResult)) {
          const source = searchResult.source || 'Unknown Source'
          const existingSourceItems = acquisitionResultsBySource.value[source] || []
          acquisitionResultsBySource.value = {
            ...acquisitionResultsBySource.value,
            [source]: [...existingSourceItems, searchResult]
          }
        }
      },
      onError: (message) => {
        error.value = String(message || 'Acquisition search stream returned an error.')
      },
      onDone: () => {
        acquisitionLoading.value = false
      }
    })
  } catch (err) {
    if (err.name === 'AbortError') {
      return
    }
    resetAcquisitionResults()
    error.value = err.message
  } finally {
    if (acquisitionSearchAbortController?.signal.aborted) {
      return
    }
    acquisitionLoading.value = false
  }
}

function onAcquisitionFocus() {
  if (acquisitionQuery.value.trim() || hasAnyAcquisitionResults.value) {
    showAcquisitionPopup.value = true
  }
}

function hideAcquisitionPopup() {
  showAcquisitionPopup.value = false
}

function useAcquisitionResult(result) {
  uploadRequest.value.title = result.title || ''
  acquisitionQuery.value = result.title || ''
  showAcquisitionPopup.value = false
}

function isImportingResult(magnetLink) {
  return Boolean(importInFlightByMagnet.value[magnetLink])
}

async function importSearchResult(result) {
  if (!result?.title || !result?.magnetLink) {
    error.value = 'Selected result is missing title or magnet link.'
    return
  }

  importInFlightByMagnet.value = {
    ...importInFlightByMagnet.value,
    [result.magnetLink]: true
  }
  error.value = ''

  try {
    const created = await importStreamMedia({
      title: result.title,
      magnetLink: result.magnetLink,
      category: result.category || acquisitionCategory.value
    })

    importStatus.value = created
      ? `Import request created for "${result.title}".`
      : `Import request could not be created for "${result.title}".`
  } catch (err) {
    error.value = err.message
  } finally {
    const nextInFlight = { ...importInFlightByMagnet.value }
    delete nextInFlight[result.magnetLink]
    importInFlightByMagnet.value = nextInFlight
  }
}

function handleDocumentClick(event) {
  if (!acquisitionSearchContainer.value) {
    return
  }

  if (!acquisitionSearchContainer.value.contains(event.target)) {
    hideAcquisitionPopup()
  }
}

function onUploadFileSelected(event) {
  const [file] = event.target.files || []
  selectedUploadFile.value = file || null
}

async function uploadAndCreateMedia() {
  if (!uploadRequest.value.title) {
    error.value = 'Title is required to upload media.'
    return
  }

  if (!selectedUploadFile.value) {
    error.value = 'A video file is required for upload.'
    return
  }

  error.value = ''
  uploadStatus.value = 'Uploading file to stream service...'

  try {
    const uploadResult = await uploadMediaFile(selectedUploadFile.value, {
      title: uploadRequest.value.title,
      type: uploadRequest.value.type,
      year: uploadRequest.value.year,
      description: uploadRequest.value.description
    })

    uploadStatus.value = `Upload complete: ${selectedUploadFile.value.name}`
    uploadRequest.value.title = ''
    uploadRequest.value.description = ''
    selectedUploadFile.value = null
    await runLibrarySearch()
  } catch (err) {
    uploadStatus.value = ''
    error.value = err.message
  }
}

function resolveMediaId(item) {
  return item?.mediaId || item?.id
}

async function startPlayback(item) {
  activeMedia.value = item
  selectedCaption.value = 'off'
  error.value = ''

  try {
    manifest.value = await streamManifest(resolveMediaId(item), item.streamUrl)
    tracks.value = (manifest.value.captions || []).map((track) => ({
      ...track,
      url: streamCaptionsUrl(resolveMediaId(item), track.language)
    }))
    await nextTick()
    applyCaptionTrack()
  } catch (err) {
    error.value = err.message
  }
}

function applyCaptionTrack() {
  const element = player.value
  if (!element || !element.textTracks) {
    return
  }

  for (let i = 0; i < element.textTracks.length; i += 1) {
    const track = element.textTracks[i]
    track.mode = selectedCaption.value !== 'off' && track.language === selectedCaption.value ? 'showing' : 'disabled'
  }
}

onMounted(() => {
  loadLibraryHome()
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  if (acquisitionSearchAbortController) {
    acquisitionSearchAbortController.abort()
  }
})
</script>
