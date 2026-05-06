import { defineStore } from 'pinia'
import {
  fetchOverview, fetchRegions, fetchCategories, fetchAdulterantCategories,
  fetchSubstrateNetwork, fetchCatalystNetwork,
  pivotSampleToAttr, pivotAttrToSample,fetchAdulterants,fetchGrade
} from '../api/index.js'

export const useMainStore = defineStore('main', {
  state: () => ({
    // Filter state
    filters: {
      region: null,
      category: null,
      adulterantCategory: null,
      adulterants: null,
    },
    // Options for dropdowns
    regions: [],
    categories: [],
    adulterantCategories: [],
    adulterants: [],
    grade:null,
    // Overview stats
    overview: null,
    // Network data
    substrateNetwork: { nodes: [], edges: [], categories: [] },
    catalystNetwork: { nodes: [], edges: [], categories: [] },
    // Pivot / selection state
    selectedSubstrateIds: [],    // selected sample node IDs (strings)
    selectedCatalystIds: [],     // selected catalyst node IDs (strings)
    highlightSampleIds: new Set(), // highlighted by pivot
    highlightAttrIds: new Set(),   // highlighted by pivot
    // Pivot result
    pivotAttrProfile: null,
    pivotSampleCount: null,
    // Loading flags
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
        this.loadAdulterants() ,
        this.loadOverview(),
        this.loadFiltersGrades(), // 提前加载grade数据
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

    async loadSubstrateNetwork() {
      this.loadingSubstrate = true
      try {
        const params = this.activeFilters()
        params.maxNodes = 200
        const res = await fetchSubstrateNetwork(params)
        this.substrateNetwork = res.data
        this.selectedSubstrateIds = []
        this.highlightSampleIds = new Set()
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
    console.log('loadFiltersGrades 开始执行')  // 调试日志
    this.loadingGroup = true
    try {
      const param = this.activeFilters()
      console.log('grade请求参数:', param)  // 调试日志
      const res = await fetchGrade(param)
      console.log('grade返回数据:', res.data)  // 调试日志
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

    /** Pivot: selected substrate samples → highlight catalyst attributes */
    async onSubstrateSelected(nodeIds,mode) {
      this.selectedSubstrateIds = nodeIds
      if (!nodeIds.length) {
        this.highlightAttrIds = new Set()
        this.pivotAttrProfile = null
        return
      }
      const ids = nodeIds.map(id => parseInt(id))
      const res = await pivotSampleToAttr(ids,mode)
      this.pivotAttrProfile = res.data
      // Build set of catalyst node IDs to highlight
      const attrIds = new Set()
      const profile = res.data
      console.log('Pivot profile:', profile) // 调试日志
      for (const r of Object.keys(profile.regions || {})) attrIds.add('region_' + r)
      for (const c of Object.keys(profile.categories || {})) attrIds.add('cat_' + c)
      for (const ac of Object.keys(profile.adulterantCategories || {})) attrIds.add('acat_' + ac)
      for (const a of Object.keys(profile.adulterants || {})) attrIds.add('adu_' + a)
      this.highlightAttrIds = attrIds
      console.log('Highlighted attribute IDs:', attrIds) // 调试日志
    },

/** Pivot: selected catalyst attribute nodes → highlight substrate samples */
async onCatalystSelected(nodeIds, mode) {
  this.selectedCatalystIds = nodeIds;
  if (!nodeIds.length) {
    this.highlightSampleIds = new Set();
    this.pivotSampleCount = null;
    return;
  }

  // ✅ 关键：把页面上的筛选器一起传给后端！
  const activeFilters = this.activeFilters();

  // ✅ 调用后端时，同时传递：节点IDs + mode + 筛选条件
  const res = await pivotAttrToSample(nodeIds, mode, activeFilters);

  this.pivotSampleCount = res.data.count;
 // 👇 空的时候插入一个不存在的 ID -1，保证不会高亮任何节点
this.highlightSampleIds = res.data.sampleIds.length 
  ? new Set(res.data.sampleIds.map(String)) 
  : new Set(['-1']);
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
