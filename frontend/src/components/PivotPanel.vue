<template>
  <div class="pivot-panel">
    <div class="pivot-title">🔗 双向透视（Pivot）</div>

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

const store = useMainStore()

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
.attr-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100px; }
.attr-count { color: #5bc8f5; font-weight: bold; margin-left: 4px; }
.match-count { color: #ff9966; font-size: 15px; font-weight: bold; }
.hint { color: #607090; font-size: 10px; }
.selected-nodes { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.node-tag { font-size: 10px; }
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
</style>
