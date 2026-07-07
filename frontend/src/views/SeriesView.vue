<template>
  <div class="page">
    <header class="hero">
      <h1>{{ series?.title || 'Series' }}</h1>
      <p v-if="series?.plotSummary">{{ series.plotSummary }}</p>
    </header>

    <div v-if="series" class="series-detail">
        <section class="panel library-subsection overview-section">
            <div class="library-header">
                <h2>Series Details</h2>
                <button type="button" class="secondary-button" @click="goBack">Back to Library</button>
            </div>
            <MediaCard :item="series" :show-action="false" />
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
    </div>
    <p v-else class="error">Series not found.</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MediaCard from '../components/MediaCard.vue'
import { searchCatalogSeasons, searchCatalogEpisodes, streamManifest, streamCaptionsUrl } from '../api'

const route = useRoute()
const router = useRouter()

const series = ref(null)
const seasons = ref([])
const episodes = ref([])
const manifest = ref(null)
const tracks = ref([])
const activeMedia = ref(null)
const selectedCaption = ref('off')
const error = ref('')
const selectedSeasonId = ref(null)

function normalizeSeriesItem(item) {
  const metadata = item?.metaData || item?.metadata || item?.meta || {}
  return {
    id: item.id || item.seriesId,
    seriesId: item.seriesId || item.id,
    type: item.type || 'series',
    title: metadata.title || item.title || item.name || 'Series',
    plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
    description: metadata.plotSummary || item.plotSummary || item.description || '',
    releaseDate: metadata.releaseDate || metadata.firstAirDate || item.releaseDate || null,
    rating: item.rating ?? metadata.rating ?? null,
    posterUrl: metadata.posterUrl || item.posterUrl || ''
  }
}

async function loadSeries() {
  const seriesId = route.params.seriesId
  if (!seriesId) {
    return
  }

  try {
    const [seriesResult, seasonsResult] = await Promise.all([
      Promise.resolve(null),
      searchCatalogSeasons(seriesId)
    ])

    const fallbackSeries = { seriesId, title: route.query.title || 'Series' }
    series.value = normalizeSeriesItem(seriesResult || fallbackSeries)
    seasons.value = Array.isArray(seasonsResult) ? seasonsResult : []

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
  selectedSeasonId.value = season.seasonId || season.id

  try {
    const result = await searchCatalogEpisodes(series.value?.seriesId, season.seasonId || season.id)
    episodes.value = Array.isArray(result) ? result : []
  } catch (err) {
    error.value = err.message || 'Failed to load episodes.'
  }
}

async function startPlayback(item) {
  activeMedia.value = item
  try {
    manifest.value = await streamManifest(item.mediaId || item.id, item.streamUrl)
    tracks.value = (manifest.value?.captions || []).map((track) => ({
      ...track,
      url: streamCaptionsUrl(item.mediaId || item.id, track.language)
    }))
  } catch (err) {
    error.value = err.message || 'Failed to start playback.'
  }
}

function goBack() {
  router.push('/')
}

onMounted(() => {
  loadSeries()
})
</script>
