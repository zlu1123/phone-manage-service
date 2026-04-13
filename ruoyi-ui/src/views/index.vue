<template>
  <div class="app-container dashboard">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">订单总数</div>
              <div class="stat-value">{{ statistics.totalOrders }}</div>
              <div class="stat-desc">
                <span class="stat-trend up"><i class="el-icon-top"></i> 12.5%</span>
                较上月
              </div>
            </div>
            <div class="stat-icon" style="background: linear-gradient(135deg, #409EFF, #66b1ff)">
              <i class="el-icon-s-order"></i>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">今日新增</div>
              <div class="stat-value">{{ statistics.todayOrders }}</div>
              <div class="stat-desc">
                <span class="stat-trend up"><i class="el-icon-top"></i> 8.2%</span>
                较昨日
              </div>
            </div>
            <div class="stat-icon" style="background: linear-gradient(135deg, #67C23A, #85ce61)">
              <i class="el-icon-document-add"></i>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">已激活设备</div>
              <div class="stat-value">{{ statistics.activatedDevices }}</div>
              <div class="stat-desc">
                <span class="stat-trend up"><i class="el-icon-top"></i> 5.7%</span>
                激活率 74%
              </div>
            </div>
            <div class="stat-icon" style="background: linear-gradient(135deg, #E6A23C, #ebb563)">
              <i class="el-icon-mobile-phone"></i>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">待处理订单</div>
              <div class="stat-value">{{ statistics.pendingOrders }}</div>
              <div class="stat-desc">
                <span class="stat-trend down"><i class="el-icon-bottom"></i> 3.1%</span>
                较昨日
              </div>
            </div>
            <div class="stat-icon" style="background: linear-gradient(135deg, #F56C6C, #f78989)">
              <i class="el-icon-time"></i>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 - 第一行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span class="chart-title">订单趋势（近7天）</span>
            <el-button-group>
              <el-button size="mini" :type="trendType === 'week' ? 'primary' : ''" @click="trendType = 'week'">近7天</el-button>
              <el-button size="mini" :type="trendType === 'month' ? 'primary' : ''" @click="trendType = 'month'">近30天</el-button>
            </el-button-group>
          </div>
          <div ref="orderTrendChart" class="chart-container" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span class="chart-title">设备型号分布</span>
          </div>
          <div ref="deviceModelChart" class="chart-container" style="height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 - 第二行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span class="chart-title">保修状态统计</span>
          </div>
          <div ref="warrantyChart" class="chart-container" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span class="chart-title">月度订单统计（近6个月）</span>
          </div>
          <div ref="monthlyChart" class="chart-container" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近订单列表 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span class="chart-title">最近订单</span>
            <el-button size="mini" type="text" icon="el-icon-d-arrow-right" @click="$router.push('/order/list')">查看更多</el-button>
          </div>
          <el-table :data="recentOrders" style="width: 100%" size="medium" :header-cell-style="{ background: '#fafafa' }">
            <el-table-column prop="sn" label="序列号" min-width="140" />
            <el-table-column prop="model" label="设备型号" min-width="140" />
            <el-table-column prop="activated" label="激活状态" min-width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.activated ? 'success' : 'info'" size="small">
                  {{ scope.row.activated ? '已激活' : '未激活' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createBy" label="创建人" min-width="100" align="center" />
            <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import { debounce } from '@/utils'
import {
  getStatistics,
  getOrderTrend,
  getDeviceModelDistribution,
  getMonthlyOrderStats,
  getWarrantyStatus,
  getRecentOrders
} from '@/api/dashboard'

export default {
  name: "Index",
  data() {
    return {
      // 统计数据
      statistics: {
        totalOrders: 0,
        todayOrders: 0,
        activatedDevices: 0,
        pendingOrders: 0
      },
      // 趋势类型
      trendType: 'week',
      // 最近订单
      recentOrders: [],
      // 图表实例
      orderTrendChart: null,
      deviceModelChart: null,
      warrantyChart: null,
      monthlyChart: null,
      // resize handler
      $_resizeHandler: null,
      $_sidebarElm: null
    }
  },
  mounted() {
    this.initData()
    this.initListener()
  },
  activated() {
    // keep-alive 激活时重新 resize
    if (!this.$_resizeHandler) {
      this.initListener()
    }
    this.resizeAllCharts()
  },
  beforeDestroy() {
    this.destroyListener()
    this.disposeAllCharts()
  },
  deactivated() {
    this.destroyListener()
  },
  methods: {
    /** 初始化所有数据 */
    async initData() {
      await Promise.all([
        this.fetchStatistics(),
        this.fetchRecentOrders()
      ])
      // DOM 渲染后初始化图表
      this.$nextTick(() => {
        this.initOrderTrendChart()
        this.initDeviceModelChart()
        this.initWarrantyChart()
        this.initMonthlyChart()
      })
    },

    /** 获取统计卡片数据 */
    async fetchStatistics() {
      try {
        const res = await getStatistics()
        this.statistics = res.data
      } catch (e) {
        console.error('获取统计数据失败：', e)
      }
    },

    /** 获取最近订单 */
    async fetchRecentOrders() {
      try {
        const res = await getRecentOrders()
        this.recentOrders = res.data
      } catch (e) {
        console.error('获取最近订单失败：', e)
      }
    },

    /** 初始化订单趋势折线图 */
    async initOrderTrendChart() {
      try {
        const res = await getOrderTrend()
        const { dates, newOrders, completedOrders } = res.data
        this.orderTrendChart = echarts.init(this.$refs.orderTrendChart, 'macarons')
        this.orderTrendChart.setOption({
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            padding: [5, 10]
          },
          legend: {
            data: ['新增订单', '完成订单'],
            right: 10
          },
          grid: {
            left: 10, right: 20, bottom: 20, top: 40,
            containLabel: true
          },
          xAxis: {
            data: dates,
            boundaryGap: false,
            axisTick: { show: false }
          },
          yAxis: {
            axisTick: { show: false }
          },
          series: [
            {
              name: '新增订单',
              type: 'line',
              smooth: true,
              symbol: 'circle',
              symbolSize: 8,
              itemStyle: { color: '#409EFF' },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(64,158,255,0.3)' },
                  { offset: 1, color: 'rgba(64,158,255,0.05)' }
                ])
              },
              data: newOrders,
              animationDuration: 2000,
              animationEasing: 'cubicInOut'
            },
            {
              name: '完成订单',
              type: 'line',
              smooth: true,
              symbol: 'circle',
              symbolSize: 8,
              itemStyle: { color: '#67C23A' },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(103,194,58,0.3)' },
                  { offset: 1, color: 'rgba(103,194,58,0.05)' }
                ])
              },
              data: completedOrders,
              animationDuration: 2000,
              animationEasing: 'cubicInOut'
            }
          ]
        })
      } catch (e) {
        console.error('初始化订单趋势图失败：', e)
      }
    },

    /** 初始化设备型号饼图 */
    async initDeviceModelChart() {
      try {
        const res = await getDeviceModelDistribution()
        this.deviceModelChart = echarts.init(this.$refs.deviceModelChart, 'macarons')
        this.deviceModelChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b} : {c} ({d}%)'
          },
          legend: {
            orient: 'horizontal',
            bottom: 10,
            data: res.data.map(item => item.name)
          },
          series: [
            {
              name: '设备型号',
              type: 'pie',
              radius: ['35%', '60%'],
              center: ['50%', '42%'],
              avoidLabelOverlap: true,
              itemStyle: {
                borderRadius: 6,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: true,
                formatter: '{b}\n{d}%'
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 14,
                  fontWeight: 'bold'
                }
              },
              data: res.data,
              animationEasing: 'cubicInOut',
              animationDuration: 2000
            }
          ]
        })
      } catch (e) {
        console.error('初始化设备型号图失败：', e)
      }
    },

    /** 初始化保修状态饼图 */
    async initWarrantyChart() {
      try {
        const res = await getWarrantyStatus()
        this.warrantyChart = echarts.init(this.$refs.warrantyChart, 'macarons')
        this.warrantyChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b} : {c} ({d}%)'
          },
          legend: {
            orient: 'horizontal',
            bottom: 10,
            data: res.data.map(item => item.name)
          },
          color: ['#67C23A', '#E6A23C', '#F56C6C'],
          series: [
            {
              name: '保修状态',
              type: 'pie',
              roseType: 'radius',
              radius: [20, 100],
              center: ['50%', '42%'],
              data: res.data,
              itemStyle: {
                borderRadius: 5,
                borderColor: '#fff',
                borderWidth: 2
              },
              animationEasing: 'cubicInOut',
              animationDuration: 2000
            }
          ]
        })
      } catch (e) {
        console.error('初始化保修状态图失败：', e)
      }
    },

    /** 初始化月度订单柱状图 */
    async initMonthlyChart() {
      try {
        const res = await getMonthlyOrderStats()
        const { months, orderCounts, activatedCounts } = res.data
        this.monthlyChart = echarts.init(this.$refs.monthlyChart, 'macarons')
        this.monthlyChart.setOption({
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' }
          },
          legend: {
            data: ['订单数量', '激活数量'],
            right: 10
          },
          grid: {
            left: 10, right: 20, bottom: 20, top: 40,
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: months,
            axisTick: { alignWithLabel: true }
          },
          yAxis: {
            type: 'value',
            axisTick: { show: false }
          },
          series: [
            {
              name: '订单数量',
              type: 'bar',
              barWidth: '30%',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#409EFF' },
                  { offset: 1, color: '#66b1ff' }
                ]),
                borderRadius: [4, 4, 0, 0]
              },
              data: orderCounts,
              animationDuration: 2000
            },
            {
              name: '激活数量',
              type: 'bar',
              barWidth: '30%',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#67C23A' },
                  { offset: 1, color: '#85ce61' }
                ]),
                borderRadius: [4, 4, 0, 0]
              },
              data: activatedCounts,
              animationDuration: 2000
            }
          ]
        })
      } catch (e) {
        console.error('初始化月度订单图失败：', e)
      }
    },

    /** 监听窗口 resize */
    initListener() {
      this.$_resizeHandler = debounce(() => {
        this.resizeAllCharts()
      }, 100)
      window.addEventListener('resize', this.$_resizeHandler)
      this.$_sidebarElm = document.getElementsByClassName('sidebar-container')[0]
      if (this.$_sidebarElm) {
        this.$_sidebarElm.addEventListener('transitionend', this.handleSidebarResize)
      }
    },

    /** 销毁监听 */
    destroyListener() {
      window.removeEventListener('resize', this.$_resizeHandler)
      this.$_resizeHandler = null
      if (this.$_sidebarElm) {
        this.$_sidebarElm.removeEventListener('transitionend', this.handleSidebarResize)
      }
    },

    /** 侧边栏 resize */
    handleSidebarResize(e) {
      if (e.propertyName === 'width') {
        this.resizeAllCharts()
      }
    },

    /** 所有图表 resize */
    resizeAllCharts() {
      this.orderTrendChart && this.orderTrendChart.resize()
      this.deviceModelChart && this.deviceModelChart.resize()
      this.warrantyChart && this.warrantyChart.resize()
      this.monthlyChart && this.monthlyChart.resize()
    },

    /** 销毁所有图表 */
    disposeAllCharts() {
      const charts = [this.orderTrendChart, this.deviceModelChart, this.warrantyChart, this.monthlyChart]
      charts.forEach(chart => {
        if (chart) {
          chart.dispose()
        }
      })
      this.orderTrendChart = null
      this.deviceModelChart = null
      this.warrantyChart = null
      this.monthlyChart = null
    }
  }
}
</script>

<style scoped lang="scss">
.dashboard {
  padding: 12px;

  /* 统计卡片 */
  .stat-cards {
    margin-bottom: 16px;
  }

  .stat-card {
    margin-bottom: 12px;

    ::v-deep .el-card__body {
      padding: 20px;
    }
  }

  .stat-card-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .stat-info {
    flex: 1;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
    font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
  }

  .stat-desc {
    font-size: 12px;
    color: #909399;
  }

  .stat-trend {
    margin-right: 4px;
    font-weight: 500;

    &.up {
      color: #67C23A;
    }

    &.down {
      color: #F56C6C;
    }
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    i {
      font-size: 28px;
      color: #fff;
    }
  }

  /* 图表区域 */
  .chart-row {
    margin-bottom: 16px;
  }

  .chart-card {
    margin-bottom: 12px;

    ::v-deep .el-card__header {
      padding: 14px 20px;
      border-bottom: 1px solid #f0f0f0;
    }

    ::v-deep .el-card__body {
      padding: 16px;
    }
  }

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .chart-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .chart-container {
    width: 100%;
  }
}
</style>
