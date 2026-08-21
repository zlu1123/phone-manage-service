/**
 * 首页仪表盘数据接口
 */

import request from '@/utils/request'

// 获取统计卡片数据（可选门店维度，storeId 为空表示全部门店）
export function getStatistics(storeId) {
  return request({
    url: '/system/order/dashboard/statistics',
    method: 'get',
    params: storeId ? { storeId } : {}
  })
}

// 获取订单趋势数据（可选门店维度）
export function getOrderTrend(period = 'week', storeId) {
  return request({
    url: '/system/order/dashboard/trend',
    method: 'get',
    params: { period, ...(storeId ? { storeId } : {}) }
  })
}

// 获取月度订单统计数据（近6个月，可选门店维度）
export function getMonthlyOrderStats(storeId) {
  return request({
    url: '/system/order/dashboard/monthlyOrderStats',
    method: 'get',
    params: storeId ? { storeId } : {}
  })
}

// 获取最近订单列表（可选门店维度）
export function getRecentOrders(storeId) {
  return request({
    url: '/system/order/dashboard/recentOrders',
    method: 'get',
    params: storeId ? { storeId } : {}
  })
}

// 获取门店列表（门店筛选下拉数据源）
export function getStoreList() {
  return request({
    url: '/system/order/dashboard/storeList',
    method: 'get'
  })
}

// 获取各门店订单对比统计（门店图表与总览表共用）
export function getStoreComparison() {
  return request({
    url: '/system/order/dashboard/storeComparison',
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
