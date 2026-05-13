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
      <span class="network-subtitle">节点 = 食品样本 | Ctrl+鼠标拖动可框选</span>
    </div>

    <div 
      class="chart-viewport"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseUp"
    >
      <div v-if="store.loadingSubstrate" class="loading-mask">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>

      <div ref="chartEl" class="chart" />

      <div
        v-show="selecting"
        class="selection-rect"
        :style="{
          left: `${Math.min(startX, endX)}px`,
          top: `${Math.min(startY, endY)}px`,
          width: `${Math.abs(endX - startX)}px`,
          height: `${Math.abs(endY - startY)}px`,
        }"
      />

      <div v-if="store.selectedSubstrateIds.length" class="selection-info">
        已选 {{ store.selectedSubstrateIds.length }} 个样本
        <el-button size="small" type="warning" @click="clearSelection">清除</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { useMainStore } from '../store/index.js'
import { ElMessage } from 'element-plus'
import { translateToChinese } from '../utils/valueTranslation.js'
const store = useMainStore()
const chartEl = ref(null)
let chart = null

// 状态
const pivotMode = ref('OR')
const selecting = ref(false)
const startX = ref(0)
const startY = ref(0)
const endX = ref(0)
const endY = ref(0)
let ctrlPressed = false

let keydownHandler = null
let keyupHandler = null
let resizeHandler = null

const pivotModeHint = computed(() => {
  if (store.selectedSubstrateIds.length === 0) return '（请先选中样本）'
  return pivotMode.value === 'OR'
    ? 'OR: 显示包含任一选中属性的催化剂'
    : 'AND: 只显示包含所有选中属性的催化剂'
})

const GRADE_COLORS = ['#52c1a4', '#faad14', '#fa8c16', '#f5222d']
const GRADE_LABELS = ['轻微(0)', '较轻(1)', '中等(2)', '严重(3)']
// ==============================================
// 核心：判断是否需要力学布局
// ==============================================
function useForceLayout(data) {
  if (!data || !data.nodes || data.nodes.length === 0) return true
  return data.nodes.some(n => n.x === null || n.y === null)
}

// ==============================================
// 构建配置
// ==============================================
function buildOption(data, highlightIds) {
  const needForce = useForceLayout(data)

  const nodes = data.nodes.map(n => {
    const isHighlighted = !highlightIds.size || highlightIds.has(n.id)
    return {
      id: n.id,
      name: n.name,
      category: n.category,
      symbolSize: n.value,
      x: n.x,
      y: n.y,
      itemStyle: {
        color: GRADE_COLORS[n.category] || '#52c1a4',
        opacity: isHighlighted ? 1 : 0.15,
        borderColor: isHighlighted ? '#fff' : 'transparent',
        borderWidth: isHighlighted ? 1 : 0,
      },
      label: { show: false },
      extra: n.properties,
    }
  })

  const filteredEdges = data.edges.filter(e => (e.value || 1) > 5)
  const weights = filteredEdges.map(e => e.value || 1)
  const maxWeight = Math.max(...weights, 1)

  const getEdgeColor = (weight) => {
    const ratio = weight / maxWeight
    if (ratio >= 0.8) return '#f5222d'
    if (ratio >= 0.6) return '#fa8c16'
    if (ratio >= 0.4) return '#fadb14'
    return '#5bc8f5'
  }

  const edges = filteredEdges.map(e => {
    const w = e.value || 1
    return {
      source: e.source,
      target: e.target,
      value: w,
      lineStyle: {
        color: getEdgeColor(w),
        width: 2,
        opacity: 0.8,
        curveness: 0.1,
        shadowBlur: w > maxWeight * 0.7 ? 6 : 0,
        shadowColor: getEdgeColor(w),
      },
    }
  })

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'edge') return `关联强度: ${params.data.value}`
        if (params.dataType !== 'node') return ''
        const p = params.data.extra || {}
        const gradeLabels = { 0: '轻微', 1: '较轻', 2: '中等', 3: '严重' }
        return `
          <div style="font-size:13px;line-height:1.8">
            <b>样品 #${p.id}</b><br/>
             省份：${p.productionLocation || '-'}</br>
             食品类别：${p.foodCategory || '-'}</br>
             违规类型：${p.adulterantCategory || '-'}</br>
             违规项目：${p.adulterant || '-'}</br>
             包装规格：${translateToChinese(p.foodSpecModel)}</br>
             抽检级别：${translateToChinese(p.mandateLevel)}</br>
             生产经营主体类型：${translateToChinese(p.manufacturerType)}</br>
             抽样场所类型：${translateToChinese(p.sampledLocationType)}</br>
             违规等级：${gradeLabels[p.grade] || '-'}
           </div>`
      },
    },
    legend: {
      data: GRADE_LABELS.map((name, i) => ({
        name, icon: 'circle', textStyle: { color: GRADE_COLORS[i] }
      })),
      top: 4, right: 8, textStyle: { color: '#a0b8d0', fontSize: 11 },
    },
    series: [{
      type: 'graph',
      layout: needForce ? 'force' : 'none',
      data: nodes,
      links: edges,
      categories: GRADE_LABELS.map((name, i) => ({ name, itemStyle: { color: GRADE_COLORS[i] } })),
      roam: true,
      draggable: true,
      force: needForce ? {
        repulsion: 220,
        gravity: 0.01,
        edgeLength: [80, 140],
        layoutAnimation: true,
      } : {
        repulsion: 0,
        gravity: 0,
        edgeLength: 0,
        layoutAnimation: false,
      },
      emphasis: { focus: 'adjacency' },
      selectedMode: 'multiple',
      select: { itemStyle: { borderColor: '#5bc8f5', borderWidth: 3, color: '#5bc8f5' } },
    }],
  }
}

