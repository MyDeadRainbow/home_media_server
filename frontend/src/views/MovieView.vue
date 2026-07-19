<template>
  <div class="page">
    <header class="hero">
      <h1>{{ movie?.title || 'Movie' }}</h1>
      <p>{{ movie?.plotSummary || 'No plot summary available.' }}</p>
    </header>

    <div v-if="movie" class="movie-detail">
      <section class="panel library-subsection movie-overview-panel">
        <div class="library-header">
          <h2>Movie Details</h2>
          <button type="button" class="secondary-button" @click="goBack">Back to Library</button>
        </div>
        <MediaCard :item="movie" action-label="Stream Movie" @action="startPlayback" />
        <div class="detail-grid">
          <div v-for="entry in movieDetails" :key="entry.label" class="detail-row">
            <strong>{{ entry.label }}:</strong>
            <span>{{ entry.value }}</span>
          </div>
        </div>
      </section>

      <section class="panel player inline-player movie-stream-panel">
        <h2>Stream Window</h2>
        <p v-if="!activeMedia">Select this movie to begin streaming.</p>
        <div v-else>
          <h3>{{ activeMedia.title || 'Movie' }}</h3>
          <p class="player-meta">
            <span>Type: {{ activeMedia.type || 'movie' }}</span>
            <span>Release: {{ formatReleaseDate(activeMedia.releaseDate) || 'N/A' }}</span>
            <span>Rating: {{ formatRating(activeMedia.rating) }}</span>
            <span>Media ID: {{ displayValue(activeMedia.mediaId || activeMedia.id) }}</span>
          </p>
          <p class="player-summary">{{ activeMedia.plotSummary || activeMedia.description || 'No description available.' }}</p>
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
          <div class="detail-grid">
            <div v-for="entry in activeMovieDetails" :key="entry.label" class="detail-row">
              <strong>{{ entry.label }}:</strong>
              <span>{{ entry.value }}</span>
            </div>
          </div>
        </div>
      </section>

      <p v-if="error" class="error">{{ error }}</p>
    </div>

    <p v-else class="error">Movie not found.</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MediaCard from '../components/MediaCard.vue'
import { API_GATEWAY, searchCatalogMovies, streamCaptionsUrl, streamManifest, streamMediaItemInfo } from '../api'

const route = useRoute()
const router = useRouter()

const movie = ref(null)
const manifest = ref(null)
const tracks = ref([])
const activeMedia = ref(null)
const selectedCaption = ref('off')
const error = ref('')
const player = ref(null)
const mediaInfo = ref(null)
const mediaInfoStreamController = ref(null)

function displayValue(value) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return 'N/A'
  }
  return String(value)
}

function pickMetaBlock(item) {
  return item?.metaData || item?.metadata || item?.meta || {}
}

function normalizeMovieItem(item) {
  const metadata = pickMetaBlock(item)
  const releaseDate = metadata.releaseDate || item.releaseDate || null
  const numericRating = Number.parseFloat(metadata.rating ?? item.rating)

  return {
    mediaId: item.mediaId || item.id,
    id: item.id || item.mediaId,
    type: item.type || 'movie',
    title: metadata.title || item.title || item.name || 'Movie',
    plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
    description: metadata.plotSummary || item.plotSummary || item.description || '',
    releaseDate,
    year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : null),
    rating: Number.isFinite(numericRating) ? numericRating : null,
    posterUrl: metadata.posterUrl || item.posterUrl || '',
    streamUrl: item.streamUrl || item.filePath || '',
    metadata
  }
}

function buildDetailEntries(item, idLabel = 'ID') {
  if (!item) {
    return []
  }

  const baseEntries = [
    { label: idLabel, value: displayValue(item.mediaId || item.id) },
    { label: 'Type', value: displayValue(item.type) },
    { label: 'Title', value: displayValue(item.title) },
    { label: 'Release Date', value: displayValue(formatReleaseDate(item.releaseDate) || 'N/A') },
    { label: 'Year', value: displayValue(item.year) },
    { label: 'Rating', value: formatRating(item.rating) },
    { label: 'Poster URL', value: displayValue(item.posterUrl) },
    { label: 'Stream URL', value: displayValue(item.streamUrl) },
    { label: 'Summary', value: displayValue(item.plotSummary || item.description) }
  ]

  const metadataEntries = Object.entries(item.metadata || {}).map(([key, value]) => ({
    label: `meta.${key}`,
    value: displayValue(value)
  }))

  return [...baseEntries, ...metadataEntries]
}

const movieDetails = computed(() => buildDetailEntries(movie.value, 'Movie ID'))
const activeMovieDetails = computed(() => buildDetailEntries(activeMedia.value, 'Playback Movie ID'))

function formatReleaseDate(value) {
  if (!value) return ''
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return String(value)
  return parsed.toLocaleDateString()
}

function formatRating(value) {
  const parsed = Number.parseFloat(value)
  if (!Number.isFinite(parsed)) return 'N/A'
  return parsed.toFixed(1)
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

function resolveMediaUrl(value) {
  if (!value) return ''
  if (/^https?:\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }
  return `${API_GATEWAY}/${String(value).replace(/^\/+/, '')}`
}

async function startPlayback(item) {
  activeMedia.value = item
  selectedCaption.value = 'off'
  error.value = ''
  mediaInfo.value = null

  const mediaItemId = item?.mediaId || item?.id
  openMediaInfoStream(mediaItemId)

  try {
    manifest.value = await streamManifest(mediaItemId, item.streamUrl)
    tracks.value = (manifest.value?.captions || []).map((track) => ({
      ...track,
      url: streamCaptionsUrl(mediaItemId, track.language)
    }))
    await nextTick()
    applyCaptionTrack()
  } catch (err) {
    closeMediaInfoStream()
    error.value = err.message || 'Failed to start playback.'
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

function goBack() {
  router.push('/')
}

async function loadMovie() {
  const movieId = route.params.movieId
  const title = route.query.title

  if (!movieId) {
    return
  }

  error.value = ''

  try {
    const movies = await searchCatalogMovies('')
    const movieList = Array.isArray(movies) ? movies : []
    const matchingMovie = movieList.find((item) => String(item.mediaId || item.id) === String(movieId))

    movie.value = normalizeMovieItem(matchingMovie || {
      mediaId: movieId,
      title,
      type: 'movie'
    })
  } catch (err) {
    movie.value = normalizeMovieItem({ mediaId: movieId, title, type: 'movie' })
    error.value = err.message || 'Failed to load movie metadata.'
  }
}

onMounted(() => {
  loadMovie()
})

onBeforeUnmount(() => {
  closeMediaInfoStream()
})
</script>
