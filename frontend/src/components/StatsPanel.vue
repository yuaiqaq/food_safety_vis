<template>
  <div class="stats-panel">
    <!-- KPI Cards -->
    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-value">{{ overview?.totalSamples?.toLocaleString() ?? '-' }}</div>
        <div class="kpi-label">不合格样本总数</div>
      </div>
      <div v-for="(val, key) in overview?.byGrade" :key="key" class="kpi-card" :style="{borderColor: gradeColor(key)}">
        <div class="kpi-value" :style="{color: gradeColor(key)}">{{ val.toLocaleString() }}</div>
        <div class="kpi-label">{{ key }}</div>
      </div>
    </div>

    <!-- Charts row -->
    <div class="charts-row">
      <div class="chart-block">
        <div class="chart-label">按省份分布（Top 15）</div>
        <div ref="regionChartEl" class="mini-chart" />
      </div>
      <div class="chart-block">
        <div class="chart-label">按食品类别分布</div>
        <div ref="categoryChartEl" class="mini-chart" />
      </div>
      <div class="chart-block">
        <div class="chart-label">违规类型分布</div>
        <div ref="adulterantCatChartEl" class="mini-chart" />
      </div>
      <div class="chart-block">
        <div class="chart-label">Top 20 违规项目</div>
        <div ref="adulterantChartEl" class="mini-chart" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { useMainStore } from '../store/index.js'

const store = useMainStore()
const overview = computed(() => store.overview)

const regionChartEl = ref(null)
const categoryChartEl = ref(null)
const adulterantCatChartEl = ref(null)
const adulterantChartEl = ref(null)

let charts = []

const GRADE_COLORS_MAP = {
  'Grade 0': '#52c41a',
  'Grade 1': '#faad14',
  'Grade 2': '#fa8c16',
  'Grade 3': '#f5222d',
}
const PALETTE = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']

function gradeColor(key) {
  return GRADE_COLORS_MAP[key] || '#5bc8f5'
}

function makeBarH(el, data, colorFn) {
  const c = echarts.init(el, 'dark')
  c.setOption({
    backgroundColor: 'transparent',
    grid: { left: 8, right: 8, top: 4, bottom: 4, containLabel: true },
    xAxis: { type: 'value', axisLabel: { color: '#607090', fontSize: 9 }, splitLine: { lineStyle: { color: '#1e3a5f' } } },
    yAxis: {
      type: 'category',
      data: data.map(d => d.name),
      axisLabel: { color: '#a0b8d0', fontSize: 9, formatter: v => v.length > 12 ? v.slice(0, 12) + '…' : v },
      inverse: true,
    },
    series: [{
      type: 'bar',
      data: data.map((d, i) => ({ value: d.value, itemStyle: { color: colorFn ? colorFn(d, i) : PALETTE[i % PALETTE.length] } })),
      label: { show: true, position: 'right', color: '#a0b8d0', fontSize: 9, formatter: '{c}' },
      barMaxWidth: 14,
    }],
    tooltip: { trigger: 'axis', backgroundColor: '#1a2540', borderColor: '#3060a0', textStyle: { color: '#e0e8f0', fontSize: 11 } },
  })
  return c
}

function makePie(el, data) {
  const c = echarts.init(el, 'dark')
  c.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: '#1a2540', borderColor: '#3060a0', textStyle: { color: '#e0e8f0', fontSize: 11 } },
    legend: { type: 'scroll', orient: 'vertical', right: 4, top: 'middle', textStyle: { color: '#a0b8d0', fontSize: 9 }, itemWidth: 8, itemHeight: 8 },
    series: [{
      type: 'pie',
      center: ['35%', '50%'],
      radius: ['35%', '65%'],
      data: data.map((d, i) => ({ name: d.name, value: d.value, itemStyle: { color: PALETTE[i % PALETTE.length] } })),
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 10, fontWeight: 'bold', color: '#fff' } },
    }],
  })
  return c
}

function renderCharts() {
  if (!overview.value) return
  const ov = overview.value

  // Clear old charts
  charts.forEach(c => c.dispose())
  charts = []

  nextTick(() => {
    if (regionChartEl.value && ov.byRegion?.length)
      charts.push(makeBarH(regionChartEl.value, ov.byRegion))

    if (categoryChartEl.value && ov.byCategory?.length)
      charts.push(makePie(categoryChartEl.value, ov.byCategory))

    if (adulterantCatChartEl.value && ov.byAdulterantCategory?.length)
      charts.push(makePie(adulterantCatChartEl.value, ov.byAdulterantCategory))

    if (adulterantChartEl.value && ov.topAdulterants?.length) {
      const top = ov.topAdulterants.slice(0, 20)
      charts.push(makeBarH(adulterantChartEl.value, top))
    }
  })
}

onMounted(() => { renderCharts() })
watch(overview, renderCharts)
onUnmounted(() => charts.forEach(c => c.dispose()))
</script>

<style scoped>
.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 12px;
  height: 100%;
  overflow: hidden;
}
.kpi-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.kpi-card {
  background: rgba(15, 25, 50, 0.8);
  border: 1px solid #1e4a8f;
  border-radius: 6px;
  padding: 6px 12px;
  min-width: 90px;
  text-align: center;
  transition: border-color 0.2s;
}
.kpi-value {
  font-size: 20px;
  font-weight: bold;
  color: #5bc8f5;
  line-height: 1.2;
}
.kpi-label {
  font-size: 10px;
  color: #607090;
  margin-top: 2px;
}
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 8px;
  flex: 1;
  min-height: 0;
}
.chart-block {
  background: rgba(15, 25, 50, 0.6);
  border: 1px solid #1e3a5f;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chart-label {
  font-size: 11px;
  color: #607090;
  padding: 4px 8px;
  border-bottom: 1px solid #1e3a5f;
}
.mini-chart {
  flex: 1;
  min-height: 0;
}
</style>
