<template>
  <div class="network-container">
    <div class="network-header">
      <span class="network-title">🟠 属性关联网络（Catalyst）</span>
      <span class="network-subtitle">节点 = 属性值，大小 = 关联频次，边 = 共现关系</span>
    </div>
    <div v-if="store.loadingCatalyst" class="loading-mask">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
    </div>
    <div ref="chartEl" class="chart" />
    <div v-if="store.selectedCatalystIds.length" class="selection-info">
      已选 {{ store.selectedCatalystIds.length }} 个属性节点
      <span v-if="store.pivotSampleCount !== null"> → 匹配 {{ store.pivotSampleCount }} 个样本</span>
      <el-button size="small" type="warning" @click="clearSelection">清除</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useMainStore } from '../store/index.js'

const store = useMainStore()
const chartEl = ref(null)
let chart = null

const TYPE_COLORS = ['#36cfc9', '#597ef7', '#ff7a45', '#ffc53d']
const TYPE_LABELS = ['省份', '食品类别', '违规类型', '违规项目']

function buildOption(data, highlightIds) {
  const nodes = data.nodes.map(n => {
    const isHighlighted = !highlightIds.size || highlightIds.has(n.id)
    const baseColor = TYPE_COLORS[n.category] || '#597ef7'
    return {
      id: n.id,
      name: n.name,
      category: n.category,
      symbolSize: Math.max(12, Math.min(n.value, 55)),
      itemStyle: {
        color: isHighlighted ? baseColor : '#1a2a40',
        opacity: isHighlighted ? 1 : 0.2,
        borderColor: isHighlighted ? baseColor : 'transparent',
        borderWidth: 1.5,
      },
      label: {
        show: n.value > 12,
        color: '#d0e8ff',
        fontSize: 10,
        formatter: n.name.length > 10 ? n.name.slice(0, 10) + '…' : n.name,
      },
      extra: n.properties,
    }
  })

  const maxEdge = Math.max(...data.edges.map(e => e.value), 1)
  const edges = data.edges.map(e => ({
    source: e.source,
    target: e.target,
    value: e.value,
    lineStyle: {
      width: Math.max(0.5, (e.value / maxEdge) * 5),
      color: '#2a4070',
      opacity: 0.5,
    },
  }))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          const p = params.data.extra || {}
          return `<b>${params.data.name}</b><br/>类型：${p.type || TYPE_LABELS[params.data.category]}<br/>样本数：${p.count || '-'}`
        }
        if (params.dataType === 'edge') {
          return `共现次数：${params.data.value}`
        }
        return ''
      },
      backgroundColor: '#1a2540',
      borderColor: '#3060a0',
      textStyle: { color: '#e0e8f0' },
    },
    legend: {
      data: TYPE_LABELS.map((name, i) => ({ name, icon: 'circle', textStyle: { color: TYPE_COLORS[i] } })),
      top: 4,
      right: 8,
      textStyle: { color: '#a0b8d0', fontSize: 11 },
      itemWidth: 10,
      itemHeight: 10,
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: edges,
      categories: TYPE_LABELS.map((name, i) => ({ name, itemStyle: { color: TYPE_COLORS[i] } })),
      roam: true,
      draggable: true,
      force: {
        repulsion: 120,
        gravity: 0.06,
        edgeLength: [60, 150],
        layoutAnimation: true,
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3 },
      },
      selectedMode: 'multiple',
      select: {
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: true, color: '#fff' },
      },
    }],
  }
}

function initChart() {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value, 'dark')

  chart.on('selectchanged', (evt) => {
    const selected = evt.selected || []
    const ids = selected.flatMap(s => s.dataIndex.map(idx => {
      const node = store.catalystNetwork.nodes[idx]
      return node ? node.id : null
    })).filter(Boolean)
    store.onCatalystSelected(ids)
  })

  chart.on('dblclick', () => {
    chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
    store.onCatalystSelected([])
  })
}

function renderChart() {
  if (!chart) return
  chart.setOption(buildOption(store.catalystNetwork, store.highlightAttrIds), true)
}

onMounted(async () => {
  await nextTick()
  initChart()
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', () => chart?.resize())
})

watch(() => store.catalystNetwork, renderChart, { deep: true })
watch(() => store.highlightAttrIds, () => renderChart(), { deep: true })

function clearSelection() {
  chart?.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
  store.onCatalystSelected([])
}
</script>

<style scoped>
.network-container {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(135deg, #0d1628 0%, #0a1220 100%);
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  overflow: hidden;
}
.network-header {
  padding: 8px 12px;
  background: rgba(20, 40, 80, 0.6);
  border-bottom: 1px solid #1e3a5f;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.network-title {
  font-size: 14px;
  font-weight: bold;
  color: #ff9966;
}
.network-subtitle {
  font-size: 11px;
  color: #607090;
}
.chart {
  flex: 1;
  min-height: 0;
}
.loading-mask {
  position: absolute;
  inset: 0;
  background: rgba(10, 15, 30, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  font-size: 16px;
  color: #5bc8f5;
  gap: 8px;
}
.selection-info {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(20, 40, 80, 0.9);
  border: 1px solid #3060a0;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  color: #ffaa66;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 5;
}
</style>
