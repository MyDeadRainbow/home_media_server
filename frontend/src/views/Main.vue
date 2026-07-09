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
      <button type="button" @click="openTorrentSearchPage">Open Search Page</button>
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
            </div>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<!-- JavaScript -->
<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MediaCard from '../components/MediaCard.vue'
import {
  searchCatalogEpisodes,
  searchCatalogMovies,
  searchCatalogSeasons,
  searchCatalogSeries,
  streamCaptionsUrl,
  streamManifest,
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
})
</script>