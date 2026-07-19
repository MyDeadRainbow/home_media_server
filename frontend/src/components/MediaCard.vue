<template>
  <article class="media-card">
    <div class="poster-frame">
      <img class="poster" :src="posterSource" :alt="displayTitle" />
    </div>
    <div class="content">
      <div class="meta-row">
        <span class="type">{{ displayType }}</span>
        <span class="year">{{ releaseYear }}</span>
      </div>
      <h3>{{ displayTitle }}</h3>
      <p class="summary">{{ displaySummary }}</p>
      <div class="card-details">
        <span v-for="entry in metadataEntries" :key="entry.label">{{ entry.label }}: {{ entry.value }}</span>
      </div>

      <div v-if="hasEditableMetadata" class="metadata-actions">
        <button
          v-if="!isEditing"
          type="button"
          class="secondary-button"
          :disabled="savingMetadata || requestingSearch"
          @click="beginEdit"
        >
          Edit Metadata
        </button>
        <button
          v-if="!isEditing"
          type="button"
          class="secondary-button"
          :disabled="savingMetadata || requestingSearch"
          @click="searchMetadataAgain"
        >
          Search Metadata Again
        </button>
      </div>

      <form v-if="hasEditableMetadata && isEditing" class="metadata-editor" @submit.prevent="saveMetadata">
        <label>
          Metadata ID
          <input type="text" :value="metadataDraft.metaDataId" disabled />
        </label>
        <label>
          Title
          <input v-model="metadataDraft.title" type="text" />
        </label>
        <label>
          Plot Summary
          <textarea v-model="metadataDraft.plotSummary" rows="3"></textarea>
        </label>
        <label>
          Air Date
          <input v-model="metadataDraft.airDate" type="date" />
        </label>
        <label>
          Rating
          <input v-model="metadataDraft.rating" type="number" min="0" max="10" step="0.1" />
        </label>
        <label>
          Status
          <select v-model="metadataDraft.status">
            <option v-for="status in metadataStatuses" :key="status" :value="status">{{ status }}</option>
          </select>
        </label>
        <label>
          Message
          <textarea v-model="metadataDraft.message" rows="2"></textarea>
        </label>

        <div class="metadata-editor-actions">
          <button type="submit" :disabled="savingMetadata">{{ savingMetadata ? 'Saving...' : 'Save Metadata' }}</button>
          <button type="button" class="secondary-button" :disabled="savingMetadata" @click="cancelEdit">Cancel</button>
        </div>
      </form>

      <p v-if="metadataStatusMessage" class="status metadata-message">{{ metadataStatusMessage }}</p>
      <p v-if="metadataError" class="error metadata-message">{{ metadataError }}</p>

      <button v-if="showAction" type="button" @click="emitAction">{{ actionLabel }}</button>
    </div>
  </article>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { requestMetadataSearch, updateMetadata } from '../api'

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

const emit = defineEmits(['action', 'play', 'metadata-updated'])

const metadataStatuses = ['PENDING', 'COMPLETE', 'INCOMPLETE', 'NOT_FOUND', 'ERROR']
const isEditing = ref(false)
const savingMetadata = ref(false)
const requestingSearch = ref(false)
const metadataError = ref('')
const metadataStatusMessage = ref('')
const currentMetadata = ref(createDefaultMetadata())
const metadataDraft = reactive(createDefaultMetadata())

function createDefaultMetadata() {
  return {
    metaDataId: '',
    title: '',
    plotSummary: '',
    airDate: '',
    rating: '',
    status: 'PENDING',
    message: ''
  }
}

function normalizeDateInput(value) {
  if (value === null || value === undefined) {
    return ''
  }

  const raw = String(value).trim()
  if (!raw) {
    return ''
  }

  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return raw
  }

  const parsed = new Date(raw)
  if (Number.isNaN(parsed.getTime())) {
    return ''
  }

  return parsed.toISOString().slice(0, 10)
}

function normalizeRatingInput(value) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return ''
  }

  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? String(parsed) : ''
}

function extractMetadataFromItem(item) {
  const metadataBlock = item?.metaData || item?.metadata || item?.meta || {}
  const airDateCandidate = metadataBlock.airDate || metadataBlock.releaseDate || item?.releaseDate
  const ratingCandidate = metadataBlock.rating ?? item?.rating

  return {
    metaDataId: metadataBlock.metaDataId || item?.metaDataId || '',
    title: metadataBlock.title || item?.title || '',
    plotSummary: metadataBlock.plotSummary || item?.plotSummary || item?.description || '',
    airDate: normalizeDateInput(airDateCandidate),
    rating: normalizeRatingInput(ratingCandidate),
    status: metadataBlock.status || 'PENDING',
    message: metadataBlock.message || ''
  }
}

function patchDraftFromCurrent() {
  metadataDraft.metaDataId = currentMetadata.value.metaDataId || ''
  metadataDraft.title = currentMetadata.value.title || ''
  metadataDraft.plotSummary = currentMetadata.value.plotSummary || ''
  metadataDraft.airDate = currentMetadata.value.airDate || ''
  metadataDraft.rating = currentMetadata.value.rating || ''
  metadataDraft.status = currentMetadata.value.status || 'PENDING'
  metadataDraft.message = currentMetadata.value.message || ''
}

