<template>
  <div class="page">
    <header class="hero">
      <h1>{{ movie?.title || 'Movie' }}</h1>
      <p v-if="movie?.plotSummary">{{ movie.plotSummary }}</p>
    </header>

    <div v-if="movie" class="movie-detail">
        <section class="panel library-subsection movie-overview-panel">
            <div class="library-header">
                <h2>Movie Details</h2>
                <button type="button" class="secondary-button" @click="goBack">Back to Library</button>
            </div>
            <MediaCard :item="movie" :show-action="false" />
        </section>

        <section class="panel player inline-player movie-stream-panel">
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

      <p v-else class="error">Movie not found.</p>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MediaCard from '../components/MediaCard.vue'
import { streamCaptionsUrl, streamManifest, API_GATEWAY } from '../api'

const route = useRoute()
const router = useRouter()

const movie = ref(null)
const manifest = ref(null)
const tracks = ref([])
const activeMedia = ref(null)
const selectedCaption = ref('off')
const error = ref('')
const player = ref(null)

function normalizeMovieItem(item) {
  const metadata = item?.metaData || item?.metadata || item?.meta || {}
  return {
    mediaId: item.mediaId || item.id,
    id: item.id || item.mediaId,
    type: item.type || 'movie',
    title: metadata.title || item.title || item.name || 'Movie',
    plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
    description: metadata.plotSummary || item.plotSummary || item.description || '',
    releaseDate: metadata.releaseDate || item.releaseDate || null,
    rating: item.rating ?? metadata.rating ?? null,
    posterUrl: metadata.posterUrl || item.posterUrl || ''
  }
}

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

onMounted(() => {
  const movieId = route.params.movieId
  const title = route.query.title
  if (movieId) {
    movie.value = normalizeMovieItem({ mediaId: movieId, title, type: 'movie' })
  }
})
</script>
