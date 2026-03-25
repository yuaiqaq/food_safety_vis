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

    <!-- Main content -->
    <main class="app-main">
      <!-- Left: Substrate network -->
      <section class="panel panel-network">
        <SubstrateNetwork />
      </section>

      <!-- Center: Catalyst network -->
      <section class="panel panel-network">
        <CatalystNetwork />
      </section>

      <!-- Right: Pivot + Stats -->
      <section class="panel panel-side">
        <div class="side-top">
          <PivotPanel />
        </div>
        <div class="side-bottom">
          <StatsPanel />
        </div>
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

/* ─── Main ─── */
.app-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr 320px;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.panel {
  min-height: 0;
  overflow: hidden;
}

.panel-network {
  /* force-directed graphs take full panel height */
}

.panel-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.side-top {
  flex: 0 0 280px;
  min-height: 0;
  overflow: hidden;
}
.side-bottom {
  flex: 1;
  min-height: 0;
  background: rgba(10, 18, 35, 0.95);
  border: 1px solid #1e3a5f;
  border-radius: 8px;
  overflow: hidden;
}
</style>
