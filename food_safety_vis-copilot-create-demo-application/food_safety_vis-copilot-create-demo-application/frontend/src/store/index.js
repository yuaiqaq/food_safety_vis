import { defineStore } from 'pinia'
import {
  fetchOverview, fetchRegions, fetchCategories, fetchAdulterantCategories,
  fetchSubstrateNetwork, fetchCatalystNetwork,
  pivotSampleToAttr, pivotAttrToSample, fetchAdulterants, fetchGrade,
  saveSubstrateLayout,
} from '../api/index.js'

// ✅ 安全 Base64（支持中文，不报错）
function toBase64(str) {
  if (typeof TextEncoder !== 'undefined') {
    const encoder = new TextEncoder()
    const uint8Array = encoder.encode(str)
    let binary = ''
    for (let i = 0; i < uint8Array.length; i++) {
      binary += String.fromCharCode(uint8Array[i])
    }
    return btoa(binary)
  }

  return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, (_, p1) =>
    String.fromCharCode(parseInt(p1, 16))
  ))
}

export const useMainStore = defineStore('main', {
  state: () => ({
    filters: {
      region: null,
      category: null,
      adulterantCategory: null,
      adulterants: null,
    },
    regions: [],
    categories: [],
    adulterantCategories: [],
    adulterants: [],
    grade: null,
    overview: null,
    substrateNetwork: { nodes: [], edges: [], categories: [] },
    catalystNetwork: { nodes: [], edges: [], categories: [] },
    selectedSubstrateIds: [],
    selectedCatalystIds: [],
    highlightSampleIds: new Set(),
    highlightAttrIds: new Set(),
    pivotAttrProfile: null,
    pivotSampleCount: null,
    loadingSubstrate: false,
    loadingCatalyst: false,
    loadingOverview: false,
    loadingGroup: false,
  }),

  actions: {
    async init() {
      await Promise.all([
        this.loadRegions(),
        this.loadCategories(),
        this.loadAdulterantCategories(),
        this.loadAdulterants(),
        this.loadOverview(),
        this.loadFiltersGrades(),
      ])
      await Promise.all([
        this.loadSubstrateNetwork(),
        this.loadCatalystNetwork(),
      ])
    },

    async loadRegions() {
      const res = await fetchRegions()
      this.regions = res.data
    },
    async loadCategories() {
      const res = await fetchCategories()
      this.categories = res.data
    },
    async loadAdulterantCategories() {
      const res = await fetchAdulterantCategories()
      this.adulterantCategories = res.data
    },
    async loadAdulterants() {
      const res = await fetchAdulterants()
      this.adulterants = res.data
    },
    async loadOverview() {
      this.loadingOverview = true
      try {
        const res = await fetchOverview()
        this.overview = res.data
      } finally {
        this.loadingOverview = false
      }
    },

    // ====================== ✅ 修复：查询时统一 filterKey ======================
    async loadSubstrateNetwork() {
      this.loadingSubstrate = true
      try {
        // 1. 获取纯筛选条件
        const params = this.activeFilters()

        // 2. ✅ 生成统一的 filterKey（绝对不带 maxNodes）
        const filterKey = toBase64(JSON.stringify(params))

        // 3. 追加参数，不影响 filterKey
        params.maxNodes = 200
        params.filterKey = filterKey

        const res = await fetchSubstrateNetwork(params)
        this.substrateNetwork = res.data

        this.selectedSubstrateIds = []
        this.highlightSampleIds = new Set()
      } catch (err) {
        console.error("❌ 请求底物网络失败：", err)
      } finally {
        this.loadingSubstrate = false
      }
    },

    async loadCatalystNetwork() {
      this.loadingCatalyst = true
      try {
        const params = this.activeFilters()
        const res = await fetchCatalystNetwork(params)
        this.catalystNetwork = res.data
        this.selectedCatalystIds = []
        this.highlightAttrIds = new Set()
      } finally {
        this.loadingCatalyst = false
      }
    },

    async loadFiltersGrades() {
      console.log('loadFiltersGrades 开始执行')
      this.loadingGroup = true
      try {
        const param = this.activeFilters()
        console.log('grade请求参数:', param)
        const res = await fetchGrade(param)
        console.log('grade返回数据:', res.data)
        this.grade = res.data
      } catch (error) {
        console.error('加载grade数据失败:', error)
      } finally {
        this.loadingGroup = false
      }
    },

    async applyFilters() {
      await Promise.all([
        this.loadSubstrateNetwork(),
        this.loadCatalystNetwork(),
        this.loadFiltersGrades(),
        this.loadOverview(),
      ])
      this.pivotAttrProfile = null
      this.pivotSampleCount = null
    },

    resetFilters() {
      this.filters = { region: null, category: null, adulterantCategory: null, adulterants: null }
      this.applyFilters()
    },

    async onSubstrateSelected(nodeIds, mode) {
      this.selectedSubstrateIds = nodeIds
      if (!nodeIds.length) {
        this.highlightAttrIds = new Set()
        this.pivotAttrProfile = null
        return
      }
      const ids = nodeIds.map(id => parseInt(id))
      const res = await pivotSampleToAttr(ids, mode)
      this.pivotAttrProfile = res.data
      const attrIds = new Set()
      const profile = res.data
      console.log('Pivot profile:', profile)
      for (const r of Object.keys(profile.regions || {})) attrIds.add('region_' + r)
      for (const c of Object.keys(profile.categories || {})) attrIds.add('cat_' + c)
      for (const ac of Object.keys(profile.adulterantCategories || {})) attrIds.add('acat_' + ac)
      for (const a of Object.keys(profile.adulterants || {})) attrIds.add('adu_' + a)
      for (const fsm of Object.keys(profile.foodSpecModels || {})) attrIds.add('fsm_' + fsm)
      for (const ml of Object.keys(profile.mandateLevels || {})) attrIds.add('ml_' + ml)
      for (const mft of Object.keys(profile.manufacturerTypes || {})) attrIds.add('mft_' + mft)
      for (const slt of Object.keys(profile.sampledLocationTypes || {})) attrIds.add('slt_' + slt)
      this.highlightAttrIds = attrIds
      console.log('Highlighted attribute IDs:', attrIds)
    },

    async onCatalystSelected(nodeIds, mode) {
      this.selectedCatalystIds = nodeIds;
      if (!nodeIds.length) {
        this.highlightSampleIds = new Set();
        this.pivotSampleCount = null;
        return;
      }
      const activeFilters = this.activeFilters();
      const res = await pivotAttrToSample(nodeIds, mode, activeFilters);
      this.pivotSampleCount = res.data.count;
      this.highlightSampleIds = res.data.sampleIds.length
        ? new Set(res.data.sampleIds.map(String))
        : new Set(['-1']);
    },

    // ====================== ✅ 修复：保存时使用完全相同的 filterKey ======================
    async saveSubstrateLayout(nodes) {
      // ✅ 关键：和查询用同一个筛选条件
      const filterObj = this.activeFilters()
      const filterKey = toBase64(JSON.stringify(filterObj))
      await saveSubstrateLayout({ filterKey, nodes })
    },

    activeFilters() {
      const p = {}
      if (this.filters.region) p.region = this.filters.region
      if (this.filters.category) p.category = this.filters.category
      if (this.filters.adulterantCategory) p.adulterantCategory = this.filters.adulterantCategory
      if (this.filters.adulterants !== null && this.filters.adulterants !== undefined) p.adulterants = this.filters.adulterants
      return p
    }
  }
})
