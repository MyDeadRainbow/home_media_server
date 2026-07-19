<template>
  <div class="page">
    <header class="hero">
      <h1>{{ series?.title || 'Series' }}</h1>
      <p>{{ series?.plotSummary || 'No plot summary available.' }}</p>
    </header>

    <div v-if="series" class="series-detail">
      <section class="panel library-subsection overview-section">
        <div class="library-header">
          <h2>Series Details</h2>
          <button type="button" class="secondary-button" @click="goBack">Back to Library</button>
        </div>
        <MediaCard :item="series" :show-action="false" />
        <div class="detail-grid">
          <div v-for="entry in seriesDetails" :key="entry.label" class="detail-row">
            <strong>{{ entry.label }}:</strong>
            <span>{{ entry.value }}</span>
          </div>
        </div>
      </section>

      <section class="panel library-subsection">
        <h3>Seasons</h3>
        <p v-if="!seasons.length" class="muted">No seasons found for this series.</p>
        <div v-else class="season-buttons">
          <button
            v-for="season in seasons"
            :key="season.seasonId || season.id"
            type="button"
            class="secondary-button"
            :class="{ 'season-active': selectedSeasonId === (season.seasonId || season.id) }"
            @click="openSeason(season)"
          >
            {{ season.title || season.name || `Season ${season.seasonNumber || ''}`.trim() || 'Season' }}
          </button>
        </div>

        <div v-if="selectedSeason" class="detail-grid">
          <div v-for="entry in selectedSeasonDetails" :key="entry.label" class="detail-row">
            <strong>{{ entry.label }}:</strong>
            <span>{{ entry.value }}</span>
          </div>
        </div>

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
        <h2>Episode Stream Window</h2>
        <p v-if="!activeMedia">Select an episode to begin streaming.</p>
        <div v-else>
          <h3>{{ activeMedia.title || 'Episode' }}</h3>
          <p class="player-meta">
            <span>Type: {{ activeMedia.type || 'episode' }}</span>
            <span>Release: {{ formatReleaseDate(activeMedia.releaseDate) || 'N/A' }}</span>
            <span>Rating: {{ formatRating(activeMedia.rating) }}</span>
            <span>Season #: {{ displayValue(activeMedia.seasonNumber) }}</span>
            <span>Episode #: {{ displayValue(activeMedia.episodeNumber) }}</span>
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
            <label for="series-caption-select">Closed Captions</label>
            <select id="series-caption-select" v-model="selectedCaption" @change="applyCaptionTrack">
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
            <div v-for="entry in activeEpisodeDetails" :key="entry.label" class="detail-row">
              <strong>{{ entry.label }}:</strong>
              <span>{{ entry.value }}</span>
            </div>
          </div>
        </div>
      </section>

      <p v-if="error" class="error">{{ error }}</p>
    </div>
    <p v-else class="error">Series not found.</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MediaCard from '../components/MediaCard.vue'
import {
  API_GATEWAY,
  searchCatalogEpisodes,
  searchCatalogSeasons,
  searchCatalogSeries,
  streamCaptionsUrl,
  streamManifest,
  streamMediaItemInfo
} from '../api'

const route = useRoute()
const router = useRouter()

const series = ref(null)
const seasons = ref([])
const episodes = ref([])
const selectedSeason = ref(null)
const manifest = ref(null)
const tracks = ref([])
const activeMedia = ref(null)
const selectedCaption = ref('off')
const error = ref('')
const selectedSeasonId = ref(null)
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

function normalizeSeriesItem(item) {
  const metadata = pickMetaBlock(item)
  const releaseDate = metadata.releaseDate || metadata.firstAirDate || item.releaseDate || null
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
    year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : null),
    rating: Number.isFinite(numericRating) ? numericRating : null,
    posterUrl: metadata.posterUrl || item.posterUrl || item.poster?.url || '',
    streamUrl: item.streamUrl || item.filePath || '',
    metadata
  }
}

function normalizeSeasonItem(item, parentSeries) {
  const metadata = pickMetaBlock(item)
  const releaseDate = metadata.releaseDate || metadata.firstAirDate || item.releaseDate || null
  const numericRating = Number.parseFloat(metadata.rating ?? item.rating)

  return {
    id: item.id || item.seasonId,
    mediaId: item.mediaId || item.seasonId || item.id,
    seasonId: item.seasonId || item.id,
    seriesId: item.seriesId || parentSeries?.seriesId,
    type: item.type || 'season',
    title: metadata.title || item.title || item.name || `Season ${item.seasonNumber || ''}`.trim() || 'Season',
    plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
    description: metadata.plotSummary || item.plotSummary || item.description || '',
    seasonName: metadata.title || item.name || null,
    seasonNumber: item.seasonNumber ?? null,
    releaseDate,
    year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : null),
    rating: Number.isFinite(numericRating) ? numericRating : null,
    posterUrl: metadata.posterUrl || item.posterUrl || parentSeries?.posterUrl || item.poster?.url || '',
    streamUrl: item.streamUrl || item.filePath || '',
    metadata
  }
}

