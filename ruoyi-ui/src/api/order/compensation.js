import request from '@/utils/request'

// 查询赔付订单列表
export function getCompensationList(query) {
  return request({
    url: '/system/CompensationOrder/getList',
    method: 'get',
    params: query
  })
}

// 新增赔付订单
export function addCompensation(data) {
  return request({
    url: '/system/CompensationOrder/insert',
    method: 'post',
    data: data
  })
}

// 审核并赔付（修改）
export function updateCompensation(data) {
  return request({
    url: '/system/CompensationOrder/update',
    method: 'post',
    data: data
  })
}

// 删除赔付订单
export function deleteCompensation(id) {
  return request({
    url: '/system/CompensationOrder/delete/' + id,
    method: 'delete'
  })
}
