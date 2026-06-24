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

// 根据协议ID获取协议内容（按需加载富文本）
export function getContractById(id) {
  return request({
    url: '/system/contract/getById',
    method: 'get',
    params: { id }
  })
}
