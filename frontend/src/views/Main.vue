<template>
    <div class="page">
    <header class="hero">
      <h1>Home Media Server</h1>
      <p>Search, ingest, and stream your movie and series library with subtitles.</p>
    </header>

    <section class="panel controls">
      <div class="controls-header">
        <h2>Search Torrents</h2>
      </div>
      <p class="muted">Torrent search now runs from a dedicated page.</p>
      <div class="torrent-shortcuts">
        <button type="button" @click="openTorrentSearchPage">Open Search Page</button>
        <button type="button" class="secondary-button" @click="openTorrentQueuePage">Open Torrent Queue</button>
      </div>
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

        <p v-if="uploadStatus" class="status">{{ uploadStatus }}</p>
        <p v-if="error" class="error">{{ error }}</p>

        <p v-if="libraryLoading">Loading library...</p>

        
        <div v-else-if="libraryScreen === 'home'" class="library-sections">
          <h3>Search Library</h3>
          <div class="search-row">
            <input v-model="query" placeholder="Search media library" @keyup.enter="runLibrarySearch" />
            <button @click="runLibrarySearch">Search</button>
          </div>

          <h3>Upload to Library</h3>
          <div class="upload-grid">
            <input v-model="uploadRequest.title" placeholder="Upload title" />
            <select v-model="uploadRequest.type">
              <option value="movie">Movie</option>
              <option value="series">Series</option>
            </select>
            <input v-model.number="uploadRequest.year" type="number" placeholder="Year" />
            <input v-model="uploadRequest.description" placeholder="Description" />
            <input type="file" accept="video/*" @change="onUploadFileSelected" />
            <button @click="uploadAndCreateMedia">Upload</button>
          </div>

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
              <video ref="player" controls preload="metadata" :src="resolveMediaUrl(manifest?.playbackUrl || activeMedia.streamUrl)">
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
              <div v-if="mediaInfo" class="media-info-panel">
                <div class="progress-wrap media-progress-wrap">
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: `${mediaDownloadPercent}%` }"></div>
                  </div>
                  <strong>{{ mediaDownloadPercent.toFixed(1) }}%</strong>
                </div>
                <div class="detail-grid">
                  <div class="detail-row">
                    <strong>Stream mediaItemId:</strong>
                    <span>{{ displayValue(mediaInfo.mediaItemId) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>File size:</strong>
                    <span>{{ formatBytes(mediaInfo.fileSize) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Bytes downloaded:</strong>
                    <span>{{ formatBytes(mediaInfo.bytesDownloaded) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Delta bytes downloaded:</strong>
                    <span>{{ formatSpeed(mediaInfo.deltaBytesDownloaded) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Required download rate:</strong>
                    <span>{{ formatSpeed(mediaInfo.requiredDownloadRate) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Import media status:</strong>
                    <span>{{ formatImportMediaStatus(mediaInfo.importMediaStatus) }}</span>
                  </div>
                </div>
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
              <video ref="player" controls preload="metadata" :src="resolveMediaUrl(manifest?.playbackUrl || activeMedia.streamUrl)">
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
              <div v-if="mediaInfo" class="media-info-panel">
                <div class="progress-wrap media-progress-wrap">
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: `${mediaDownloadPercent}%` }"></div>
                  </div>
                  <strong>{{ mediaDownloadPercent.toFixed(1) }}%</strong>
                </div>
                <div class="detail-grid">
                  <div class="detail-row">
                    <strong>Stream mediaItemId:</strong>
                    <span>{{ displayValue(mediaInfo.mediaItemId) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>File size:</strong>
                    <span>{{ formatBytes(mediaInfo.fileSize) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Bytes downloaded:</strong>
                    <span>{{ formatBytes(mediaInfo.bytesDownloaded) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Delta bytes downloaded:</strong>
                    <span>{{ formatSpeed(mediaInfo.deltaBytesDownloaded) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Required download rate:</strong>
                    <span>{{ formatSpeed(mediaInfo.requiredDownloadRate) }}</span>
                  </div>
                  <div class="detail-row">
                    <strong>Import media status:</strong>
                    <span>{{ formatImportMediaStatus(mediaInfo.importMediaStatus) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<!-- JavaScript -->
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MediaCard from '../components/MediaCard.vue'
import {
  searchCatalogEpisodes,
  searchCatalogMovies,
  searchCatalogSeasons,
  searchCatalogSeries,
  streamCaptionsUrl,
  streamManifest,
  streamMediaItemInfo,
  uploadMediaFile,
  API_GATEWAY
} from '../api'

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
const uploadStatus = ref('')
const error = ref('')
const player = ref(null)
const selectedUploadFile = ref(null)
const mediaInfo = ref(null)
const mediaInfoStreamController = ref(null)
const router = useRouter()

const uploadRequest = ref({
  title: '',
  type: 'movie',
  year: new Date().getFullYear(),
  description: ''
})

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
    posterUrl: metadata.posterUrl || item.posterUrl || item.poster?.url || '',
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
    posterUrl: metadata.posterUrl || item.posterUrl || item.poster?.url || '',
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
    posterUrl: metadata.posterUrl || item.posterUrl || item.poster?.url || '',
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

function displayValue(value) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return 'N/A'
  }
  return String(value)
}

function formatBytes(bytes) {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value < 0) {
    return 'N/A'
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex += 1
  }

  return `${size.toFixed(size >= 100 ? 0 : 1)} ${units[unitIndex]}`
}

function formatSpeed(bytesPerSecond) {
  const value = Number(bytesPerSecond)
  if (!Number.isFinite(value) || value < 0) {
    return 'N/A'
  }

  return `${formatBytes(value)}/s`
}

function formatImportMediaStatus(value) {
  if (!value) {
    return 'N/A'
  }

  return String(value)
    .split('_')
    .filter(Boolean)
    .map((token) => token.charAt(0) + token.slice(1).toLowerCase())
    .join(' ')
}

const mediaDownloadPercent = computed(() => {
  const total = Number(mediaInfo.value?.fileSize)
  const downloaded = Number(mediaInfo.value?.bytesDownloaded)

  if (!Number.isFinite(total) || total <= 0 || !Number.isFinite(downloaded) || downloaded < 0) {
    return 0
  }

  return Math.max(0, Math.min(100, (downloaded / total) * 100))
})

function resolveMediaUrl(value) {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  return `${API_GATEWAY}/${String(value).replace(/^\/+/, '')}`
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
  const normalizedSeries = normalizeSeriesItem(seriesItem)
  router.push({
    path: `/series/${normalizedSeries.seriesId}`,
    query: { title: normalizedSeries.title }
  })
}

function openMovie(movieItem) {
  const normalizedMovie = normalizeMediaItem(movieItem, 'movie')
  router.push({
    path: `/movie/${normalizedMovie.mediaId || normalizedMovie.id}`,
    query: { title: normalizedMovie.title }
  })
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

function openTorrentSearchPage() {
  router.push({ path: '/search' })
}

function openTorrentQueuePage() {
  router.push({ path: '/torrents' })
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

function closeMediaInfoStream() {
  const controller = mediaInfoStreamController.value
  if (!controller) {
    return
  }

  controller.abort()
  mediaInfoStreamController.value = null
}

function openMediaInfoStream(mediaItemId) {
  if (!mediaItemId) {
    mediaInfo.value = null
    return
  }

  closeMediaInfoStream()
  const controller = new AbortController()
  mediaInfoStreamController.value = controller

  streamMediaItemInfo(mediaItemId, {
    signal: controller.signal,
    onUpdate: (update) => {
      mediaInfo.value = update
    }
  }).catch((err) => {
    if (controller.signal.aborted) {
      return
    }

    mediaInfoStreamController.value = null
    error.value = err?.message || 'Failed to open media info stream.'
  })
}

async function startPlayback(item) {
  activeMedia.value = item
  selectedCaption.value = 'off'
  error.value = ''
  mediaInfo.value = null

  const mediaItemId = resolveMediaId(item)
  openMediaInfoStream(mediaItemId)

  try {
    manifest.value = await streamManifest(mediaItemId, item.streamUrl)
    tracks.value = (manifest.value.captions || []).map((track) => ({
      ...track,
      url: streamCaptionsUrl(mediaItemId, track.language)
    }))
    await nextTick()
    applyCaptionTrack()
  } catch (err) {
    closeMediaInfoStream()
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
})

onBeforeUnmount(() => {
  closeMediaInfoStream()
})
</script>