<template>
  <div class="network-container">
    <div class="network-header">
      <div class="header-top">
        <span class="network-title">🔵 样本网络视图（Substrate）</span>
        <div class="mode-switch">
          <el-button-group size="small">
            <el-button 
              :type="pivotMode === 'OR' ? 'primary' : 'default'" 
              @click="setPivotMode('OR')"
              :disabled="store.selectedSubstrateIds.length === 0">
              OR
            </el-button>
            <el-button 
              :type="pivotMode === 'AND' ? 'primary' : 'default'" 
              @click="setPivotMode('AND')"
              :disabled="store.selectedSubstrateIds.length === 0">
              AND
            </el-button>
          </el-button-group>
          <span class="mode-hint">{{ pivotModeHint }}</span>
        </div>
      </div>
      <span class="network-subtitle">节点 = 食品样本，颜色 = 违规等级，大小 = 严重程度（按住 Shift 可框选）</span>
    </div>
    <div v-if="store.loadingSubstrate" class="loading-mask">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
    </div>
    <div class="chart-wrapper">
      <div ref="chartEl" class="chart" />
      <div
        v-if="lassoRect.visible"
        class="lasso-rect"
        :style="{
          left: `${lassoRect.left}px`,
          top: `${lassoRect.top}px`,
          width: `${lassoRect.width}px`,
          height: `${lassoRect.height}px`,
        }"
      />
    </div>
    <div v-if="store.selectedSubstrateIds.length" class="selection-info">
      已选 {{ store.selectedSubstrateIds.length }} 个样本
      <el-button size="small" type="warning" @click="clearSelection">清除</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { useMainStore } from '../store/index.js'
import { saveSubstrateSnapshot } from '../api/index.js'

const store = useMainStore()
const chartEl = ref(null)
let chart = null
let lastSnapshotKey = ''
let snapshotSaving = false
let layoutNeedsSnapshot = false
let isLassoSelecting = false
const lassoStart = { x: 0, y: 0 }
const lassoRect = ref({ visible: false, left: 0, top: 0, width: 0, height: 0 })

// ⭐ 添加这些缺失的响应式变量
const pivotMode = ref('OR')  // 'OR' 或 'AND'

const pivotModeHint = computed(() => {
  if (store.selectedSubstrateIds.length === 0) {
    return '（请先选中样本）'
  }
  if (pivotMode.value === 'OR') {
    return 'OR: 显示包含任一选中属性的催化剂'
  }
  return 'AND: 只显示包含所有选中属性的催化剂'
})

const GRADE_COLORS = ['#52c41a', '#faad14', '#fa8c16', '#f5222d']
const GRADE_LABELS = ['轻微(0)', '较轻(1)', '中等(2)', '严重(3)']

