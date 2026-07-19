<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import {
  deleteTorrent,
  getTorrentInfo,
  pauseTorrent,
  resumeTorrent,
  streamTorrentInfo
} from '../api'

const torrents = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('')
const actionInFlight = ref({})
const streamControllers = ref({})
const streamRetryTimers = ref({})

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
    syncTorrentStreams()
  } catch (err) {
    error.value = err?.message || 'Failed to load torrent information.'
  } finally {
    loading.value = false
  }
}

function applyTorrentUpdate(update) {
  const infoHash = update?.infoHash
  if (!infoHash) {
    return
  }

  torrents.value = torrents.value.map((torrent) => {
    if (torrent.infoHash !== infoHash) {
      return torrent
    }

    return {
      ...torrent,
      downloadedSize: update.downloadedSize ?? torrent.downloadedSize,
      uploadSpeed: update.uploadSpeed ?? torrent.uploadSpeed,
      downloadSpeed: update.downloadSpeed ?? torrent.downloadSpeed,
      numPeers: update.numPeers ?? torrent.numPeers,
      importMediaStatus: update.importMediaStatus ?? torrent.importMediaStatus
    }
  })
}

function scheduleStreamReconnect(infoHash, delayMs = 1000) {
  if (!infoHash || streamRetryTimers.value[infoHash]) {
    return
  }

  const timeoutId = setTimeout(() => {
    const { [infoHash]: timerToClear, ...remainingTimers } = streamRetryTimers.value
    clearTimeout(timerToClear)
    streamRetryTimers.value = remainingTimers

    const stillExists = torrents.value.some((torrent) => torrent.infoHash === infoHash)
    if (stillExists) {
      openTorrentStream(infoHash)
    }
  }, delayMs)

  streamRetryTimers.value = {
    ...streamRetryTimers.value,
    [infoHash]: timeoutId
  }
}

function closeTorrentStream(infoHash) {
  const controller = streamControllers.value[infoHash]
  if (controller) {
    controller.abort()
    const { [infoHash]: controllerToClear, ...remainingControllers } = streamControllers.value
    controllerToClear.abort()
    streamControllers.value = remainingControllers
  }

  const timer = streamRetryTimers.value[infoHash]
  if (timer) {
    clearTimeout(timer)
    const { [infoHash]: timerToClear, ...remainingTimers } = streamRetryTimers.value
    clearTimeout(timerToClear)
    streamRetryTimers.value = remainingTimers
  }
}

function closeAllTorrentStreams() {
  Object.keys(streamControllers.value).forEach((infoHash) => {
    closeTorrentStream(infoHash)
  })
  Object.keys(streamRetryTimers.value).forEach((infoHash) => {
    closeTorrentStream(infoHash)
  })
}

function openTorrentStream(infoHash) {
  if (!infoHash || streamControllers.value[infoHash]) {
    return
  }

  const controller = new AbortController()
  streamControllers.value = {
    ...streamControllers.value,
    [infoHash]: controller
  }

  streamTorrentInfo(infoHash, {
    signal: controller.signal,
    onUpdate: (update) => {
      applyTorrentUpdate(update)
    },
    onDone: () => {
      if (controller.signal.aborted) {
        return
      }

      const { [infoHash]: closedController, ...remainingControllers } = streamControllers.value
      if (closedController === controller) {
        streamControllers.value = remainingControllers
      }
      scheduleStreamReconnect(infoHash)
    },
    onError: () => {
      if (controller.signal.aborted) {
        return
      }

      scheduleStreamReconnect(infoHash)
    }
  }).catch((err) => {
    if (controller.signal.aborted) {
      return
    }

    const { [infoHash]: failedController, ...remainingControllers } = streamControllers.value
    if (failedController === controller) {
      streamControllers.value = remainingControllers
    }

    error.value = err?.message || `Failed to open torrent stream for ${infoHash}.`
    scheduleStreamReconnect(infoHash, 2000)
  })
}

function syncTorrentStreams() {
  const infoHashes = new Set(
    torrents.value
      .map((torrent) => torrent?.infoHash)
      .filter((infoHash) => Boolean(infoHash))
  )

  Object.keys(streamControllers.value).forEach((infoHash) => {
    if (!infoHashes.has(infoHash)) {
      closeTorrentStream(infoHash)
    }
  })

  Object.keys(streamRetryTimers.value).forEach((infoHash) => {
    if (!infoHashes.has(infoHash)) {
      closeTorrentStream(infoHash)
    }
  })

  infoHashes.forEach((infoHash) => {
    openTorrentStream(infoHash)
  })
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

onBeforeUnmount(() => {
  closeAllTorrentStreams()
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
            <p><strong>Import Status:</strong> {{ formatImportMediaStatus(torrent.importMediaStatus) }}</p>
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
