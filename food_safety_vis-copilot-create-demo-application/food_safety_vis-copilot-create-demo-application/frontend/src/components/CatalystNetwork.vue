<template>
  <div class="network-container">
    <div class="network-header">
      <div class="header-top">
        <span class="network-title">🟠 属性关联网络（Catalyst）</span>
        <div class="mode-switch">
          <el-button-group size="small">
            <el-button 
              :type="pivotMode === 'OR' ? 'primary' : 'default'" 
              @click="setPivotMode('OR')"
              :disabled="store.selectedCatalystIds.length === 0">
              OR
            </el-button>
            <el-button 
              :type="pivotMode === 'AND' ? 'primary' : 'default'" 
              @click="setPivotMode('AND')"
              :disabled="store.selectedCatalystIds.length === 0">
              AND
            </el-button>
          </el-button-group>
          <span class="mode-hint">{{ pivotModeHint }}</span>
        </div>
      </div>
      <span class="network-subtitle">节点 = 属性值，大小 = 关联频次，边 = 共现关系（颜色越深关联越强）</span>
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
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { useMainStore } from '../store/index.js'
import { translateToChinese } from '../utils/valueTranslation.js'

const store = useMainStore()
const chartEl = ref(null)
let chart = null
let lastSelectedIds = []
let isClearing = false

// AND/OR 模式
const pivotMode = ref('OR')

const pivotModeHint = computed(() => {
  if (store.selectedCatalystIds.length === 0) {
    return '（请先选中属性节点）'
  }
  if (pivotMode.value === 'OR') {
    return 'OR: 显示包含任一选中属性的样本'
  }
  return 'AND: 只显示包含所有选中属性的样本'
})

// 依次对应：省份、食品类别、违规类型、违规项目、包装规格、抽检级别、生产经营主体类型、抽样场所类型
const TYPE_COLORS = ['#36cfc9', '#597ef7', '#ff7a45', '#ffc53d', '#9f7aea', '#13c2c2', '#95de64', '#40a9ff']

// 边颜色配置
const EDGE_COLORS = {
  WEAK: '#5bc8f5',      // 弱关联 (青色)
  MEDIUM: '#fadb14',    // 中等关联 (黄色)
  STRONG: '#fa8c16',    // 强关联 (橙色)
  VERY_STRONG: '#f5222d' // 极强关联 (红色)
}

function getEdgeStyle(weight, maxWeight) {
  const ratio = weight / maxWeight
  
  let color = EDGE_COLORS.WEAK
  let width = 1
  let opacity = 0.4
  let shadowBlur = 0
  
  if (ratio >= 0.8) {
    color = EDGE_COLORS.VERY_STRONG
    width = 4
    opacity = 0.9
    shadowBlur = 8
  } else if (ratio >= 0.6) {
    color = EDGE_COLORS.STRONG
    width = 3
    opacity = 0.8
    shadowBlur = 4
  } else if (ratio >= 0.4) {
    color = EDGE_COLORS.MEDIUM
    width = 2.5
    opacity = 0.7
    shadowBlur = 2
  } else {
    color = EDGE_COLORS.WEAK
    width = 1.5
    opacity = 0.5
    shadowBlur = 0
  }
  
  return { color, width, opacity, shadowBlur, ratio }
}

