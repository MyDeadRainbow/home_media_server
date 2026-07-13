<template>
  <article class="media-card">
    <div class="poster-frame">
      <img class="poster" :src="posterSource" :alt="item.title" />
    </div>
    <div class="content">
      <div class="meta-row">
        <span class="type">{{ displayType }}</span>
        <span class="year">{{ releaseYear }}</span>
      </div>
      <h3>{{ item.title }}</h3>
      <p class="summary">{{ item.plotSummary || item.description || 'No description available.' }}</p>
      <div class="card-details">
        <span v-for="entry in metadataEntries" :key="entry.label">{{ entry.label }}: {{ entry.value }}</span>
      </div>
      <button v-if="showAction" type="button" @click="emitAction">{{ actionLabel }}</button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  actionLabel: {
    type: String,
    default: 'Stream'
  },
  showAction: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['action', 'play'])

function emitAction() {
  emit('action', props.item)
  emit('play', props.item)
}

function detectBase64ImageMime(base64) {
  if (base64.startsWith('/9j/')) {
    return 'image/jpeg'
  }

  if (base64.startsWith('iVBOR')) {
    return 'image/png'
  }

  if (base64.startsWith('R0lGOD')) {
    return 'image/gif'
  }

  if (base64.startsWith('UklGR')) {
    return 'image/webp'
  }

  return 'image/jpeg'
}

function normalizePosterSrc(value) {
  if (value === null || value === undefined) {
    return ''
  }

  const raw = String(value).trim()
  if (!raw) {
    return ''
  }

  if (
    raw.startsWith('data:image/')
    || /^https?:\/\//i.test(raw)
    || raw.startsWith('/')
    || raw.startsWith('./')
    || raw.startsWith('../')
    || raw.startsWith('blob:')
  ) {
    return raw
  }

  const base64Payload = raw.replace(/\s+/g, '')
  const looksLikeBase64 = /^[A-Za-z0-9+/=]+$/.test(base64Payload)

  if (!looksLikeBase64 || base64Payload.length < 32) {
    return raw
  }

  return `data:${detectBase64ImageMime(base64Payload)};base64,${base64Payload}`
}

function bytesToBase64(bytes) {
  if (!Array.isArray(bytes) || !bytes.length) {
    return ''
  }

  let binary = ''
  const chunkSize = 0x8000
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const chunk = bytes.slice(i, i + chunkSize)
    binary += String.fromCharCode(...chunk)
  }

  try {
    return btoa(binary)
  } catch {
    return ''
  }
}

function extractPosterImageData(item) {
  const candidate = item?.poster?.imageData ?? item?.imageData ?? item?.posterImageData

  if (typeof candidate === 'string') {
    return candidate
  }

  if (Array.isArray(candidate)) {
    return bytesToBase64(candidate)
  }

  if (candidate && Array.isArray(candidate.data)) {
    return bytesToBase64(candidate.data)
  }

  return ''
}

const displayType = computed(() => {
  const type = String(props.item.type || '').trim()
  if (!type) {
    return 'Unknown'
  }
  return `${type.slice(0, 1).toUpperCase()}${type.slice(1).toLowerCase()}`
})

const formattedReleaseDate = computed(() => {
  if (!props.item.releaseDate) {
    return ''
  }

  const parsed = new Date(props.item.releaseDate)
  if (Number.isNaN(parsed.getTime())) {
    return String(props.item.releaseDate)
  }

  return parsed.toLocaleDateString()
})

const formattedRating = computed(() => {
  const parsed = Number.parseFloat(props.item.rating)
  if (!Number.isFinite(parsed)) {
    return 'N/A'
  }
  return parsed.toFixed(1)
})

const metadataEntries = computed(() => {
  const entries = []

  if (props.item.releaseDate) {
    entries.push({ label: 'Release', value: formattedReleaseDate.value })
  }

  if (props.item.rating !== null && props.item.rating !== undefined) {
    entries.push({ label: 'Rating', value: formattedRating.value })
  }

  if (props.item.seriesName) {
    entries.push({ label: 'Series', value: props.item.seriesName })
  }

  if (props.item.seasonName) {
    entries.push({ label: 'Season', value: props.item.seasonName })
  }

  if (props.item.seasonNumber !== null && props.item.seasonNumber !== undefined) {
    entries.push({ label: 'Season #', value: props.item.seasonNumber })
  }

  if (props.item.episodeNumber !== null && props.item.episodeNumber !== undefined) {
    entries.push({ label: 'Episode #', value: props.item.episodeNumber })
  }

  if (props.item.mediaId) {
    entries.push({ label: 'Media ID', value: props.item.mediaId })
  }

  if (props.item.streamUrl) {
    entries.push({ label: 'Stream', value: props.item.streamUrl })
  }

  return entries
})

const releaseYear = computed(() => {
  if (props.item.year) {
    return props.item.year
  }

  if (props.item.releaseDate) {
    const parsed = new Date(props.item.releaseDate)
    if (!Number.isNaN(parsed.getTime())) {
      return parsed.getFullYear()
    }
  }

  return 'N/A'
})

const posterSource = computed(() => {
  const imageData = extractPosterImageData(props.item)
  const normalized = normalizePosterSrc(imageData || props.item.posterUrl)
  return normalized || fallbackPoster
})

const fallbackPoster = 'https://picsum.photos/seed/fallback/320/180'
</script>