watch(
  () => props.item,
  (item) => {
    currentMetadata.value = extractMetadataFromItem(item)
    patchDraftFromCurrent()
    isEditing.value = false
    metadataError.value = ''
    metadataStatusMessage.value = ''
  },
  { immediate: true, deep: true }
)

function emitAction() {
  emit('action', actionItem.value)
  emit('play', actionItem.value)
}

function beginEdit() {
  metadataError.value = ''
  metadataStatusMessage.value = ''
  patchDraftFromCurrent()
  isEditing.value = true
}

function cancelEdit() {
  patchDraftFromCurrent()
  metadataError.value = ''
  metadataStatusMessage.value = ''
  isEditing.value = false
}

function buildMetadataPayload() {
  const parsedRating = Number.parseFloat(metadataDraft.rating)

  return {
    metaDataId: metadataDraft.metaDataId,
    title: metadataDraft.title || '',
    plotSummary: metadataDraft.plotSummary || '',
    airDate: metadataDraft.airDate || null,
    rating: Number.isFinite(parsedRating) ? parsedRating : null,
    status: metadataDraft.status || 'PENDING',
    message: metadataDraft.message || ''
  }
}

async function saveMetadata() {
  if (!metadataDraft.metaDataId || savingMetadata.value) {
    return
  }

  savingMetadata.value = true
  metadataError.value = ''
  metadataStatusMessage.value = ''

  try {
    const payload = buildMetadataPayload()
    const updated = await updateMetadata(metadataDraft.metaDataId, payload)
    const normalized = extractMetadataFromItem({
      ...props.item,
      metaData: updated,
      metadata: updated
    })

    currentMetadata.value = normalized
    patchDraftFromCurrent()
    isEditing.value = false
    metadataStatusMessage.value = 'Metadata updated.'
    emit('metadata-updated', {
      item: actionItem.value,
      metadata: {
        ...updated,
        airDate: normalized.airDate,
        rating: normalized.rating ? Number.parseFloat(normalized.rating) : null
      }
    })
  } catch (err) {
    metadataError.value = err?.message || 'Failed to update metadata.'
  } finally {
    savingMetadata.value = false
  }
}

async function searchMetadataAgain() {
  if (!currentMetadata.value.metaDataId || requestingSearch.value) {
    return
  }

  requestingSearch.value = true
  metadataError.value = ''
  metadataStatusMessage.value = ''

  try {
    await requestMetadataSearch(currentMetadata.value.metaDataId)
    currentMetadata.value = {
      ...currentMetadata.value,
      status: 'PENDING'
    }
    patchDraftFromCurrent()
    metadataStatusMessage.value = 'Metadata search requested.'
    emit('metadata-updated', {
      item: actionItem.value,
      metadata: {
        ...currentMetadata.value,
        rating: currentMetadata.value.rating ? Number.parseFloat(currentMetadata.value.rating) : null
      }
    })
  } catch (err) {
    metadataError.value = err?.message || 'Failed to request metadata search.'
  } finally {
    requestingSearch.value = false
  }
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

const hasEditableMetadata = computed(() => Boolean(currentMetadata.value.metaDataId))

const displayTitle = computed(() => currentMetadata.value.title || props.item.title || 'Untitled')

const displaySummary = computed(() => {
  return currentMetadata.value.plotSummary
    || props.item.plotSummary
    || props.item.description
    || 'No description available.'
})

const displayReleaseDate = computed(() => currentMetadata.value.airDate || props.item.releaseDate || '')

const displayRating = computed(() => {
  if (currentMetadata.value.rating !== '') {
    return currentMetadata.value.rating
  }
  return props.item.rating
})

const actionItem = computed(() => {
  const metadata = {
    ...(props.item.metaData || props.item.metadata || {}),
    metaDataId: currentMetadata.value.metaDataId,
    title: displayTitle.value,
    plotSummary: currentMetadata.value.plotSummary,
    airDate: currentMetadata.value.airDate || null,
    rating: displayRating.value === '' ? null : Number.parseFloat(displayRating.value),
    status: currentMetadata.value.status,
    message: currentMetadata.value.message
  }

  return {
    ...props.item,
    title: displayTitle.value,
    plotSummary: displaySummary.value,
    description: displaySummary.value,
    releaseDate: displayReleaseDate.value || props.item.releaseDate,
    rating: displayRating.value === '' ? null : Number.parseFloat(displayRating.value),
    metaData: metadata,
    metadata
  }
})

const formattedReleaseDate = computed(() => {
  if (!displayReleaseDate.value) {
    return ''
  }

  const parsed = new Date(displayReleaseDate.value)
  if (Number.isNaN(parsed.getTime())) {
    return String(displayReleaseDate.value)
  }

  return parsed.toLocaleDateString()
})

const formattedRating = computed(() => {
  const parsed = Number.parseFloat(displayRating.value)
  if (!Number.isFinite(parsed)) {
    return 'N/A'
  }
  return parsed.toFixed(1)
})

const metadataEntries = computed(() => {
  const entries = []

  if (displayReleaseDate.value) {
    entries.push({ label: 'Release', value: formattedReleaseDate.value })
  }

  if (displayRating.value !== null && displayRating.value !== undefined && displayRating.value !== '') {
    entries.push({ label: 'Rating', value: formattedRating.value })
  }

  if (currentMetadata.value.status) {
    entries.push({ label: 'Metadata', value: currentMetadata.value.status })
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

  if (displayReleaseDate.value) {
    const parsed = new Date(displayReleaseDate.value)
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
