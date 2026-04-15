/**
 * 首页仪表盘 Mock 数据接口
 * 后续可替换为真实接口
 */

import request from '@/utils/request'

// 获取统计卡片数据
export function getStatistics() {
  return request({
    url: '/system/order/dashboard/statistics',
    method: 'get'
  })
}

// 获取订单趋势数据
export function getOrderTrend(period = 'week') {
  return request({
    url: '/system/order/dashboard/trend',
    method: 'get',
    params: { period }
  })
}

// 获取设备型号分布数据
export function getDeviceModelDistribution() {
  return request({
    url: '/system/order/dashboard/deviceModelDistribution',
    method: 'get'
  })
}

// 获取月度订单统计数据（近6个月）
export function getMonthlyOrderStats() {
  return request({
    url: '/system/order/dashboard/monthlyOrderStats',
    method: 'get'
  })
}

// 获取保修状态统计
export function getWarrantyStatus() {
  return request({
    url: '/system/order/dashboard/warrantyStatus',
    method: 'get'
  })
}

// 获取最近订单列表
export function getRecentOrders() {
  return request({
    url: '/system/order/dashboard/recentOrders',
    method: 'get'
  })
}

// 获取个人统计卡片数据
export function getUserStatistics() {
  return request({
    url: '/system/order/dashboard/userStatistics',
    method: 'get'
  })
}

// 获取个人订单趋势数据（近7天）
export function getUserOrderTrend() {
  return request({
    url: '/system/order/dashboard/userTrend',
    method: 'get'
  })
}

// 获取个人最近订单列表
export function getUserRecentOrders() {
  return request({
    url: '/system/order/dashboard/userRecentOrders',
    method: 'get'
  })
}

// 查询06API余额（真实接口）
export function getApiBalance() {
  return request({
    url: '/06/api/balance',
    method: 'get'
  })
}