function buildOption(data, highlightIds) {
  const hasSnapshotLayout = data.nodes.length > 0 && data.nodes.every(
    n => Number.isFinite(n.x) && Number.isFinite(n.y)
  )
  const nodes = data.nodes.map(n => {
    const isHighlighted = !highlightIds.size || highlightIds.has(n.id)
    const node = {
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
    if (hasSnapshotLayout) {
      node.x = n.x
      node.y = n.y
      node.fixed = true
    }
    return node
  })

  const filteredEdges = data.edges.filter(e => (e.value || 1) > 5);
  const weights = filteredEdges.map(e => e.value || 1);
  const maxWeight = Math.max(...weights, 1);
  
  const getEdgeColor = (weight, maxWeight) => {
    const ratio = weight / maxWeight;
    if (ratio >= 0.8) return '#f5222d';
    if (ratio >= 0.6) return '#fa8c16';
    if (ratio >= 0.4) return '#fadb14';
    return '#5bc8f5';
  };
  
  const edges = filteredEdges.map(e => {
    const weight = e.value || 1;
    const color = getEdgeColor(weight, maxWeight);
    
    return {
      source: e.source,
      target: e.target,
      value: weight,
      lineStyle: {
        color: color,
        width: 2,
        opacity: 0.8,
        curveness: 0.1,
        shadowBlur: weight > maxWeight * 0.7 ? 6 : 0,
        shadowColor: color,
      },
    }
  })

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'edge') {
          return `关联强度: ${params.data.value}`;
        }
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
      layout: hasSnapshotLayout ? 'none' : 'force',
      data: nodes,
      links: edges,
      categories: data.categories?.map((c, i) => ({ name: GRADE_LABELS[i] || c.name, itemStyle: { color: GRADE_COLORS[i] } })) || [],
      roam: true,
      draggable: true,
      force: hasSnapshotLayout ? undefined : {
        repulsion: 300,
        gravity: 0.03,
        edgeLength: [80, 150],
        layoutAnimation: true,
        initIterations: 300,
        iterations: 200,
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

// ⭐ 添加 setPivotMode 方法
async function setPivotMode(mode) {
  pivotMode.value = mode
  
  if (store.selectedSubstrateIds.length === 0) {
    return
  }
  
  // 调用 store 的方法更新 Catalyst 高亮
  // 注意：这里调用的是 store.onSubstrateSelected，它会触发 API 请求
  console.log(`切换到 ${mode} 模式，更新属性网络...`)
  await store.onSubstrateSelected(store.selectedSubstrateIds,mode)
}

function updateLassoRect(currentX, currentY) {
  const left = Math.min(lassoStart.x, currentX)
  const top = Math.min(lassoStart.y, currentY)
  const width = Math.abs(currentX - lassoStart.x)
  const height = Math.abs(currentY - lassoStart.y)
  lassoRect.value = { visible: true, left, top, width, height }
}

function resetLassoRect() {
  lassoRect.value = { visible: false, left: 0, top: 0, width: 0, height: 0 }
}

function collectSelectedIdsFromRect(rect) {
  const selectedIndices = new Set()
  const seriesModel = chart?.getModel()?.getSeriesByIndex(0)
  const seriesData = seriesModel?.getData()
  if (!seriesData) return []

  const minX = rect.left
  const maxX = rect.left + rect.width
  const minY = rect.top
  const maxY = rect.top + rect.height

  seriesData.each((idx) => {
    const layout = seriesData.getItemLayout(idx)
    if (!layout) return
    if (layout.x >= minX && layout.x <= maxX && layout.y >= minY && layout.y <= maxY) {
      selectedIndices.add(idx)
    }
  })

  return Array.from(selectedIndices)
    .map(idx => store.substrateNetwork.nodes[idx]?.id)
    .filter(Boolean)
}

function getSnapshotNodesFromChart() {
  const optionNodes = chart?.getOption()?.series?.[0]?.data || []
  return optionNodes
    .map(node => ({
      id: node.id != null ? String(node.id) : null,
      x: Number(node.x),
      y: Number(node.y),
    }))
    .filter(node => node.id && Number.isFinite(node.x) && Number.isFinite(node.y))
}

function buildSnapshotKey(snapshotNodes) {
  if (!snapshotNodes.length) return ''
  const sortedIds = snapshotNodes.map(n => n.id).sort()
  return sortedIds.join('|')
}

async function saveLayoutSnapshot() {
  if (!chart || snapshotSaving || !layoutNeedsSnapshot) return
  const snapshotNodes = getSnapshotNodesFromChart()
  if (!snapshotNodes.length || snapshotNodes.length !== store.substrateNetwork.nodes.length) return

  const snapshotKey = buildSnapshotKey(snapshotNodes)
  if (snapshotKey === lastSnapshotKey) return

  snapshotSaving = true
  try {
    await saveSubstrateSnapshot(snapshotNodes)
    lastSnapshotKey = snapshotKey
    layoutNeedsSnapshot = false
  } catch (error) {
    console.error('保存样本网络快照失败:', error)
  } finally {
    snapshotSaving = false
  }
}

function initChart() {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value, 'dark')

  chart.on('selectchanged', (evt) => {
    const selectedIndices = new Set()

    if (evt.batch && evt.batch.length) {
      evt.batch.forEach(batch => {
        if (batch.selected && batch.selected.length) {
          batch.selected.forEach(series => {
            series.dataIndex?.forEach(idx => selectedIndices.add(idx))
          })
        }
      })
    } else if (evt.selected && evt.selected.length) {
      evt.selected.forEach(series => {
        series.dataIndex?.forEach(idx => selectedIndices.add(idx))
      })
    }

    const ids = Array.from(selectedIndices).map(idx => {
      const node = store.substrateNetwork.nodes[idx]
      return node ? node.id : null
    }).filter(Boolean)
    
    if (JSON.stringify(ids) !== JSON.stringify(store.selectedSubstrateIds)) {
      store.selectedSubstrateIds = ids
      
      if (ids.length === 0) {
        // 没有选中时，清空 Catalyst 高亮
        store.highlightAttrIds = new Set()
      }
      // 注意：有选中时，不自动更新 Catalyst，等待用户点击 OR/AND 按钮
    }
  })

  chart.on('finished', async () => {
    const hasSnapshotLayout = store.substrateNetwork.nodes.length > 0
      && store.substrateNetwork.nodes.every(node => Number.isFinite(node.x) && Number.isFinite(node.y))
    if (!hasSnapshotLayout) {
      await saveLayoutSnapshot()
    }
  })

  chart.on('dblclick', () => {
    chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
    store.selectedSubstrateIds = []
    store.highlightAttrIds = new Set()
  })

  const zr = chart.getZr()
  zr.on('mousedown', (evt) => {
    const nativeEvent = evt.event
    if (!nativeEvent?.shiftKey || nativeEvent.button !== 0) return
    isLassoSelecting = true
    lassoStart.x = nativeEvent.offsetX
    lassoStart.y = nativeEvent.offsetY
    updateLassoRect(nativeEvent.offsetX, nativeEvent.offsetY)
  })

  zr.on('mousemove', (evt) => {
    if (!isLassoSelecting) return
    const nativeEvent = evt.event
    updateLassoRect(nativeEvent.offsetX, nativeEvent.offsetY)
  })

  zr.on('mouseup', (evt) => {
    if (!isLassoSelecting) return
    isLassoSelecting = false
    const nativeEvent = evt.event
    updateLassoRect(nativeEvent.offsetX, nativeEvent.offsetY)
    const rect = lassoRect.value
    const ids = collectSelectedIdsFromRect(rect)
    resetLassoRect()

    chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
    ids.forEach((id) => {
      const idx = store.substrateNetwork.nodes.findIndex(node => node.id === id)
      if (idx >= 0) {
        chart.dispatchAction({ type: 'select', seriesIndex: 0, dataIndex: idx })
      }
    })

    store.selectedSubstrateIds = ids
    if (!ids.length) {
      store.highlightAttrIds = new Set()
    }
  })

  zr.on('globalout', () => {
    if (!isLassoSelecting) return
    isLassoSelecting = false
    resetLassoRect()
  })
}

function renderChart() {
  if (!chart) return
  const option = buildOption(store.substrateNetwork, store.highlightSampleIds)
  chart.setOption(option, true)
  if (store.substrateNetwork.nodes.length > 0 && store.substrateNetwork.nodes.every(
    node => Number.isFinite(node.x) && Number.isFinite(node.y)
  )) {
    lastSnapshotKey = buildSnapshotKey(store.substrateNetwork.nodes)
    layoutNeedsSnapshot = false
  } else {
    layoutNeedsSnapshot = store.substrateNetwork.nodes.length > 0
  }
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

// ⭐ 修复清除功能
function clearSelection() {
  console.log('清除所有选中')
  resetLassoRect()
  isLassoSelecting = false
  
  if (chart) {
    chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
  }
  
  // 清空 store 中的选中样本
  store.selectedSubstrateIds = []
  
  // 清空 Catalyst 高亮属性
  store.highlightAttrIds = new Set()
  
  // 重新渲染
  nextTick(() => {
    renderChart()
  })
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
  gap: 6px;
}
.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
.mode-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mode-hint {
  font-size: 10px;
  color: #a0b8d0;
  background: rgba(0, 0, 0, 0.3);
  padding: 2px 6px;
  border-radius: 4px;
}
.chart {
  width: 100%;
  height: 100%;
}
.chart-wrapper {
  position: relative;
  flex: 1;
  min-height: 0;
}
.lasso-rect {
  position: absolute;
  border: 1px dashed #5bc8f5;
  background: rgba(91, 200, 245, 0.15);
  pointer-events: none;
  z-index: 6;
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
