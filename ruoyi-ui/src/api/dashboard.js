/**
 * 首页仪表盘 Mock 数据接口
 * 后续可替换为真实接口
 */

// 模拟异步请求
const mockRequest = (data, delay = 300) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, data })
    }, delay)
  })
}

// 获取统计卡片数据
export function getStatistics() {
  return mockRequest({
    totalOrders: 1286,
    todayOrders: 38,
    activatedDevices: 952,
    pendingOrders: 67
  })
}

// 获取订单趋势数据（近7天）
export function getOrderTrend() {
  return mockRequest({
    dates: ['04-06', '04-07', '04-08', '04-09', '04-10', '04-11', '04-12'],
    newOrders: [32, 45, 28, 56, 41, 38, 52],
    completedOrders: [28, 38, 25, 48, 36, 35, 45]
  })
}

// 获取设备型号分布数据
export function getDeviceModelDistribution() {
  return mockRequest([
    { value: 320, name: 'iPhone 15' },
    { value: 240, name: 'iPhone 14' },
    { value: 180, name: 'iPhone 13' },
    { value: 120, name: 'iPhone 12' },
    { value: 80, name: 'iPhone SE' },
    { value: 60, name: '其他' }
  ])
}

// 获取月度订单统计数据（近6个月）
export function getMonthlyOrderStats() {
  return mockRequest({
    months: ['2025-11', '2025-12', '2026-01', '2026-02', '2026-03', '2026-04'],
    orderCounts: [186, 225, 198, 210, 245, 222],
    activatedCounts: [142, 189, 165, 178, 203, 175]
  })
}

// 获取保修状态统计
export function getWarrantyStatus() {
  return mockRequest([
    { value: 580, name: '保修期内' },
    { value: 372, name: '即将过保（30天内）' },
    { value: 334, name: '已过保' }
  ])
}

// 获取最近订单列表
export function getRecentOrders() {
  return mockRequest([
    { id: 1, sn: 'F17FV5GA0DYP', model: 'iPhone 15 Pro', activated: true, createTime: '2026-04-12 14:30:22', createBy: 'admin' },
    { id: 2, sn: 'G28HW6HB1EZQ', model: 'iPhone 14', activated: true, createTime: '2026-04-12 13:15:08', createBy: 'admin' },
    { id: 3, sn: 'H39IX7IC2FAR', model: 'iPhone 13 Mini', activated: false, createTime: '2026-04-12 11:42:35', createBy: 'operator1' },
    { id: 4, sn: 'I40JY8JD3GBS', model: 'iPhone 15', activated: true, createTime: '2026-04-12 10:28:17', createBy: 'admin' },
    { id: 5, sn: 'J51KZ9KE4HCT', model: 'iPhone 12', activated: false, createTime: '2026-04-12 09:55:43', createBy: 'operator2' },
    { id: 6, sn: 'K62LA0LF5IDU', model: 'iPhone SE', activated: true, createTime: '2026-04-11 17:20:11', createBy: 'admin' },
    { id: 7, sn: 'L73MB1MG6JEV', model: 'iPhone 14 Pro Max', activated: true, createTime: '2026-04-11 16:05:39', createBy: 'operator1' },
    { id: 8, sn: 'M84NC2NH7KFW', model: 'iPhone 15 Pro', activated: false, createTime: '2026-04-11 14:48:22', createBy: 'admin' }
  ])
}
