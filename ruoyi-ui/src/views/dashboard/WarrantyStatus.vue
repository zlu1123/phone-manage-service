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
    height: { type: String, default: '300px' },
    title: { type: String, default: '' },
    chartData: {
      type: Array,
      default: () => [],
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
    setOptions(data) {
      if (!this.chart) return
      const names = (data || []).map((d) => d.name)
      const values = (data || []).map((d) => d.value)
      this.chart.setOption({
        title: {
          text: this.title,
          left: 'center',
          textStyle: { fontSize: 16, fontWeight: 'normal' },
        },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '10%', bottom: '3%', top: '15%', containLabel: true },
        xAxis: { type: 'value' },
        yAxis: {
          type: 'category',
          data: names,
          axisLabel: { fontSize: 13 },
        },
        series: [{
          type: 'bar',
          data: values,
          barWidth: '40%',
          itemStyle: {
            borderRadius: 4,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#36a3f7' },
              { offset: 1, color: '#40c9c6' },
            ]),
          },
          label: {
            show: true,
            position: 'right',
            fontSize: 13,
          },
          animationDuration: 2000,
        }],
      })
    },
  },
}
</script>
