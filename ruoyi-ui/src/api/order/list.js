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
