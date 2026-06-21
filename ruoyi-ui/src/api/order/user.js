import request from '@/utils/request'

// 查询留资用户列表
export function getLeaveInfoList(query) {
  return request({
    url: '/system/leaveInfo/getList',
    method: 'get',
    params: query
  })
}

// 新增留资用户
export function addLeaveInfo(data) {
  return request({
    url: '/system/leaveInfo/insert',
    method: 'post',
    data: data
  })
}

// 修改留资用户
export function updateLeaveInfo(data) {
  return request({
    url: '/system/leaveInfo/update',
    method: 'post',
    data: data
  })
}

// 删除留资用户
export function deleteLeaveInfo(id) {
  return request({
    url: '/system/leaveInfo/delete/' + id,
    method: 'delete'
  })
}
