<template>
  <div class="app-shell">
    <!-- Header -->
    <header class="app-header">
      <div class="header-left">
        <span class="logo">🍱</span>
        <div>
          <div class="title">食品安全不合格原因可视化分析系统</div>
          <div class="subtitle">基于多层关系网络 · Detangler 启发 · Vue 3 + Spring Boot</div>
        </div>
      </div>
      <div class="header-right">
        <el-tag v-if="store.overview" type="info" size="small">
          共 {{ store.overview.totalSamples.toLocaleString() }} 条不合格记录
        </el-tag>
        <el-tag type="danger" size="small">实时数据</el-tag>
      </div>
    </header>

    <!-- Filter bar -->
    <FilterPanel />

    <!-- Main content - 2x2 Grid Layout -->
    <main class="app-main">
      <!-- 左上：样本网络视图 -->
      <section class="panel">
        <SubstrateNetwork />
      </section>

      <!-- 右上：双向透视 -->
      <section class="panel">
        <PivotPanel />
      </section>

      <!-- 左下：属性关联网络 -->
      <section class="panel">
        <CatalystNetwork />
      </section>

      <!-- 右下：排行榜 -->
      <section class="panel">
        <StatsPanel />
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useMainStore } from './store/index.js'
import FilterPanel from './components/FilterPanel.vue'
import SubstrateNetwork from './components/SubstrateNetwork.vue'
import CatalystNetwork from './components/CatalystNetwork.vue'
import PivotPanel from './components/PivotPanel.vue'
import StatsPanel from './components/StatsPanel.vue'

const store = useMainStore()
onMounted(() => store.init())
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #060d1e;
  overflow: hidden;
}

/* ─── Header ─── */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  background: linear-gradient(90deg, #0a1430 0%, #0d1e45 100%);
  border-bottom: 1px solid #1e3a5f;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo { font-size: 28px; }
.title {
  font-size: 16px;
  font-weight: bold;
  color: #7ab8f5;
  letter-spacing: 0.5px;
}
.subtitle {
  font-size: 11px;
  color: #3a6090;
  margin-top: 2px;
}
.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* ─── Main - 2x2 四宫格布局 ─── */
.app-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;  /* 左侧稍宽，右侧稍窄，让网络图有更多空间 */
  grid-template-rows: 1fr 1fr;          /* 两行等高 */
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}

/* 四个面板通用样式 */
.panel {
  min-height: 0;
  overflow: hidden;
  background: rgba(10, 18, 35, 0.85);
  border: 1px solid #1e3a5f;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
}

/* hover 效果 */
.panel:hover {
  border-color: #2e6a9e;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
</style>