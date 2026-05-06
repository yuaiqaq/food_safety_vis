<template>
  <div class="stats-panel">
    <!-- 改为上下两行，每行两个图表 -->
    <div class="charts-row">
      <div class="chart-block">
        <div class="chart-label">📍 按省份分布（Top 15）</div>
        <div ref="regionChartEl" class="mini-chart" />
      </div>
      <div class="chart-block">
        <div class="chart-label">🍱 按食品类别分布</div>
        <div ref="categoryChartEl" class="mini-chart" />
      </div>
    </div>
    <div class="charts-row">
      <div class="chart-block">
        <div class="chart-label">⚠️ 违规类型分布</div>
        <div ref="adulterantCatChartEl" class="mini-chart" />
      </div>
      <div class="chart-block">
        <div class="chart-label">📋 Top 15 违规项目</div>
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

// 颜色调色板
const PALETTE = [
  '#5470c6', '#fac858', '#ee6666', '#73c0de', '#3ba272', 
  '#fc8452', '#9a60b4', '#ea7ccc', '#ff9f7c', '#87d4a0',
  '#6e9ef1', '#f7b35b', '#f28b6c', '#84c0a0', '#b48ead',
]

// ========== 完整映射表 ==========
const provinceMap = {
  '广西壮族自治区': '广西', '新疆维吾尔自治区': '新疆', '内蒙古自治区': '内蒙古',
  '宁夏回族自治区': '宁夏', '西藏自治区': '西藏', '黑龙江省': '黑龙江',
  '吉林省': '吉林', '辽宁省': '辽宁', '河北省': '河北', '河南省': '河南',
  '山东省': '山东', '山西省': '山西', '湖南省': '湖南', '湖北省': '湖北',
  '江苏省': '江苏', '浙江省': '浙江', '安徽省': '安徽', '江西省': '江西',
  '福建省': '福建', '广东省': '广东', '海南省': '海南', '贵州省': '贵州',
  '云南省': '云南', '四川省': '四川', '陕西省': '陕西', '甘肃省': '甘肃', '青海省': '青海',
}

const categoryMap = {
  // 基础类别
  'pastry': '糕点',
  'alcohol': '酒类',
  'bee product': '蜂蜜产品',
  'sugar': '食糖',
    'snack': '零食',
  'condiment': '调味品',
  'tea and tea product': '茶叶',
  'confectionery': '糖果',
  'convenience food': '方便食品',
  'restaurant food': '餐饮食品',
  'frozen ice and ice cream': '冷冻饮品',
  
  // 肉/蛋/奶/水产
  'meat and meat product': '肉制品',
  'aquatic product': '水产品',
  'bee product': '蜂产品',
  'egg and egg product': '蛋制品',
  'bean and bean product': '豆制品',
  'nut and nut product': '坚果制品',
  
  // 蔬果/谷物
  'vegetable and vegetable product': '蔬菜制品',
  'fruit and fruit product': '水果制品',
  'cereal and cereal product': '谷物制品',
  
  // 油脂类（精确匹配）
  'edible oil, fat and their product': '食用油',
  '\"edible oil': '食用油',  // 添加备用匹配
  'fat and their product\"': '食用油',
}


const adulterantCatMap = {
  'Specification': '规格问题', 'Microbial contamination': '微生物污染',
  'Food additive': '食品添加剂', 'Environmental contaminant': '环境污染物',
  'Pesticide and veterinary drug': '农兽药残留', 'Toxin': '毒素',
}

// 名称转换
function convertName(name) {
  if (!name) return ''
  
  // 清理：去除首尾空格和双引号
  let cleanName = name.trim()
  // 去掉开头和结尾的双引号
  if (cleanName.startsWith('"') && cleanName.endsWith('"')) {
    cleanName = cleanName.slice(1, -1)
  }
  
  // 使用清理后的名称进行匹配
  if (provinceMap[cleanName]) return provinceMap[cleanName]
  if (categoryMap[cleanName]) return categoryMap[cleanName]
  if (adulterantCatMap[cleanName]) return adulterantCatMap[cleanName]
  
  // 如果还没匹配，打印警告
  if (!categoryMap[cleanName] && !provinceMap[cleanName]) {
    console.warn('未映射的名称:', `"${cleanName}"`)
  }
  
  // 违规项目：超过10个字符就截断
  if (cleanName.length > 10) return cleanName.slice(0, 9) + '…'
  return cleanName
}

