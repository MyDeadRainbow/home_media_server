<script setup>
import { onMounted, ref } from 'vue'
import {
  deleteTorrent,
  getTorrentInfo,
  pauseTorrent,
  resumeTorrent
} from '../api'

const torrents = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('')
const actionInFlight = ref({})

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
  return `${formatBytes(bytesPerSecond)}/s`
}

function getCompletionPercent(item) {
  const total = Number(item?.totalSize)
  const downloaded = Number(item?.downloadedSize)

  if (!Number.isFinite(total) || total <= 0 || !Number.isFinite(downloaded) || downloaded < 0) {
    return 0
  }

  return Math.max(0, Math.min(100, (downloaded / total) * 100))
}

async function loadTorrentInfo() {
  loading.value = true
  error.value = ''

  try {
    const response = await getTorrentInfo()
    torrents.value = Array.isArray(response) ? response : []
  } catch (err) {
    error.value = err?.message || 'Failed to load torrent information.'
  } finally {
    loading.value = false
  }
}

function setInFlight(infoHash, value) {
  actionInFlight.value = {
    ...actionInFlight.value,
    [infoHash]: value
  }
}

function isActionInFlight(infoHash) {
  return Boolean(actionInFlight.value[infoHash])
}

async function runTorrentAction(actionName, infoHash, actionFn) {
  if (!infoHash || isActionInFlight(infoHash)) {
    return
  }

  setInFlight(infoHash, true)
  error.value = ''
  status.value = ''

  try {
    const ok = await actionFn(infoHash)
    if (!ok) {
      throw new Error(`Unable to ${actionName} torrent.`)
    }

    status.value = `Torrent ${actionName} request sent.`
    await loadTorrentInfo()
  } catch (err) {
    error.value = err?.message || `Failed to ${actionName} torrent.`
  } finally {
    setInFlight(infoHash, false)
  }
}

function pause(infoHash) {
  return runTorrentAction('pause', infoHash, pauseTorrent)
}

function resume(infoHash) {
  return runTorrentAction('resume', infoHash, resumeTorrent)
}

function remove(infoHash) {
  return runTorrentAction('delete', infoHash, deleteTorrent)
}

onMounted(() => {
  loadTorrentInfo()
})
</script>

<template>
  <div class="page torrent-info-page">
    <header class="hero">
      <h1>Torrent Queue</h1>
      <p>Inspect active torrents and control pause, resume, and delete operations.</p>
    </header>

    <section class="panel controls">
      <div class="torrent-toolbar">
        <h2>TorrentInfoResponse List</h2>
        <button type="button" @click="loadTorrentInfo" :disabled="loading">Refresh</button>
      </div>

      <p v-if="status" class="status">{{ status }}</p>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="loading" class="muted">Loading torrent information...</p>
      <p v-else-if="!torrents.length" class="muted">No torrents found.</p>

      <div v-else class="torrent-list">
        <article v-for="torrent in torrents" :key="torrent.infoHash" class="torrent-card">
          <header class="torrent-card-header">
            <h3>{{ torrent.name || 'Unnamed Torrent' }}</h3>
            <span class="queue-chip">Queue #{{ torrent.queuePosition }}</span>
          </header>

          <p class="torrent-hash">{{ torrent.infoHash }}</p>

          <div class="progress-wrap">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: `${getCompletionPercent(torrent)}%` }"></div>
            </div>
            <span>{{ getCompletionPercent(torrent).toFixed(1) }}%</span>
          </div>

          <div class="torrent-grid">
            <p><strong>Total:</strong> {{ formatBytes(torrent.totalSize) }}</p>
            <p><strong>Downloaded:</strong> {{ formatBytes(torrent.downloadedSize) }}</p>
            <p><strong>Download:</strong> {{ formatSpeed(torrent.downloadSpeed) }}</p>
            <p><strong>Upload:</strong> {{ formatSpeed(torrent.uploadSpeed) }}</p>
            <p><strong>Peers:</strong> {{ torrent.numPeers }}</p>
          </div>

          <div class="torrent-actions">
            <button
              type="button"
              class="secondary-button"
              :disabled="isActionInFlight(torrent.infoHash)"
              @click="pause(torrent.infoHash)"
            >
              Pause
            </button>
            <button
              type="button"
              class="secondary-button"
              :disabled="isActionInFlight(torrent.infoHash)"
              @click="resume(torrent.infoHash)"
            >
              Resume
            </button>
            <button
              type="button"
              class="danger-button"
              :disabled="isActionInFlight(torrent.infoHash)"
              @click="remove(torrent.infoHash)"
            >
              Delete
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
