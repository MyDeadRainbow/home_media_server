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
        <select v-model="importRequest.type">
          <option value="movie">Movie</option>
          <option value="series">Series</option>
        </select>
        <input v-model="importRequest.quality" placeholder="Quality (1080p/4k)" />
        <button @click="runImport">Find + Download Torrent</button>
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
            <track v-for="track in tracks" :key="track.language" kind="subtitles" :label="track.label" :src="track.url"
              :srclang="track.language" :default="track.language === selectedCaption" />
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
import { nextTick, onMounted, ref } from 'vue'
import MediaCard from './components/MediaCard.vue'
import { createMedia, importMedia, searchMedia, streamCaptionsUrl, streamManifest, uploadMediaFile, API_GATEWAY } from './api'

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

const newMedia = ref({
  title: '',
  type: 'movie',
  year: new Date().getFullYear()
})

const importRequest = ref({
  title: '',
  type: 'movie',
  quality: '1080p'
})

const uploadRequest = ref({
  title: '',
  type: 'movie',
  year: new Date().getFullYear(),
  description: ''
})

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
  if (!importRequest.value.title) {
    error.value = 'Title is required to import media.'
    return
  }

  error.value = ''
  try {
    const result = await importMedia(importRequest.value)
    importStatus.value = `Status: ${result.status} | Scan passed: ${result.virusScanPassed}`
  } catch (err) {
    error.value = err.message
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
    // uploadStatus.value = 'File uploaded. Saving metadata in catalog...'

    // await createMedia({
    //   title: uploadRequest.value.title,
    //   type: uploadRequest.value.type,
    //   year: uploadRequest.value.year,
    //   description: uploadRequest.value.description,
    //   streamUrl: uploadResult.playbackUrl
    // })

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

onMounted(loadMedia)
</script>
