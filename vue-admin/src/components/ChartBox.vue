<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * ECharts 通用容器
 * - option 变化时自动 setOption
 * - 容器尺寸变化自动 resize (ResizeObserver)
 */
const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '340px' },
  loading: { type: Boolean, default: false }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart) return
  chart.setOption(props.option || {}, true)
}

function init() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  render()
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => {
      if (chart) chart.resize()
    })
    observer.observe(chartRef.value)
  }
}

watch(
  () => props.option,
  () => render(),
  { deep: true }
)

watch(
  () => props.loading,
  (val) => {
    if (!chart) return
    if (val) chart.showLoading({ text: '加载中...', color: '#ff6700' })
    else chart.hideLoading()
  }
)

onMounted(() => {
  nextTick(init)
})

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})

defineExpose({
  resize: () => chart && chart.resize()
})
</script>
