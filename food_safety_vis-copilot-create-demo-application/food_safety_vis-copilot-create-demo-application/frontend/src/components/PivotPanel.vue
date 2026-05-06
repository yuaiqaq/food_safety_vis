<template>
  <div class="pivot-panel">
    <div class="pivot-title">🔗 双向透视（Pivot）</div>

    <!-- KPI Cards - 动态显示 overview 或 grade -->
    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-value">{{ totalSamples?.toLocaleString() ?? '-' }}</div>
        <div class="kpi-label">不合格样本总数</div>
      </div>
      <div 
        v-for="(val, key) in currentGradeData" 
        :key="key" 
        class="kpi-card" 
        :style="{borderColor: gradeColor(key)}"
      >
        <div class="kpi-value" :style="{color: gradeColor(key)}">{{ val.toLocaleString() }}</div>
        <div class="kpi-label">{{ gradeName(key) }}</div>
      </div>
    </div>
    
    <!-- 其余内容保持不变 -->
    <!-- From samples → attributes -->
    <div v-if="store.pivotAttrProfile" class="pivot-section">
      <div class="pivot-section-title">↑ 选中样本的属性画像（{{ store.pivotAttrProfile.sampleCount }} 个样本）</div>
      <div class="pivot-grid">
        <div v-for="(counts, type) in profileDisplay" :key="type" class="pivot-attr-group">
          <div class="attr-group-title">{{ typeLabels[type] }}</div>
          <div v-for="(cnt, name) in counts" :key="name" class="attr-item">
            <span class="attr-name">{{ name }}</span>
            <span class="attr-count">{{ cnt }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- From attributes → samples -->
    <div v-if="store.pivotSampleCount !== null" class="pivot-section">
      <div class="pivot-section-title">
        ↓ 属性节点匹配到
        <span class="match-count">{{ store.pivotSampleCount }}</span> 个样本
        <span class="hint">（在样本网络中已高亮显示）</span>
      </div>
      <div class="selected-nodes">
        <el-tag v-for="nodeId in store.selectedCatalystIds" :key="nodeId" size="small" class="node-tag">
          {{ nodeId.replace(/^(region_|cat_|acat_|adu_)/, '') }}
        </el-tag>
      </div>
    </div>

    <div v-if="!store.pivotAttrProfile && store.pivotSampleCount === null" class="pivot-hint">
      <el-icon><InfoFilled /></el-icon>
      在左侧<b>样本网络</b>中点击节点，查看对应属性；<br/>
      在右侧<b>属性网络</b>中点击节点，高亮匹配样本。
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useMainStore } from '../store/index.js'
import { InfoFilled } from '@element-plus/icons-vue'

const store = useMainStore()

const overview = computed(() => store.overview)
const grade = computed(() => store.grade)

// 判断是否有筛选条件激活
const hasActiveFilters = computed(() => {
  return !!(store.filters.region || 
            store.filters.category || 
            store.filters.adulterantCategory || 
            store.filters.adulterants)
})

// 当前显示的等级数据（优先显示筛选后的 grade，否则显示 overview）
const currentGradeData = computed(() => {
  // 如果有筛选条件且 grade 有数据，显示筛选后的数据
  if (hasActiveFilters.value && grade.value) {
    return grade.value
  }
  // 否则显示 overview 的数据
  return overview.value?.byGrade || {}
})

// 总样本数（根据当前显示的数据计算）
const totalSamples = computed(() => {
  const data = currentGradeData.value
  if (!data || Object.keys(data).length === 0) return null
  
  // 计算所有等级的总和
  return Object.values(data).reduce((sum, count) => sum + count, 0)
})

const GRADE_COLORS_MAP = {
  'Grade 0': '#52c41a',   // 轻微 - 绿色
  'Grade 1': '#faad14',   // 较轻 - 橙色
  'Grade 2': '#fa8c16',   // 中等 - 橙红
  'Grade 3': '#f5222d',   // 严重 - 红色
}

// 等级名称映射
const GRADE_NAME_MAP = {
  'Grade 0': '轻微',
  'Grade 1': '较轻',
  'Grade 2': '中等',
  'Grade 3': '严重',
}

function gradeColor(key) {
  return GRADE_COLORS_MAP[key] || '#5bc8f5'
}

function gradeName(key) {
  return GRADE_NAME_MAP[key] || key
}

const typeLabels = {
  regions: '省份',
  categories: '食品类别',
  adulterantCategories: '违规类型',
  adulterants: '违规项目（Top10）',
}

const profileDisplay = computed(() => {
  const p = store.pivotAttrProfile
  if (!p) return {}
  return {
    regions: p.regions,
    categories: p.categories,
    adulterantCategories: p.adulterantCategories,
    adulterants: p.adulterants,
  }
})
</script>

<style scoped>
.pivot-panel {
  height: 100%;
  background: rgba(10, 18, 35, 0.95);
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  padding: 10px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pivot-title {
  font-size: 13px;
  font-weight: bold;
  color: #5bc8f5;
  border-bottom: 1px solid #1e3a5f;
  padding-bottom: 6px;
}
.pivot-section {
  background: rgba(15, 30, 60, 0.5);
  border: 1px solid #1e3a5f;
  border-radius: 6px;
  padding: 8px;
}
.pivot-section-title {
  font-size: 11px;
  color: #7ab8f5;
  margin-bottom: 6px;
}
.pivot-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.pivot-attr-group {
  background: rgba(20, 40, 80, 0.4);
  border-radius: 4px;
  padding: 4px 6px;
}
.attr-group-title {
  font-size: 10px;
  color: #607090;
  margin-bottom: 3px;
  font-weight: bold;
}
.attr-item {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #a0c8e0;
  padding: 1px 0;
}
.attr-name { 
  overflow: hidden; 
  text-overflow: ellipsis; 
  white-space: nowrap; 
  max-width: 100px; 
}
.attr-count { 
  color: #5bc8f5; 
  font-weight: bold; 
  margin-left: 4px; 
}
.match-count { 
  color: #ff9966; 
  font-size: 15px; 
  font-weight: bold; 
}
.hint { 
  color: #607090; 
  font-size: 10px; 
}
.selected-nodes { 
  display: flex; 
  flex-wrap: wrap; 
  gap: 4px; 
  margin-top: 4px; 
}
.node-tag { 
  font-size: 10px; 
}
.pivot-hint {
  color: #607090;
  font-size: 11px;
  line-height: 1.8;
  padding: 10px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.kpi-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.kpi-card {
  background: rgba(15, 25, 50, 0.8);
  border-left: 3px solid #5bc8f5;
  border-radius: 6px;
  padding: 8px 12px;
  min-width: 70px;
  flex: 1;
  text-align: center;
  transition: all 0.2s;
}
.kpi-value {
  font-size: 22px;
  font-weight: bold;
  color: #5bc8f5;
  line-height: 1.2;
}
.kpi-label {
  font-size: 11px;
  color: #607090;
  margin-top: 4px;
}
</style>