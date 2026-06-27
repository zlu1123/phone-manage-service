import request from '@/utils/request'

// 查询订单列表
export function queryOrderList(query) {
  return request({
    url: '/system/order/queryOrderList',
    method: 'get',
    params: query
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
