<template>
  <div class="filter-panel">
    <div class="filter-title">🔍 筛选条件</div>
    <div class="filter-row">
      <el-select
        v-model="store.filters.region"
        placeholder="省份"
        clearable
        size="small"
        style="width:130px"
      >
        <el-option v-for="r in store.regions" :key="r" :label="r" :value="r" />
      </el-select>

      <el-select
        v-model="store.filters.category"
        placeholder="食品类别"
        clearable
        size="small"
        style="width:165px"
      >
        <el-option v-for="c in store.categories" :key="c" :label="c" :value="c" />
      </el-select>

      <el-select
        v-model="store.filters.adulterantCategory"
        placeholder="违规类型"
        clearable
        size="small"
        style="width:185px"
      >
        <el-option v-for="ac in store.adulterantCategories" :key="ac" :label="ac" :value="ac" />
      </el-select>

      <el-select
        v-model="store.filters.grade"
        placeholder="违规等级"
        clearable
        size="small"
        style="width:130px"
      >
        <el-option label="Grade 0 — 轻微" :value="0" />
        <el-option label="Grade 1 — 较轻" :value="1" />
        <el-option label="Grade 2 — 中等" :value="2" />
        <el-option label="Grade 3 — 严重" :value="3" />
      </el-select>

      <el-button type="primary" size="small" @click="store.applyFilters()">
        <el-icon><Search /></el-icon> 查询
      </el-button>
      <el-button size="small" @click="store.resetFilters()">重置</el-button>
    </div>
  </div>
</template>

<script setup>
import { useMainStore } from '../store/index.js'
const store = useMainStore()
</script>

<style scoped>
.filter-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(15, 25, 50, 0.9);
  border-bottom: 1px solid #1e3a5f;
}
.filter-title {
  font-size: 13px;
  font-weight: bold;
  color: #5bc8f5;
  white-space: nowrap;
}
.filter-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
