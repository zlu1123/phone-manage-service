<template>
  <div class="watermark-container" :style="backgroundStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  text: {
    type: String,
    default: '',
  },
  rotate: {
    type: Number,
    default: -22,
  },
  gap: {
    type: Number,
    default: 180,
  },
  fontSize: {
    type: Number,
    default: 14,
  },
  color: {
    type: String,
    default: 'rgba(180, 180, 180, 0.2)',
  },
});

/**
 * 生成 SVG 水印背景图
 * 返回 data URI，可直接用作 background-image
 */
const generateSvgDataUri = () => {
  if (!props.text) return '';

  const w = props.gap;
  const h = Math.round(props.gap * 0.7);
  const cx = Math.round(w / 2);
  const cy = Math.round(h / 2);

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">
    <text x="${cx}" y="${cy}" transform="rotate(${props.rotate},${cx},${cy})"
      font-size="${props.fontSize}" fill="${props.color}"
      font-family="Microsoft YaHei, PingFang SC, sans-serif"
      text-anchor="middle" dominant-baseline="middle">${props.text}</text>
  </svg>`;

  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
};

const backgroundStyle = computed(() => {
  const uri = generateSvgDataUri();
  return uri ? { backgroundImage: uri, backgroundRepeat: 'repeat' } : {};
});
</script>

<style scoped>
.watermark-container {
  position: relative;
  width: 100%;
  height: 100%;
}
</style>