function buildOption(data, highlightIds) {
  const typeLabels = (data.categories || []).map(c => c.name)
  const nodes = data.nodes.map(n => {
    const isHighlighted = !highlightIds.size || highlightIds.has(n.id)
    const baseColor = TYPE_COLORS[n.category] || '#597ef7'
    const displayName = translateToChinese(n.name)
    return {
      id: n.id,
      name: displayName,
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
        formatter: displayName.length > 10 ? displayName.slice(0, 10) + '…' : displayName,
      },
      extra: n.properties,
    }
  })

  const weights = data.edges.map(e => e.value || 1)
  const maxWeight = Math.max(...weights, 1)
  
  const edges = data.edges.map(e => {
    const weight = e.value || 1
    const edgeStyle = getEdgeStyle(weight, maxWeight)
    
    return {
      source: e.source,
      target: e.target,
      value: weight,
      lineStyle: {
        color: edgeStyle.color,
        width: edgeStyle.width,
        opacity: edgeStyle.opacity,
        shadowBlur: edgeStyle.shadowBlur,
        shadowColor: edgeStyle.color,
        type: 'solid',
      },
    }
  })

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
        formatter: (params) => {
          if (params.dataType === 'node') {
            const p = params.data.extra || {}
            return `<b>${params.data.name}</b><br/>类型：${p.type || typeLabels[params.data.category]}<br/>样本数：${p.count || '-'}`
          }
        if (params.dataType === 'edge') {
          const weight = params.data.value
          const ratio = weight / maxWeight
          let strength = '弱'
          if (ratio >= 0.8) strength = '极强'
          else if (ratio >= 0.6) strength = '强'
          else if (ratio >= 0.4) strength = '中等'
          
          return `
            <div style="font-size:12px;line-height:1.6">
              <b>共现关系</b><br/>
              共现次数: ${weight}<br/>
              关联强度: ${strength} (${Math.round(ratio * 100)}%)
            </div>
          `
        }
        return ''
      },
      backgroundColor: '#1a2540',
      borderColor: '#3060a0',
      textStyle: { color: '#e0e8f0' },
    },
    legend: {
      data: [
        ...typeLabels.map((name, i) => ({ name, icon: 'circle', textStyle: { color: TYPE_COLORS[i] } })),
        { name: '极强关联 (>80%)', icon: 'circle', textStyle: { color: EDGE_COLORS.VERY_STRONG } },
        { name: '强关联 (60-80%)', icon: 'circle', textStyle: { color: EDGE_COLORS.STRONG } },
        { name: '中等关联 (40-60%)', icon: 'circle', textStyle: { color: EDGE_COLORS.MEDIUM } },
        { name: '弱关联 (<40%)', icon: 'circle', textStyle: { color: EDGE_COLORS.WEAK } }
      ],
      top: 4,
      right: 8,
      textStyle: { color: '#a0b8d0', fontSize: 10 },
      itemWidth: 10,
      itemHeight: 10,
      selectedMode: 'multiple',
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: edges,
      categories: typeLabels.map((name, i) => ({ name, itemStyle: { color: TYPE_COLORS[i] } })),
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
        lineStyle: {
          width: 4,
          opacity: 1,
        },
      },
      selectedMode: 'multiple',
      select: {
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: true, color: '#fff' },
      },
    }],
  }
}

// ⭐ 设置 AND/OR 模式并更新 Substrate 高亮
async function setPivotMode(mode) {
  pivotMode.value = mode
  
  if (store.selectedCatalystIds.length === 0) {
    return
  }
  
   // OR 模式：调用现有的 onCatalystSelected
    await store.onCatalystSelected(store.selectedCatalystIds, mode)
  
}


function initChart() {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value, 'dark')

  chart.on('selectchanged', (evt) => {
    if (isClearing) {
      console.log('⏭️ 忽略清除过程中的 selectchanged 事件')
      return
    }
    
    const selectedIndices = new Set()
    
    if (evt.batch && evt.batch.length) {
      evt.batch.forEach(batch => {
        if (batch.selected && batch.selected.length) {
          batch.selected.forEach(series => {
            if (series.dataIndex && series.dataIndex.length) {
              series.dataIndex.forEach(idx => selectedIndices.add(idx))
            }
          })
        }
      })
    } else if (evt.selected && evt.selected.length) {
      evt.selected.forEach(series => {
        if (series.dataIndex && series.dataIndex.length) {
          series.dataIndex.forEach(idx => selectedIndices.add(idx))
        }
      })
    }
    
    const ids = Array.from(selectedIndices)
      .map(idx => store.catalystNetwork.nodes[idx]?.id)
      .filter(Boolean)
    
    const idsKey = ids.slice().sort().join(',')
    const lastKey = lastSelectedIds.slice().sort().join(',')
    
    if (idsKey !== lastKey) {
      lastSelectedIds = ids
      // 只保存选中的属性，不立即更新 Substrate
      store.selectedCatalystIds = ids
      
      // 如果没有选中任何属性，清空 Substrate 高亮
      if (ids.length === 0) {
        store.highlightSampleIds = new Set()
        store.pivotSampleCount = null
      }
    }
  })

  chart.on('dblclick', () => {
    if (isClearing) return
    lastSelectedIds = []
    chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
    store.selectedCatalystIds = []
    store.highlightSampleIds = new Set()
    store.pivotSampleCount = null
  })
}

function renderChart() {
  if (!chart) return
  chart.setOption(buildOption(store.catalystNetwork, store.highlightAttrIds), true)
}

// ⭐ 修复清除函数
function clearSelection() {
  console.log('🟠 开始清除...')
  
  isClearing = true
  lastSelectedIds = []
  
  if (chart) {
    chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
  }
  
  // 清空所有相关状态
  store.selectedCatalystIds = []
  store.highlightSampleIds = new Set()
  store.highlightAttrIds = new Set()
  store.pivotSampleCount = null
  
  nextTick(() => {
    if (chart) {
      renderChart()
      console.log('✅ 清除完成，图表已重新渲染')
    }
    
    setTimeout(() => {
      isClearing = false
      console.log('🔓 清除标志已重置')
    }, 200)
  })
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

watch(() => store.catalystNetwork, () => {
  if (!isClearing) {
    lastSelectedIds = []
    renderChart()
  }
}, { deep: true })

watch(() => store.highlightAttrIds, () => {
  if (!isClearing) {
    renderChart()
  }
}, { deep: true })
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
  color: #ff9966;
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
