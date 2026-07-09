<script setup>
import SearchResultItem from './SearchResultItem.vue'

const props = defineProps({
  source: {
    type: String,
    required: true
  },
  items: {
    type: Array,
    required: true
  },
  category: {
    type: String,
    required: true
  },
  isImporting: {
    type: Function,
    required: true
  }
})

const emit = defineEmits(['import', 'use-title'])

function onImport(item) {
  emit('import', item)
}

function onUseTitle(title) {
  emit('use-title', title)
}
</script>

<template>
  <article class="search-source-group">
    <h3>{{ source }}</h3>
    <ul class="search-results-list">
      <SearchResultItem
        v-for="(item, index) in items"
        :key="`${source}-${item.magnetLink || item.sourceUrl || index}`"
        :item="item"
        :category="category"
        :importing="isImporting(item.magnetLink)"
        @import="onImport"
        @use-title="onUseTitle"
      />
    </ul>
  </article>
</template>
