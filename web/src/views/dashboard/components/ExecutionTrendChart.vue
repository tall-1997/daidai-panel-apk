<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '@/stores/theme'

const props = defineProps<{
  stats: Array<{
    date?: string
    success?: number
    failed?: number
  }>
}>()

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const theme = useThemeStore()

const colors = computed(() => {
  if (theme.isDark) {
    return {
      tooltipBg: '#1e293b',
      tooltipBorder: '#334155',
      tooltipText: '#e2e8f0',
      axisLine: '#334155',
      splitLine: '#1e293b',
      labelColor: '#94a3b8',
      pointBorder: '#1e293b',
      shadow: 'rgba(0,0,0,0.25)',
    }
  }
  return {
    tooltipBg: '#fff',
    tooltipBorder: '#f0f0f0',
    tooltipText: '#333',
    axisLine: '#f0f0f0',
    splitLine: '#f5f5f5',
    labelColor: '#8c8c8c',
    pointBorder: '#fff',
    shadow: 'rgba(0,0,0,0.08)',
  }
})

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const c = colors.value

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: c.tooltipBg,
      borderColor: c.tooltipBorder,
      borderWidth: 1,
      textStyle: { color: c.tooltipText, fontSize: 12 },
      extraCssText: `border-radius: 8px; box-shadow: 0 2px 8px ${c.shadow};`,
    },
    legend: {
      data: ['执行总数', '成功', '失败'],
      icon: 'circle',
      itemWidth: 8,
      textStyle: { fontSize: 12, color: c.labelColor },
      top: 0,
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: props.stats.map((item) => item.date),
      axisLine: { lineStyle: { color: c.axisLine } },
      axisTick: { show: false },
      axisLabel: { color: c.labelColor, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { lineStyle: { color: c.axisLine } },
      splitLine: { lineStyle: { color: c.splitLine } },
      axisLabel: { color: c.labelColor, fontSize: 11 },
    },
    series: [
      {
        name: '执行总数',
        type: 'line',
        data: props.stats.map((item) => (item.success || 0) + (item.failed || 0)),
        smooth: 0.6,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.5, color: '#409EFF' },
        itemStyle: { color: '#409EFF', borderWidth: 2, borderColor: c.pointBorder },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.2)' },
            { offset: 1, color: 'rgba(64,158,255,0)' },
          ])
        },
      },
      {
        name: '成功',
        type: 'line',
        data: props.stats.map((item) => item.success || 0),
        smooth: 0.6,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.5, color: '#67C23A' },
        itemStyle: { color: '#67C23A', borderWidth: 2, borderColor: c.pointBorder },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103,194,58,0.15)' },
            { offset: 1, color: 'rgba(103,194,58,0)' },
          ])
        },
      },
      {
        name: '失败',
        type: 'line',
        data: props.stats.map((item) => item.failed || 0),
        smooth: 0.6,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.5, color: '#F56C6C' },
        itemStyle: { color: '#F56C6C', borderWidth: 2, borderColor: c.pointBorder },
      },
    ],
  })
}

watch(() => props.stats, renderChart, { deep: true })
watch(() => theme.isDark, renderChart)

onMounted(() => {
  renderChart()
  resizeHandler = () => {
    chart?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="chartRef" class="trend-chart"></div>
</template>

<style scoped>
.trend-chart {
  height: 280px;
}
</style>
