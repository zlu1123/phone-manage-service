import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 查询协议列表
export function getContractList(query) {
  return request({
    url: '/system/contract/getList',
    method: 'get',
    params: query
  })
}

// 新增协议（form-data提交）
export function addContract(data) {
  return request({
    url: '/system/contract/insert',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 修改协议（form-data提交）
export function updateContract(data) {
  return request({
    url: '/system/contract/update',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除协议
export function deleteContract(id) {
  return request({
    url: '/system/contract/delete/' + id,
    method: 'delete'
  })
}

// 更新协议状态
export function updateContractStatus(data) {
  return request({
    url: '/system/contract/updateStatus',
    method: 'post',
    data: data
  })
}

// 获取协议详情
export function getContractDetail(id) {
  return request({
    url: '/system/contract/getDetail/' + id,
    method: 'get'
  })
}
