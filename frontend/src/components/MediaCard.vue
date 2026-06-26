<template>
  <article class="media-card">
    <img class="poster" :src="item.posterUrl || fallbackPoster" :alt="item.title" />
    <div class="content">
      <div class="meta-row">
        <span class="type">{{ item.type }}</span>
        <span class="year">{{ releaseYear }}</span>
      </div>
      <h3>{{ item.title }}</h3>
      <p class="summary">{{ item.plotSummary || item.description || 'No description available.' }}</p>
      <div class="card-details">
        <span v-if="item.releaseDate">Release: {{ formattedReleaseDate }}</span>
        <span v-if="item.rating !== null && item.rating !== undefined">Rating: {{ formattedRating }}</span>
      </div>
      <button @click="$emit('play', item)">Stream</button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  }
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

const fallbackPoster = 'https://picsum.photos/seed/fallback/320/180'
</script>
