<template>
  <div class="page">
    <header class="hero">
      <h1>Home Media Server</h1>
      <p>Search, ingest, and stream your movie and series library with subtitles.</p>
    </header>

    <section class="panel controls">
      <div class="search-row">
        <input v-model="query" placeholder="Search title, type, description" @keyup.enter="loadMedia" />
        <button @click="loadMedia">Search</button>
      </div>

      <div ref="acquisitionSearchContainer" class="acquisition-search">
        <label for="acquisition-search-box">Acquisition Search</label>
        <div class="acquisition-actions">
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

      <div class="form-grid">
        <input v-model="newMedia.title" placeholder="New media title" />
        <select v-model="newMedia.type">
          <option value="movie">Movie</option>
          <option value="series">Series</option>
        </select>
        <input v-model.number="newMedia.year" type="number" placeholder="Year" />
        <button @click="addMedia">Add Catalog Item</button>
      </div>

      <div class="form-grid">
        <input v-model="importRequest.title" placeholder="Import title" />
        <button @click="runImport">Create Import Request</button>
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
        <h2>Media Library</h2>
        <div class="cards">
          <MediaCard v-for="item in media" :key="item.id" :item="item" @play="startPlayback" />
        </div>
      </section>

      <section class="panel player">
        <h2>Stream Window</h2>
        <p v-if="!activeMedia">Select media to begin streaming.</p>
        <div v-else>
          <h3>{{ activeMedia.title }}</h3>
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
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import MediaCard from './components/MediaCard.vue'
import {
  createMedia,
  importMedia,
  importStreamMedia,
  searchAcquisitionStream,
  searchMedia,
  streamCaptionsUrl,
  streamManifest,
  uploadMediaFile,
  API_GATEWAY
} from './api'

const media = ref([])
const query = ref('')
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
const acquisitionSortBy = ref('seeders')
const importInFlightByMagnet = ref({})
let acquisitionSearchAbortController = null

const newMedia = ref({
  title: '',
  type: 'movie',
  year: new Date().getFullYear()
})

const importRequest = ref({
  title: ''
})

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

async function loadMedia() {
  error.value = ''
  try {
    media.value = await searchMedia(query.value)
  } catch (err) {
    error.value = err.message
  }
}

async function addMedia() {
  if (!newMedia.value.title) {
    error.value = 'Title is required to add media.'
    return
  }

  error.value = ''
  try {
    await createMedia(newMedia.value)
    newMedia.value.title = ''
    await loadMedia()
  } catch (err) {
    error.value = err.message
  }
}

async function runImport() {
  const title = importRequest.value.title.trim()
  if (!title) {
    error.value = 'Title is required to import media.'
    return
  }

  error.value = ''
  importStatus.value = 'Creating import request...'
  try {
    const created = await importMedia({ title })
    importStatus.value = created
      ? `Import request created for "${title}".`
      : `Import request could not be created for "${title}".`
    importRequest.value.title = ''
  } catch (err) {
    importStatus.value = ''
    error.value = err.message
  }
}

function startAcquisitionSearch() {
  const term = acquisitionQuery.value.trim()

  if (!term) {
    resetAcquisitionResults()
    showAcquisitionPopup.value = false
    return
  }

  fetchAcquisitionResults(term)
}

async function fetchAcquisitionResults(term) {
  if (acquisitionSearchAbortController) {
    acquisitionSearchAbortController.abort()
  }

  acquisitionSearchAbortController = new AbortController()
  acquisitionLoading.value = true
  error.value = ''
  showAcquisitionPopup.value = true
  resetAcquisitionResults()

  try {
    await searchAcquisitionStream(term, {
      signal: acquisitionSearchAbortController.signal,
      onItem: (item) => {
        if (!item || !item.title) {
          return
        }

        if (!isDuplicateAcquisitionResult(item)) {
          const source = item.source || 'Unknown Source'
          const existingSourceItems = acquisitionResultsBySource.value[source] || []
          acquisitionResultsBySource.value = {
            ...acquisitionResultsBySource.value,
            [source]: [...existingSourceItems, item]
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
  importRequest.value.title = result.title || ''
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
      magnetLink: result.magnetLink
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
    await loadMedia()
  } catch (err) {
    uploadStatus.value = ''
    error.value = err.message
  }
}

async function startPlayback(item) {
  activeMedia.value = item
  selectedCaption.value = 'off'
  error.value = ''

  try {
    manifest.value = await streamManifest(item.id, item.streamUrl)
    tracks.value = (manifest.value.captions || []).map((track) => ({
      ...track,
      url: streamCaptionsUrl(item.id, track.language)
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
  loadMedia()
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  if (acquisitionSearchAbortController) {
    acquisitionSearchAbortController.abort()
  }
})
</script>
