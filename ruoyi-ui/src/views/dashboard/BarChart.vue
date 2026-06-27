<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>
import * as echarts from 'echarts'
import 'echarts/theme/macarons'
import resize from './mixins/resize'

export default {
  mixins: [resize],
  props: {
    className: { type: String, default: 'chart' },
    width: { type: String, default: '100%' },
    height: { type: String, default: '350px' },
    title: { type: String, default: '' },
    chartData: {
      type: Object,
      default: () => ({ months: [], series: [] }),
    },
  },
  data() {
    return { chart: null }
  },
  watch: {
    chartData: {
      deep: true,
      handler(val) { this.setOptions(val) },
    },
  },
  mounted() {
    this.$nextTick(() => { this.initChart() })
  },
  beforeUnmount() {
    if (!this.chart) return
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')
      this.setOptions(this.chartData)
    },
    setOptions({ months, series } = {}) {
      if (!this.chart) return
      this.chart.setOption({
        title: {
          text: this.title,
          left: 'center',
          textStyle: { fontSize: 16, fontWeight: 'normal' },
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
        },
        legend: {
          data: (series || []).map((s) => s.name),
          bottom: 0,
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '12%',
          top: this.title ? '18%' : '10%',
          containLabel: true,
        },
        xAxis: {
          type: 'category',
          data: months || [],
          axisTick: { alignWithLabel: true },
        },
        yAxis: { type: 'value' },
        series: (series || []).map((s) => ({
          name: s.name,
          type: 'bar',
          barWidth: '50%',
          data: s.data || [],
          animationDuration: 2000,
        })),
      })
    },
  },
}
</script>
