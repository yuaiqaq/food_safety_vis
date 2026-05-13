export const VALUE_TRANSLATION = {
  'Pre-packaged': '预包装',
  'Bulk weighing': '散装称重',
  'Nationally Mandated': '国家监督抽检',
  'Provincially Mandated': '省级监督抽检',
  manufacturer: '生产企业',
  eatery: '餐饮单位',
  restaurant: '餐馆',
  'chain supermarket': '连锁超市',
  'online store': '网店',
  'other supermarket/convenience store': '其他商超/便利店',
  'wet market': '农贸市场',
  'wet market/wholesale market': '农贸/批发市场',
  'wholesale market': '批发市场',
  'wholesale/retail': '批零经营',
  cafeteria: '食堂',
}

export function translateToChinese(value) {
  if (value === null || value === undefined || value === '') return '-'
  return VALUE_TRANSLATION[value] || value
}