// 把变量放到函数外，全局唯一，才能真正锁住
let saveTimer = null
let isLayoutSaved = false

function listenAndSaveLayout() {
  if (!chart) return

  // 【关键】每次进来先清空旧事件 + 清空定时器
  chart.off('rendered')
  clearTimeout(saveTimer)
  
  // 重置保存状态（每次重新布局时允许再次保存）
  isLayoutSaved = false

  chart.on('rendered', () => {
    // 已经保存过，直接跳过
    if (isLayoutSaved) return

    // 标准防抖：动一次就清掉之前的定时器
    clearTimeout(saveTimer)

    saveTimer = setTimeout(() => {
      const seriesIndex = 0
      const data = chart.getModel().getSeriesByIndex(seriesIndex).getData()

      const layout = store.substrateNetwork.nodes.map((node, idx) => {
        const point = data.getItemLayout(idx)
        let x = point ? Math.round(point[0]) : 0
        let y = point ? Math.round(point[1]) : 0

        x = Math.max(-9999, Math.min(9999, x))
        y = Math.max(-9999, Math.min(9999, y))

        return { id: node.id, x, y }
      })

      console.log('✅ 布局已完全稳定，保存坐标')
      store.saveSubstrateLayout(layout)
      
      // ✅ 只会弹一次
      ElMessage.success('布局保存成功！')

      // 锁定：保存后不再执行
      isLayoutSaved = true
      chart.off('rendered')
      clearTimeout(saveTimer)
    }, 2500)
  })
}

// OR/AND
async function setPivotMode(mode) {
  pivotMode.value = mode
  if (store.selectedSubstrateIds.length === 0) return
  await store.onSubstrateSelected(store.selectedSubstrateIds, mode)
}

// 初始化
function initChart() {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value, 'dark')

  chart.on('selectchanged', (evt) => {
    const selectedIndices = new Set()
    if (evt.selected?.[0]) {
      evt.selected[0].dataIndex?.forEach(idx => selectedIndices.add(idx))
    }
    const ids = Array.from(selectedIndices).map(idx => store.substrateNetwork.nodes[idx]?.id).filter(Boolean)
    store.selectedSubstrateIds = ids
  })
}

// 渲染
function renderChart() {
  if (!chart || !store.substrateNetwork) return

  const option = buildOption(store.substrateNetwork, store.highlightSampleIds)
  chart.setOption(option, true)

  if (useForceLayout(store.substrateNetwork)) {
    nextTick(() => listenAndSaveLayout())
  }
}

