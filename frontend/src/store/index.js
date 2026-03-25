import { defineStore } from 'pinia'
import {
  fetchOverview, fetchRegions, fetchCategories, fetchAdulterantCategories,
  fetchSubstrateNetwork, fetchCatalystNetwork,
  pivotSampleToAttr, pivotAttrToSample
} from '../api/index.js'

export const useMainStore = defineStore('main', {
  state: () => ({
    // Filter state
    filters: {
      region: null,
      category: null,
      adulterantCategory: null,
      grade: null,
    },
    // Options for dropdowns
    regions: [],
    categories: [],
    adulterantCategories: [],
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
  }),

  actions: {
    async init() {
      await Promise.all([
        this.loadRegions(),
        this.loadCategories(),
        this.loadAdulterantCategories(),
        this.loadOverview(),
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

    async applyFilters() {
      await Promise.all([
        this.loadSubstrateNetwork(),
        this.loadCatalystNetwork(),
        this.loadOverview(),
      ])
      this.pivotAttrProfile = null
      this.pivotSampleCount = null
    },

    resetFilters() {
      this.filters = { region: null, category: null, adulterantCategory: null, grade: null }
      this.applyFilters()
    },

    /** Pivot: selected substrate samples → highlight catalyst attributes */
    async onSubstrateSelected(nodeIds) {
      this.selectedSubstrateIds = nodeIds
      if (!nodeIds.length) {
        this.highlightAttrIds = new Set()
        this.pivotAttrProfile = null
        return
      }
      const ids = nodeIds.map(id => parseInt(id))
      const res = await pivotSampleToAttr(ids)
      this.pivotAttrProfile = res.data
      // Build set of catalyst node IDs to highlight
      const attrIds = new Set()
      const profile = res.data
      for (const r of Object.keys(profile.regions || {})) attrIds.add('region_' + r)
      for (const c of Object.keys(profile.categories || {})) attrIds.add('cat_' + c)
      for (const ac of Object.keys(profile.adulterantCategories || {})) attrIds.add('acat_' + ac)
      for (const a of Object.keys(profile.adulterants || {})) attrIds.add('adu_' + a)
      this.highlightAttrIds = attrIds
    },

    /** Pivot: selected catalyst attribute nodes → highlight substrate samples */
    async onCatalystSelected(nodeIds) {
      this.selectedCatalystIds = nodeIds
      if (!nodeIds.length) {
        this.highlightSampleIds = new Set()
        this.pivotSampleCount = null
        return
      }
      const res = await pivotAttrToSample(nodeIds)
      this.pivotSampleCount = res.data.count
      this.highlightSampleIds = new Set(res.data.sampleIds.map(String))
    },

    activeFilters() {
      const p = {}
      if (this.filters.region) p.region = this.filters.region
      if (this.filters.category) p.category = this.filters.category
      if (this.filters.adulterantCategory) p.adulterantCategory = this.filters.adulterantCategory
      if (this.filters.grade !== null && this.filters.grade !== undefined) p.grade = this.filters.grade
      return p
    }
  }
})