// 条形图函数（优化版）
function makeBarChart(el, data, title) {
  if (!data || data.length === 0) return null
  
  // 降序排序
  const sortedData = [...data].sort((a, b) => a.value - b.value)
  const maxValue = Math.max(...sortedData.map(d => d.value))
  
  const c = echarts.init(el, 'dark')
  c.setOption({
    backgroundColor: 'transparent',
    grid: { 
      left: '20%',    // 给左侧标签留更多空间
      right: '8%', 
      top: 20, 
      bottom: 25,
      containLabel: false
    },
    xAxis: { 
      type: 'value', 
      max: maxValue,
      axisLabel: { color: '#607090', fontSize: 10 }, 
      splitLine: { lineStyle: { color: '#1e3a5f' } },
      name: '样本数',
      nameTextStyle: { color: '#607090', fontSize: 9 },
      nameLocation: 'middle',
      nameGap: 30,
    },
    yAxis: {
      type: 'category',
      data: sortedData.map(d => convertName(d.name)),
      axisLabel: { 
        color: '#a0b8d0', 
        fontSize: 10,
        overflow: 'break',
        width: 85,
      },
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [{
      type: 'bar',
      data: sortedData.map((d, i) => ({ 
        value: d.value, 
        itemStyle: { 
          color: PALETTE[i % PALETTE.length],
          borderRadius: [0, 4, 4, 0],
        } 
      })),
      label: { 
        show: true, 
        position: 'right', 
        color: '#a0b8d0', 
        fontSize: 10, 
        formatter: (params) => {
          return params.value
        }
      },
      barWidth: '55%',
    }],
    tooltip: { 
      trigger: 'axis', 
      axisPointer: { type: 'shadow' },
      backgroundColor: '#1a2540', 
      borderColor: '#3060a0', 
      textStyle: { color: '#e0e8f0', fontSize: 11 },
      formatter: function(params) {
        if (params && params[0]) {
          const shortName = params[0].name
          const originalItem = sortedData.find(d => convertName(d.name) === shortName)
          const fullName = originalItem ? originalItem.name : shortName
          return `<strong>${fullName}</strong><br/>样本数: ${params[0].value}`
        }
        return ''
      }
    },
  })
  return c
}

function renderCharts() {
  if (!overview.value) {
    console.log('Overview data not available')
    return
  }
  
  const ov = overview.value
    if (ov.byCategory) {
    ov.byCategory.forEach(item => {
      if (!categoryMap[item.name] && !provinceMap[item.name] && !adulterantCatMap[item.name]) {
        console.warn('未映射的类别:', item.name, '数值:', item.value)
      }
    })
  }
  console.log('渲染图表，数据:', ov)

  charts.forEach(c => c && c.dispose())
  charts = []
nextTick(() => {
    if (regionChartEl.value && ov.byRegion?.length) {
      // 限制显示数量
      const top = ov.byRegion.slice(0, 15)
      charts.push(makeBarChart(regionChartEl.value, top, '省份'))
    }
    if (categoryChartEl.value && ov.byCategory?.length) {
      // 限制显示数量，确保糕点能显示
      const top = ov.byCategory.slice(0, 15)
      charts.push(makeBarChart(categoryChartEl.value, top, '食品类别'))
    }
    if (adulterantCatChartEl.value && ov.byAdulterantCategory?.length) {
      charts.push(makeBarChart(adulterantCatChartEl.value, ov.byAdulterantCategory, '违规类型'))
    }
    if (adulterantChartEl.value && ov.topAdulterants?.length) {
      const top = ov.topAdulterants.slice(0, 15)
      charts.push(makeBarChart(adulterantChartEl.value, top, '违规项目'))
    }
  })
}


onMounted(() => { renderCharts() })
watch(overview, renderCharts, { deep: true })
onUnmounted(() => charts.forEach(c => c && c.dispose()))
</script>

<style scoped>
.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  height: 100%;
  overflow: auto;
  background: linear-gradient(135deg, #0d1628 0%, #0a1220 100%);
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  flex: 1;
  min-height: 280px;
}

.chart-block {
  background: rgba(15, 25, 50, 0.6);
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.2s;
}

.chart-block:hover {
  border-color: #3060a0;
}

.chart-label {
  font-size: 12px;
  font-weight: 500;
  color: #7ab8f5;
  padding: 8px 12px;
  border-bottom: 1px solid #1e3a5f;
  background: rgba(20, 40, 80, 0.3);
}

.mini-chart {
  flex: 1;
  min-height: 240px;
  width: 100%;
}
</style>