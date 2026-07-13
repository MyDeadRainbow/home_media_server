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
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MediaCard from '../components/MediaCard.vue'
import { API_GATEWAY, searchCatalogMovies, streamCaptionsUrl, streamManifest } from '../api'

const route = useRoute()
const router = useRouter()

const movie = ref(null)
const manifest = ref(null)
const tracks = ref([])
const activeMedia = ref(null)
const selectedCaption = ref('off')
const error = ref('')
const player = ref(null)

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

  try {
    manifest.value = await streamManifest(item.mediaId || item.id, item.streamUrl)
    tracks.value = (manifest.value?.captions || []).map((track) => ({
      ...track,
      url: streamCaptionsUrl(item.mediaId || item.id, track.language)
    }))
    await nextTick()
    applyCaptionTrack()
  } catch (err) {
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
</script>
