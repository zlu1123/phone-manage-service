import request from '@/utils/request'

// 查询订单列表
export function queryOrderList(query) {
  return request({
    url: '/system/order/queryOrderList',
    method: 'get',
    params: query
  })
}

// 导出订单数据（全字段，不分页）
export function exportOrderList(query) {
  return request({
    url: '/system/order/exportOrderList',
    method: 'get',
    params: query
  })
}

// 查询订单详情（全字段，包含详情表和签约表）
export function getOrderDetail(id) {
  return request({
    url: '/system/order/getDetail',
    method: 'get',
    params: { id }
  })
}

// 查询手机品牌列表
export function queryPhoneTypeList() {
  return request({
    url: '/06/api/queryPhoneTypeList',
    method: 'get'
  })
}

// 根据订单ID获取签约时的协议内容（历史快照，不受协议模板表变更影响）
export function getOrderContractContent(id) {
  return request({
    url: '/system/order/getContractContent',
    method: 'get',
    params: { id }
  })
}

// 标记订单为测试数据（逻辑删除，列表不再展示），支持单条/批量，仅管理员
export function markTestData(ids) {
  return request({
    url: '/system/order/markTestData',
    method: 'put',
    params: { ids: ids.join(',') }
  })
}
