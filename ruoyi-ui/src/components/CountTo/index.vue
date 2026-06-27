<template>
  <span>{{ displayValue }}</span>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  startVal: { type: Number, default: 0 },
  endVal: { type: Number, required: true },
  duration: { type: Number, default: 2000 },
  decimals: { type: Number, default: 0 },
  separator: { type: String, default: ',' },
  decimal: { type: String, default: '.' },
})

const displayValue = ref(formatNumber(props.startVal))

let startTime = null
let rafId = null

function formatNumber(num) {
  const fixed = num.toFixed(props.decimals)
  const [intPart, decPart] = fixed.split('.')
  const formatted = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, props.separator)
  return decPart ? `${formatted}${props.decimal}${decPart}` : formatted
}

function easeOutExpo(t) {
  return t === 1 ? 1 : 1 - Math.pow(2, -10 * t)
}

function animate(timestamp) {
  if (!startTime) startTime = timestamp
  const elapsed = timestamp - startTime
  const progress = Math.min(elapsed / props.duration, 1)
  const easedProgress = easeOutExpo(progress)
  const current = props.startVal + (props.endVal - props.startVal) * easedProgress
  displayValue.value = formatNumber(current)

  if (progress < 1) {
    rafId = requestAnimationFrame(animate)
  }
}

function start() {
  startTime = null
  if (rafId) cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(animate)
}

watch(() => props.endVal, () => {
  start()
})

onMounted(() => {
  start()
})
</script>
