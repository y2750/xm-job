<template>
  <a-card>
    <template #title>
      <h3>{{ title || '页面改造中' }}</h3>
    </template>
    <a-empty :description="description || '此页面正在改造中，将逐步迁移到 Ant Design Vue。'" />
  </a-card>
</template>

<script setup>
defineProps({
  title: String,
  description: String
})
</script>