// 框选
function onMouseDown(e) {
  if (!ctrlPressed) return
  const rt = chartEl.value.getBoundingClientRect()
  selecting.value = true
  startX.value = e.clientX - rt.left
  startY.value = e.clientY - rt.top
  endX.value = startX.value
  endY.value = startY.value
  e.preventDefault()
  e.stopPropagation()
}
function onMouseMove(e) {
  if (!selecting.value) return
  const rt = chartEl.value.getBoundingClientRect()
  endX.value = e.clientX - rt.left
  endY.value = e.clientY - rt.top
}
function onMouseUp() {
  if (!selecting.value) return
  selecting.value = false
  doSelect()
}

// 精准框选
function doSelect() {
  if (!chart) return
  const sx1 = Math.min(startX.value, endX.value)
  const sx2 = Math.max(startX.value, endX.value)
  const sy1 = Math.min(startY.value, endY.value)
  const sy2 = Math.max(startY.value, endY.value)
  if (sx2 - sx1 < 3 && sy2 - sy1 < 3) return

  const seriesModel = chart.getModel().getSeriesByIndex(0)
  const data = seriesModel.getData()
  const cs = seriesModel.coordinateSystem
  if (!cs?.invTransform) return

  const m = cs.invTransform
  const x1 = sx1 * m[0] + sy1 * m[2] + m[4]
  const y1 = sx1 * m[1] + sy1 * m[3] + m[5]
  const x2 = sx2 * m[0] + sy2 * m[2] + m[4]
  const y2 = sx2 * m[1] + sy2 * m[3] + m[5]

  chart.dispatchAction({ type: 'unselect', seriesIndex: 0 })
  const selected = []

  store.substrateNetwork.nodes.forEach((node, idx) => {
    const p = data.getItemLayout(idx)
    if (!p) return
    if (
      p[0] >= Math.min(x1, x2) && p[0] <= Math.max(x1, x2) &&
      p[1] >= Math.min(y1, y2) && p[1] <= Math.max(y1, y2)
    ) {
      selected.push(idx)
    }
  })

  if (selected.length) {
    chart.dispatchAction({ type: 'select', seriesIndex: 0, dataIndex: selected })
  }
}

// 清除
function clearSelection() {
  if (chart) chart.dispatchAction({ type: 'unselect', seriesIndex: 0, dataIndex: 'all' })
  store.selectedSubstrateIds = []
  store.highlightAttrIds = new Set()
  nextTick(() => renderChart())
}

// 生命周期
onMounted(async () => {
  await nextTick()
  initChart()
  renderChart()

  keydownHandler = (e) => {
    if (e.key === 'Control' && !e.altKey && !e.shiftKey && !e.metaKey) {
      ctrlPressed = true
      chart?.setOption({ series: [{ roam: false }] })
    }
  }
  keyupHandler = (e) => {
    if (e.key === 'Control') {
      ctrlPressed = false
      chart?.setOption({ series: [{ roam: true }] })
    }
  }
  resizeHandler = () => chart?.resize()

  window.addEventListener('keydown', keydownHandler)
  window.addEventListener('keyup', keyupHandler)
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  window.removeEventListener('keydown', keydownHandler)
  window.removeEventListener('keyup', keyupHandler)
  window.removeEventListener('resize', resizeHandler)
  chart?.dispose()
  chart = null
})

watch(() => store.substrateNetwork, renderChart, { deep: true })
watch(() => store.highlightSampleIds, renderChart, { deep: true })
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
}
.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
}

.chart-viewport {
  position: relative;
  flex: 1;
  min-height: 0;
  user-select: none;
}
.chart {
  width: 100%;
  height: 100%;
}

.selection-rect {
  position: absolute;
  border: 1px solid #5bc8f5;
  background: rgba(91, 200, 245, 0.15);
  pointer-events: none;
  z-index: 999;
}

.loading-mask {
  position: absolute;
  inset: 0;
  background: rgba(10,15,30,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  color: #5bc8f5;
}
.selection-info {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(20,40,80,0.9);
  border: 1px solid #3060a0;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  color: #7ab8f5;
  z-index: 5;
}
</style>
