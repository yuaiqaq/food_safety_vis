<template>
  <div class="network-container">
    <div class="network-header">
      <span class="network-title">🔵 样本网络视图（Substrate）</span>
      <span class="network-subtitle">节点 = 食品样本，颜色 = 违规等级，大小 = 严重程度</span>
    </div>
    <div v-if="store.loadingSubstrate" class="loading-mask">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
    </div>
    <div ref="chartEl" class="chart" />
    <div v-if="store.selectedSubstrateIds.length" class="selection-info">
      已选 {{ store.selectedSubstrateIds.length }} 个样本
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

const GRADE_COLORS = ['#52c41a', '#faad14', '#fa8c16', '#f5222d']
const GRADE_LABELS = ['轻微(0)', '较轻(1)', '中等(2)', '严重(3)']

function buildOption(data, highlightIds) {
  const nodes = data.nodes.map(n => {
    const isHighlighted = !highlightIds.size || highlightIds.has(n.id)
    return {
      id: n.id,
      name: n.name,
      category: n.category,
      symbolSize: n.value,
      itemStyle: {
        color: GRADE_COLORS[n.category] || '#52c41a',
        opacity: isHighlighted ? 1 : 0.15,
        borderColor: isHighlighted ? '#fff' : 'transparent',
        borderWidth: isHighlighted ? 1 : 0,
      },
      label: { show: false },
      extra: n.properties,
    }
  })

  const edges = data.edges.map(e => ({
    source: e.source,
    target: e.target,
    lineStyle: { color: '#2a4080', opacity: 0.4, width: 0.8 },
  }))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType !== 'node') return ''
        const p = params.data.extra || {}
        const gradeLabels = { 0: '轻微', 1: '较轻', 2: '中等', 3: '严重' }
        return `
          <div style="font-size:13px;line-height:1.8">
            <b>样品 #${p.id}</b><br/>
            省份：${p.productionLocation || '-'} ${p.productionLocation2 || ''}<br/>
            食品类别：${p.foodCategory || '-'}<br/>
            违规类型：${p.adulterantCategory || '-'}<br/>
            违规项目：${p.adulterant || '-'}<br/>
            违规等级：<span style="color:${GRADE_COLORS[p.grade]}">${gradeLabels[p.grade] || '-'}</span><br/>
            检查级别：${p.mandateLevel || '-'}
          </div>`
      },
      backgroundColor: '#1a2540',
      borderColor: '#3060a0',
      textStyle: { color: '#e0e8f0' },
    },
    legend: {
      data: GRADE_LABELS.map((name, i) => ({ name, icon: 'circle', textStyle: { color: GRADE_COLORS[i] } })),
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
      categories: data.categories?.map((c, i) => ({ name: GRADE_LABELS[i] || c.name, itemStyle: { color: GRADE_COLORS[i] } })) || [],
      roam: true,
      draggable: true,
      force: {
        repulsion: 55,
        gravity: 0.08,
        edgeLength: [30, 80],
        layoutAnimation: true,
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 2 },
      },
      selectedMode: 'multiple',
      select: {
        itemStyle: { borderColor: '#5bc8f5', borderWidth: 2, color: '#5bc8f5' },
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
      const node = store.substrateNetwork.nodes[idx]
      return node ? node.id : null
    })).filter(Boolean)
    store.onSubstrateSelected(ids)
  })

  chart.on('dblclick', () => {
    chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
    store.onSubstrateSelected([])
  })
}

function renderChart() {
  if (!chart) return
  const option = buildOption(store.substrateNetwork, store.highlightSampleIds)
  chart.setOption(option, true)
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

watch(() => store.substrateNetwork, renderChart, { deep: true })
watch(() => store.highlightSampleIds, () => renderChart(), { deep: true })

function clearSelection() {
  chart?.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
  store.onSubstrateSelected([])
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
  color: #7ab8f5;
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
  color: #7ab8f5;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 5;
}
</style>
