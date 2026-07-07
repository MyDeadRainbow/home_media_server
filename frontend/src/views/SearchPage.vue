<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const results = ref([])
const loading = ref(false)

async function runSearch() {
  const query = route.query.q

  if (!query) return

  loading.value = true

  try {
    const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`)
    const data = await res.json()
    results.value = data
  } catch (e) {
    console.error(e)
  }

  loading.value = false
}

onMounted(runSearch)
</script>

<template>
  <div class="search-page">
    <h2>Search Results for "{{ $route.query.q }}"</h2>

    <div v-if="loading">Loading...</div>

    <div v-else>
      <div v-if="results.length === 0">
        No results found
      </div>

      <ul>
        <li v-for="(item, index) in results" :key="index">
          {{ item.title || item.name }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script>

</script>