import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
})

export const fetchOverview = () => api.get('/stats/overview')
export const fetchRegions = () => api.get('/stats/regions')
export const fetchCategories = () => api.get('/stats/categories')
export const fetchAdulterantCategories = () => api.get('/stats/adulterant-categories')

export const fetchSubstrateNetwork = (params) => api.get('/network/substrate', { params })
export const fetchCatalystNetwork = (params) => api.get('/network/catalyst', { params })

export const fetchSamples = (params) => api.get('/samples', { params })
export const fetchSampleById = (id) => api.get(`/samples/${id}`)

export const pivotSampleToAttr = (ids) => api.post('/pivot/sample-to-attr', { ids })
export const pivotAttrToSample = (nodeIds) => api.post('/pivot/attr-to-sample', { nodeIds })