function normalizeEpisodeItem(item) {
  const metadata = pickMetaBlock(item)
  const releaseDate = metadata.releaseDate || metadata.airDate || metadata.firstAirDate || item.releaseDate || null
  const numericRating = Number.parseFloat(metadata.rating ?? item.rating)

  return {
    mediaId: item.mediaId || item.id,
    id: item.id || item.mediaId,
    seriesId: item.seriesId || selectedSeason.value?.seriesId || series.value?.seriesId || null,
    seasonId: item.seasonId || selectedSeason.value?.seasonId || null,
    type: item.type || 'episode',
    title: metadata.title || item.title || item.name || 'Episode',
    plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
    description: metadata.plotSummary || item.plotSummary || item.description || '',
    seriesName: item.seriesName || series.value?.title || null,
    seasonName: item.seasonName || selectedSeason.value?.title || null,
    seasonNumber: item.seasonNumber ?? selectedSeason.value?.seasonNumber ?? null,
    episodeNumber: item.episodeNumber ?? null,
    releaseDate,
    year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : null),
    rating: Number.isFinite(numericRating) ? numericRating : null,
    posterUrl: metadata.posterUrl || item.posterUrl || series.value?.posterUrl || item.poster?.url || '',
    streamUrl: item.streamUrl || item.filePath || '',
    metadata
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
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  return `${API_GATEWAY}/${String(value).replace(/^\/+/, '')}`
}

function buildDetailEntries(item, labels = {}) {
  if (!item) {
    return []
  }

  const baseEntries = [
    { label: labels.idLabel || 'ID', value: displayValue(item.mediaId || item.seriesId || item.seasonId || item.id) },
    { label: 'Type', value: displayValue(item.type) },
    { label: 'Title', value: displayValue(item.title) },
    { label: 'Release Date', value: displayValue(formatReleaseDate(item.releaseDate) || 'N/A') },
    { label: 'Year', value: displayValue(item.year) },
    { label: 'Rating', value: formatRating(item.rating) },
    { label: 'Series', value: displayValue(item.seriesName) },
    { label: 'Season', value: displayValue(item.seasonName) },
    { label: 'Season #', value: displayValue(item.seasonNumber) },
    { label: 'Episode #', value: displayValue(item.episodeNumber) },
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

const seriesDetails = computed(() => buildDetailEntries(series.value, { idLabel: 'Series ID' }))
const selectedSeasonDetails = computed(() => buildDetailEntries(selectedSeason.value, { idLabel: 'Season ID' }))
const activeEpisodeDetails = computed(() => buildDetailEntries(activeMedia.value, { idLabel: 'Episode ID' }))

async function loadSeries() {
  const seriesId = route.params.seriesId
  if (!seriesId) {
    return
  }

  try {
    const [seriesResult, seasonsResult] = await Promise.all([
      searchCatalogSeries(''),
      searchCatalogSeasons(seriesId)
    ])

    const seriesList = Array.isArray(seriesResult) ? seriesResult : []
    const matchingSeries = seriesList.find((item) => String(item.seriesId || item.id) === String(seriesId))
    const fallbackSeries = {
      seriesId,
      title: route.query.title || 'Series',
      type: 'series'
    }

    series.value = normalizeSeriesItem(matchingSeries || fallbackSeries)
    seasons.value = (Array.isArray(seasonsResult) ? seasonsResult : [])
      .map((item) => normalizeSeasonItem(item, series.value))
      .sort((left, right) => (left.seasonNumber || 0) - (right.seasonNumber || 0))

    if (seasons.value.length) {
      const firstSeason = seasons.value[0]
      selectedSeasonId.value = firstSeason.seasonId || firstSeason.id
      await openSeason(firstSeason)
    }
  } catch (err) {
    error.value = err.message || 'Failed to load series.'
  }
}

async function openSeason(season) {
  const normalizedSeason = normalizeSeasonItem(season, series.value)
  selectedSeason.value = normalizedSeason
  selectedSeasonId.value = normalizedSeason.seasonId || normalizedSeason.id
  closeMediaInfoStream()
  mediaInfo.value = null
  activeMedia.value = null
  manifest.value = null
  tracks.value = []

  try {
    const result = await searchCatalogEpisodes(series.value?.seriesId, normalizedSeason.seasonId || normalizedSeason.id)
    episodes.value = (Array.isArray(result) ? result : [])
      .map((item) => normalizeEpisodeItem(item))
  } catch (err) {
    error.value = err.message || 'Failed to load episodes.'
  }
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

onMounted(() => {
  loadSeries()
})

onBeforeUnmount(() => {
  closeMediaInfoStream()
})
</script>
