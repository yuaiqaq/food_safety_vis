import axios from 'axios'

const api = axios.create({
  baseURL: 'http://yuaiqaq.gnway.cc:80/api',
  timeout: 15000,
})

export const fetchOverview = () => api.get('/stats/overview')
export const fetchRegions = () => api.get('/stats/regions')
export const fetchCategories = () => api.get('/stats/categories')
export const fetchGrade = (params) => api.get('/stats/grade', { params })
export const fetchAdulterantCategories = () => api.get('/stats/adulterant-categories')
export const fetchAdulterants = () => api.get('/stats/adulterants')
export const fetchSubstrateNetwork = (params) => api.get('/network/substrate', { params })
export const fetchCatalystNetwork = (params) => api.get('/network/catalyst', { params })


export const fetchSamples = (params) => api.get('/samples', { params })
export const fetchSampleById = (id) => api.get(`/samples/${id}`)

export const pivotSampleToAttr = (ids, mode) => api.post('/pivot/sample-to-attr', { ids, mode })
export const pivotAttrToSample = (ids, mode, filters) => api.post('/pivot/attr-to-sample', { ids, mode, ...filters })
export const saveSubstrateLayout = (data) => api.post('/network/save-layout', data)
export const getSubstrateLayout = (filterKey) => api.get('/network/substrate/layout', { params: { filterKey } })

// api/index.js

