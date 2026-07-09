<script setup>
const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  category: {
    type: String,
    required: true
  },
  importing: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['import', 'use-title'])

function onImport() {
  emit('import', props.item)
}

function onUseTitle() {
  emit('use-title', props.item.title || '')
}
</script>

<template>
  <li class="search-result-item">
    <div class="search-result-row">
      <strong>{{ item.title }}</strong>
    </div>

    <div class="search-result-row search-result-meta">
      <span>Seeders: {{ item.seeders || 'N/A' }}</span>
      <span>Leechers: {{ item.leechers || 'N/A' }}</span>
      <span>Size: {{ item.size || 'N/A' }}</span>
      <span>Category: {{ item.category || category }}</span>
    </div>

    <div class="search-result-actions">
      <button type="button" class="secondary-button" @click="onUseTitle">Use Title</button>
      <button type="button" :disabled="importing" @click="onImport">
        {{ importing ? 'Importing...' : 'Import' }}
      </button>
    </div>
  </li>
</template>
