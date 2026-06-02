<template>
  <article class="media-card">
    <img class="poster" :src="item.posterUrl || fallbackPoster" :alt="item.title" />
    <div class="content">
      <div class="meta-row">
        <span class="type">{{ item.type }}</span>
        <span class="year">{{ item.year }}</span>
      </div>
      <h3>{{ item.title }}</h3>
      <p>{{ item.description || 'No description available.' }}</p>
      <button @click="$emit('play', item)">Stream</button>
    </div>
  </article>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true
  }
})

const fallbackPoster = 'https://picsum.photos/seed/fallback/320/180'
</script>
