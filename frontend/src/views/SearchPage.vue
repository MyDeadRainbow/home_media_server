<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SearchResultsGroup from '../components/SearchResultsGroup.vue'
import { importStreamMedia, searchAcquisitionStream } from '../api'

const route = useRoute()
const router = useRouter()

const query = ref(typeof route.query.q === 'string' ? route.query.q : '')
const category = ref(route.query.category === 'SERIES' ? 'SERIES' : 'MOVIE')
const sortBy = ref('seeders')
const loading = ref(false)
const hasSearched = ref(false)
const error = ref('')
const importStatus = ref('')
const resultsBySource = ref({})
const importInFlightByMagnet = ref({})
let searchAbortController = null

function resetResults() {
  resultsBySource.value = {}
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

function compareResults(left, right) {
  if (sortBy.value === 'size') {
    return parseSizeToBytes(right.size) - parseSizeToBytes(left.size)
  }
  return parseSeeders(right.seeders) - parseSeeders(left.seeders)
}

const groupedResults = computed(() => Object.entries(resultsBySource.value)
  .map(([source, items]) => ({
    source,
    items: [...items].sort(compareResults)
  }))
  .sort((a, b) => a.source.localeCompare(b.source)))

const hasAnyResults = computed(() => groupedResults.value.some((group) => group.items.length > 0))

function isDuplicateResult(result) {
  return Object.values(resultsBySource.value).some((group) => group.some((existing) => (
    (result.magnetLink && result.magnetLink === existing.magnetLink)
    || (result.sourceUrl && result.sourceUrl === existing.sourceUrl)
  )))
}

async function runSearch() {
  const term = query.value.trim()
  if (!term) {
    hasSearched.value = false
    resetResults()
    error.value = ''
    return
  }

  if (searchAbortController) {
    searchAbortController.abort()
  }

  searchAbortController = new AbortController()
  loading.value = true
  hasSearched.value = true
  error.value = ''
  importStatus.value = ''
  resetResults()

  router.replace({
    path: '/search',
    query: {
      q: term,
      category: category.value
    }
  })

  try {
    await searchAcquisitionStream(term, category.value, {
      signal: searchAbortController.signal,
      onItem: (item) => {
        if (!item || !item.title) {
          return
        }

        const normalized = {
          ...item,
          category: category.value
        }

        if (!isDuplicateResult(normalized)) {
          const source = normalized.source || 'Unknown Source'
          const existing = resultsBySource.value[source] || []
          resultsBySource.value = {
            ...resultsBySource.value,
            [source]: [...existing, normalized]
          }
        }
      },
      onError: (message) => {
        if (message) {
          error.value = String(message)
        }
      }
    })
  } catch (err) {
    if (err?.name !== 'AbortError') {
      error.value = err?.message || 'Search failed.'
      resetResults()
    }
  } finally {
    if (!searchAbortController?.signal.aborted) {
      loading.value = false
    }
  }
}

function isImportingResult(magnetLink) {
  return Boolean(importInFlightByMagnet.value[magnetLink])
}

function useResultTitle(title) {
  query.value = String(title || '').trim()
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
    await importStreamMedia({
      title: result.title,
      magnetLink: result.magnetLink,
      category: result.category || category.value
    })
    importStatus.value = `Import request created for "${result.title}".`
  } catch (err) {
    error.value = err?.message || 'Import request failed.'
  } finally {
    const nextInFlight = { ...importInFlightByMagnet.value }
    delete nextInFlight[result.magnetLink]
    importInFlightByMagnet.value = nextInFlight
  }
}

onBeforeUnmount(() => {
  if (searchAbortController) {
    searchAbortController.abort()
  }
})

onMounted(() => {
  if (query.value.trim()) {
    runSearch()
  }
})
</script>

<template>
  <div class="search-page">
    <section class="panel controls">
      <div class="controls-header">
        <h2>Search Torrents</h2>
      </div>

      <div class="acquisition-search">
        <div class="acquisition-actions">
          <select v-model="category" aria-label="Select torrent category">
            <option value="MOVIE">Movies</option>
            <option value="SERIES">Series</option>
          </select>
          <select v-model="sortBy" aria-label="Sort torrent results">
            <option value="seeders">Sort by Seeders</option>
            <option value="size">Sort by Size</option>
          </select>
        </div>

        <div class="acquisition-search-box">
          <input
            id="search-page-search-box"
            v-model="query"
            placeholder="Search torrent sources"
            aria-label="Search torrent sources"
          >
          <button type="button" @click="runSearch">Search Sources</button>
        </div>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="importStatus" class="status">{{ importStatus }}</p>
    </section>

    <section class="panel search-results-panel">
      <h2 v-if="query.trim()">Results for "{{ query.trim() }}"</h2>
      <h2 v-else>Search Results</h2>

      <p v-if="loading" class="muted">Searching sources...</p>
      <p v-if="!loading && hasSearched && !hasAnyResults" class="muted">No results found.</p>
      <p v-else-if="!loading && !hasSearched" class="muted">Click Search Sources to run a search.</p>

      <div v-if="hasAnyResults" class="search-results-groups">
        <SearchResultsGroup
          v-for="group in groupedResults"
          :key="group.source"
          :source="group.source"
          :items="group.items"
          :category="category"
          :is-importing="isImportingResult"
          @import="importSearchResult"
          @use-title="useResultTitle"
        />
      </div>
    </section>
  </div>
</template>