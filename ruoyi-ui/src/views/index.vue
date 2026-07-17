<template>
  <div class="app-container dashboard">
    <!-- 06API 余额卡片（仅管理员可见） -->
    <el-row v-if="isAdmin" :gutter="16" class="balance-row">
      <el-col :span="24">
        <el-card class="balance-card" shadow="hover">
          <div class="balance-body">
            <div class="balance-left">
              <div class="balance-icon-wrapper">
                <el-icon :size="24" color="#fff"><Wallet /></el-icon>
              </div>
              <div class="balance-info">
                <div class="balance-label">06API 账户余额</div>
                <div class="balance-value">
                  <span class="balance-symbol">¥</span>
                  <span class="balance-amount">{{ apiBalance }}</span>
                </div>
              </div>
            </div>
            <div class="balance-right">
              <el-tag :type="balanceTagType" size="medium" effect="plain">
                <el-icon :size="14"><component :is="balanceTagIcon" /></el-icon>
                {{ balanceStatusText }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 管理员视图 ==================== -->
    <template v-if="isAdmin">
      <!-- 顶部统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-card-body">
              <div class="stat-info">
                <div class="stat-label">订单总数</div>
                <div class="stat-value">{{ statistics.totalOrders }}</div>
                <div class="stat-desc">
                  <span :class="['stat-trend', statistics.totalOrdersTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="statistics.totalOrdersTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ statistics.totalOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
                <el-icon :size="28" color="#fff"><Tickets /></el-icon>
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
                  <span :class="['stat-trend', statistics.todayOrdersTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="statistics.todayOrdersTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ statistics.todayOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a, #85ce61)">
                <el-icon :size="28" color="#fff"><DocumentAdd /></el-icon>
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
                  <span :class="['stat-trend', statistics.activationRateTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="statistics.activationRateTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ statistics.activationRateLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c, #ebb563)">
                <el-icon :size="28" color="#fff"><Cellphone /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-card-body">
              <div class="stat-info">
                <div class="stat-label">已签约</div>
                <div class="stat-value">{{ statistics.signedOrders }}</div>
                <div class="stat-desc">
                  <span :class="['stat-trend', statistics.signedRateTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="statistics.signedRateTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ statistics.signedRateLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #f56c6c, #f78989)">
                <el-icon :size="28" color="#fff"><EditPen /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 - 第一行 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :sm="24" :md="16" :lg="16">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">
                  {{ trendType === 'week' ? '订单趋势（近7天）' : '订单趋势（近30天）' }}
                </span>
                <el-button-group>
                  <el-button size="mini" :type="trendType === 'week' ? 'primary' : ''" @click="trendType = 'week'"
                    >近7天</el-button
                  >
                  <el-button size="mini" :type="trendType === 'month' ? 'primary' : ''" @click="trendType = 'month'"
                    >近30天</el-button
                  >
                </el-button-group>
              </div>
            </template>
            <div ref="orderTrendChartRef" class="chart-container" style="height: 320px"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8" :lg="8">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">设备型号分布</span>
              </div>
            </template>
            <div ref="deviceModelChartRef" class="chart-container" style="height: 320px"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 - 第二行 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :sm="24" :md="8" :lg="8">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">保修状态统计</span>
              </div>
            </template>
            <div ref="warrantyChartRef" class="chart-container" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="16" :lg="16">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">月度订单统计（近6个月）</span>
              </div>
            </template>
            <div ref="monthlyChartRef" class="chart-container" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 最近订单列表 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">最近订单</span>
                <el-button size="mini" type="text" :icon="DArrowRight" @click="router.push('/order/list')"
                  >查看更多</el-button
                >
              </div>
            </template>
            <el-table
              :data="recentOrders"
              style="width: 100%"
              size="medium"
              :header-cell-style="{ background: '#fafafa' }"
            >
              <el-table-column prop="sn" label="序列号" min-width="140" />
              <el-table-column prop="model" label="设备型号" min-width="140" />
              <el-table-column prop="activated" label="激活状态" min-width="100" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.activated ? 'success' : 'info'" size="small">
                    {{ scope.row.activated ? '已激活' : '未激活' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createBy" label="创建人" min-width="100" align="center" />
              <el-table-column label="业务日期" min-width="120" align="center">
                <template #default="scope">
                  {{ formatBusinessDate(scope.row) }}
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- ==================== 普通用户视图 ==================== -->
    <template v-else>
      <!-- 欢迎区域 -->
      <el-card class="welcome-card" shadow="hover">
        <div class="welcome-body">
          <div class="welcome-info">
            <h2 class="welcome-title">{{ greetingText }}，{{ nickName }} 👋</h2>
            <p class="welcome-desc">欢迎回到 IMEI 保修查询管理系统，以下是您的工作概览</p>
          </div>
          <div class="welcome-avatar">
            <img :src="avatar" class="avatar-img" />
          </div>
        </div>
      </el-card>

      <!-- 个人统计卡片 -->
      <el-row :gutter="16" class="stat-cards" style="margin-top: 16px">
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-card-body">
              <div class="stat-info">
                <div class="stat-label">我的订单</div>
                <div class="stat-value">{{ userStatistics.myTotalOrders }}</div>
                <div class="stat-desc">
                  <span :class="['stat-trend', userStatistics.myTotalOrdersTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="userStatistics.myTotalOrdersTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ userStatistics.myTotalOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
                <el-icon :size="28" color="#fff"><Tickets /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-card-body">
              <div class="stat-info">
                <div class="stat-label">今日提交</div>
                <div class="stat-value">{{ userStatistics.myTodayOrders }}</div>
                <div class="stat-desc">
                  <span :class="['stat-trend', userStatistics.myTodayOrdersTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="userStatistics.myTodayOrdersTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ userStatistics.myTodayOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a, #85ce61)">
                <el-icon :size="28" color="#fff"><DocumentAdd /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-card-body">
              <div class="stat-info">
                <div class="stat-label">已激活</div>
                <div class="stat-value">
                  {{ userStatistics.myActivatedDevices }}
                </div>
                <div class="stat-desc">
                  <span :class="['stat-trend', userStatistics.myActivationRateTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="userStatistics.myActivationRateTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ userStatistics.myActivationRateLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c, #ebb563)">
                <el-icon :size="28" color="#fff"><Cellphone /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-card-body">
              <div class="stat-info">
                <div class="stat-label">我的签约</div>
                <div class="stat-value">
                  {{ userStatistics.mySignedOrders }}
                </div>
                <div class="stat-desc">
                  <span :class="['stat-trend', userStatistics.mySignedRateTrend]"
                    ><el-icon style="vertical-align: middle; font-size: 12px"
                      ><component :is="userStatistics.mySignedRateTrend === 'down' ? 'ArrowDown' : 'ArrowUp'"
                    /></el-icon>
                    {{ userStatistics.mySignedRateLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #f56c6c, #f78989)">
                <el-icon :size="28" color="#fff"><EditPen /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 个人图表 + 快捷操作 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :sm="24" :md="16" :lg="16">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">我的订单趋势（近7天）</span>
              </div>
            </template>
            <div ref="userOrderTrendChartRef" class="chart-container" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8" :lg="8">
          <el-card class="chart-card shortcut-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">快捷操作</span>
              </div>
            </template>
            <div class="shortcut-grid">
              <div class="shortcut-item" @click="router.push('/order/list')">
                <div class="shortcut-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
                  <el-icon :size="22" color="#fff"><Tickets /></el-icon>
                </div>
                <span class="shortcut-text">订单列表</span>
              </div>
              <div class="shortcut-item" @click="router.push('/user/profile')">
                <div class="shortcut-icon" style="background: linear-gradient(135deg, #67c23a, #85ce61)">
                  <el-icon :size="22" color="#fff"><User /></el-icon>
                </div>
                <span class="shortcut-text">个人中心</span>
              </div>
              <div class="shortcut-item" @click="handleQuickQuery">
                <div class="shortcut-icon" style="background: linear-gradient(135deg, #e6a23c, #ebb563)">
                  <el-icon :size="22" color="#fff"><Search /></el-icon>
                </div>
                <span class="shortcut-text">快速查询</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 个人最近订单 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">我的最近订单</span>
                <el-button size="mini" type="text" :icon="DArrowRight" @click="router.push('/order/list')"
                  >查看更多</el-button
                >
              </div>
            </template>
            <el-table
              :data="userRecentOrders"
              style="width: 100%"
              size="medium"
              :header-cell-style="{ background: '#fafafa' }"
            >
              <el-table-column prop="sn" label="序列号" min-width="140" />
              <el-table-column prop="model" label="设备型号" min-width="140" />
              <el-table-column prop="activated" label="激活状态" min-width="100" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.activated ? 'success' : 'info'" size="small">
                    {{ scope.row.activated ? '已激活' : '未激活' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="业务日期" min-width="120" align="center">
                <template #default="scope">
                  {{ formatBusinessDate(scope.row) }}
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup name="Index">
import { ref, computed, watch, onMounted, onActivated, onBeforeUnmount, onDeactivated, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import 'echarts/theme/macarons';
import { ElMessageBox } from 'element-plus';
import { debounce } from '@/utils';
import {
  Wallet,
  Tickets,
  DocumentAdd,
  Cellphone,
  EditPen,
  ArrowDown,
  ArrowUp,
  DArrowRight,
  User,
  Search,
} from '@element-plus/icons-vue';
import useUserStore from '@/store/modules/user';
import {
  getStatistics,
  getOrderTrend,
  getDeviceModelDistribution,
  getMonthlyOrderStats,
  getWarrantyStatus,
  getRecentOrders,
  getUserStatistics,
  getUserOrderTrend,
  getUserRecentOrders,
  getApiBalance,
} from '@/api/dashboard';

const router = useRouter();
const userStore = useUserStore();

// ==================== 响应式数据 ====================

// 公共数据
const apiBalance = ref('--');

// 管理员数据
const statistics = ref({
  totalOrders: 0,
  todayOrders: 0,
  activatedDevices: 0,
  pendingOrders: 0,
  signedOrders: 0,
  totalOrdersGrowthLabel: '0.0% 较上月',
  todayOrdersGrowthLabel: '0.0% 较昨日',
  activationRateLabel: '激活率 0.0%',
  pendingRateLabel: '待处理占比 0.0%',
  signedRateLabel: '签约率 0.0%',
  totalOrdersTrend: 'up',
  todayOrdersTrend: 'up',
  activationRateTrend: 'up',
  pendingRateTrend: 'down',
  signedRateTrend: 'up',
});

const trendType = ref('week');
const recentOrders = ref([]);

// 管理员图表实例
const orderTrendChart = ref(null);
const deviceModelChart = ref(null);
const warrantyChart = ref(null);
const monthlyChart = ref(null);

// 管理员图表 ref 引用
const orderTrendChartRef = ref(null);
const deviceModelChartRef = ref(null);
const warrantyChartRef = ref(null);
const monthlyChartRef = ref(null);

// 普通用户数据
const userStatistics = ref({
  myTotalOrders: 0,
  myTodayOrders: 0,
  myActivatedDevices: 0,
  myPendingOrders: 0,
  mySignedOrders: 0,
  myTotalOrdersGrowthLabel: '0.0% 近7天较前7天',
  myTodayOrdersGrowthLabel: '0.0% 较昨日',
  myActivationRateLabel: '激活率 0.0%',
  myPendingRateLabel: '待处理占比 0.0%',
  mySignedRateLabel: '签约率 0.0%',
  myTotalOrdersTrend: 'up',
  myTodayOrdersTrend: 'up',
  myActivationRateTrend: 'up',
  myPendingRateTrend: 'down',
  mySignedRateTrend: 'up',
});

const userRecentOrders = ref([]);
const userOrderTrendChart = ref(null);
const userOrderTrendChartRef = ref(null);

// resize handler
let $_resizeHandler = null;
let $_sidebarElm = null;

// ==================== 计算属性 ====================

/** 判断是否为管理员 */
const isAdmin = computed(() => {
  const roles = userStore.roles || [];
  const adminRoles = ['admin', 'user'];
  return adminRoles.some((role) => roles.includes(role));
});

/** 余额状态标签类型 */
const balanceTagType = computed(() => {
  const val = parseFloat(apiBalance.value);
  if (isNaN(val)) return 'info';
  if (val <= 1) return 'danger';
  if (val <= 5) return 'warning';
  return 'success';
});

/** 余额状态文字 */
const balanceStatusText = computed(() => {
  const val = parseFloat(apiBalance.value);
  if (isNaN(val)) return '查询中...';
  if (val <= 1) return '余额不足';
  if (val <= 5) return '余额偏低';
  return '余额充足';
});

/** 余额状态图标 */
const balanceTagIcon = computed(() => {
  const val = parseFloat(apiBalance.value);
  if (isNaN(val)) return 'Loading';
  if (val <= 1) return 'WarningFilled';
  if (val <= 5) return 'InfoFilled';
  return 'CircleCheckFilled';
});

/** 获取员工姓名 */
const nickName = computed(() => {
  return userStore.nickName || userStore.name || '用户';
});

/** 获取用户头像 */
const avatar = computed(() => {
  return userStore.avatar;
});

/** 根据时间段生成问候语 */
const greetingText = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 9) return '早上好';
  if (hour < 12) return '上午好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

// ==================== Watch ====================

watch(trendType, () => {
  if (isAdmin.value && orderTrendChartRef.value) {
    initOrderTrendChart();
  }
});

// ==================== 生命周期 ====================

onMounted(() => {
  initData();
  initListener();
});

onActivated(() => {
  if (!$_resizeHandler) {
    initListener();
  }
  resizeAllCharts();
});

onBeforeUnmount(() => {
  destroyListener();
  disposeAllCharts();
});

onDeactivated(() => {
  destroyListener();
});

// ==================== 初始化方法 ====================

/** 初始化所有数据（根据角色区分） */
async function initData() {
  if (isAdmin.value) {
    fetchApiBalance();
    await initAdminData();
  } else {
    await initUserData();
  }
}

// ==================== 管理员相关方法 ====================

/** 初始化管理员数据 */
async function initAdminData() {
  await Promise.all([fetchStatistics(), fetchRecentOrders()]);
  nextTick(() => {
    initOrderTrendChart();
    initDeviceModelChart();
    initWarrantyChart();
    initMonthlyChart();
  });
}

/** 获取统计卡片数据 */
async function fetchStatistics() {
  try {
    const res = await getStatistics();
    statistics.value = res.data;
  } catch (e) {
    console.error('获取统计数据失败：', e);
  }
}

/** 获取最近订单 */
async function fetchRecentOrders() {
  try {
    const res = await getRecentOrders();
    recentOrders.value = res.data;
  } catch (e) {
    console.error('获取最近订单失败：', e);
  }
}

/** 初始化订单趋势折线图 */
async function initOrderTrendChart() {
  try {
    const res = await getOrderTrend(trendType.value);
    const { dates, newOrders, completedOrders, signedOrders } = res.data;
    if (!orderTrendChart.value) {
      orderTrendChart.value = echarts.init(orderTrendChartRef.value, 'macarons');
    }
    orderTrendChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        padding: [5, 10],
      },
      legend: {
        data: ['新增订单', '完成订单', '签约订单'],
        right: 10,
      },
      grid: {
        left: 10,
        right: 20,
        bottom: 20,
        top: 40,
        containLabel: true,
      },
      xAxis: {
        data: dates,
        boundaryGap: false,
        axisTick: { show: false },
      },
      yAxis: {
        axisTick: { show: false },
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
              { offset: 1, color: 'rgba(64,158,255,0.05)' },
            ]),
          },
          data: newOrders,
          animationDuration: 2000,
          animationEasing: 'cubicInOut',
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
              { offset: 1, color: 'rgba(103,194,58,0.05)' },
            ]),
          },
          data: completedOrders,
          animationDuration: 2000,
          animationEasing: 'cubicInOut',
        },
        {
          name: '签约订单',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: { color: '#F56C6C' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(245,108,108,0.3)' },
              { offset: 1, color: 'rgba(245,108,108,0.05)' },
            ]),
          },
          data: signedOrders,
          animationDuration: 2000,
          animationEasing: 'cubicInOut',
        },
      ],
    });
  } catch (e) {
    console.error('初始化订单趋势图失败：', e);
  }
}

/** 初始化设备型号饼图 */
async function initDeviceModelChart() {
  try {
    const res = await getDeviceModelDistribution();
    deviceModelChart.value = echarts.init(deviceModelChartRef.value, 'macarons');
    deviceModelChart.value.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b} : {c} ({d}%)',
      },
      legend: {
        orient: 'horizontal',
        bottom: 10,
        data: res.data.map((item) => item.name),
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
            borderWidth: 2,
          },
          label: {
            show: true,
            formatter: '{b}\n{d}%',
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: 'bold',
            },
          },
          data: res.data,
          animationEasing: 'cubicInOut',
          animationDuration: 2000,
        },
      ],
    });
  } catch (e) {
    console.error('初始化设备型号图失败：', e);
  }
}

/** 初始化保修状态饼图 */
async function initWarrantyChart() {
  try {
    const res = await getWarrantyStatus();
    warrantyChart.value = echarts.init(warrantyChartRef.value, 'macarons');
    warrantyChart.value.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b} : {c} ({d}%)',
      },
      legend: {
        orient: 'horizontal',
        bottom: 10,
        data: res.data.map((item) => item.name),
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
            borderWidth: 2,
          },
          animationEasing: 'cubicInOut',
          animationDuration: 2000,
        },
      ],
    });
  } catch (e) {
    console.error('初始化保修状态图失败：', e);
  }
}

/** 初始化月度订单柱状图 */
async function initMonthlyChart() {
  try {
    const res = await getMonthlyOrderStats();
    const { months, orderCounts, activatedCounts, signedCounts } = res.data;
    monthlyChart.value = echarts.init(monthlyChartRef.value, 'macarons');
    monthlyChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
      },
      legend: {
        data: ['订单数量', '激活数量', '签约数量'],
        right: 10,
      },
      grid: {
        left: 10,
        right: 20,
        bottom: 20,
        top: 40,
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: months,
        axisTick: { alignWithLabel: true },
      },
      yAxis: {
        type: 'value',
        axisTick: { show: false },
      },
      series: [
        {
          name: '订单数量',
          type: 'bar',
          barWidth: '22%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#409EFF' },
              { offset: 1, color: '#66b1ff' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
          data: orderCounts,
          animationDuration: 2000,
        },
        {
          name: '激活数量',
          type: 'bar',
          barWidth: '22%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#67C23A' },
              { offset: 1, color: '#85ce61' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
          data: activatedCounts,
          animationDuration: 2000,
        },
        {
          name: '签约数量',
          type: 'bar',
          barWidth: '22%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#F56C6C' },
              { offset: 1, color: '#f78989' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
          data: signedCounts,
          animationDuration: 2000,
        },
      ],
    });
  } catch (e) {
    console.error('初始化月度订单图失败：', e);
  }
}

// ==================== 普通用户相关方法 ====================

/** 初始化普通用户数据 */
async function initUserData() {
  await Promise.all([fetchUserStatistics(), fetchUserRecentOrders()]);
  nextTick(() => {
    initUserOrderTrendChart();
  });
}

/** 获取普通用户统计数据 */
async function fetchUserStatistics() {
  try {
    const res = await getUserStatistics();
    userStatistics.value = res.data;
  } catch (e) {
    console.error('获取用户统计数据失败：', e);
  }
}

/** 获取普通用户最近订单 */
async function fetchUserRecentOrders() {
  try {
    const res = await getUserRecentOrders();
    userRecentOrders.value = res.data;
  } catch (e) {
    console.error('获取用户最近订单失败：', e);
  }
}

/** 初始化普通用户订单趋势图 */
async function initUserOrderTrendChart() {
  try {
    const res = await getUserOrderTrend();
    const { dates, myOrders } = res.data;
    userOrderTrendChart.value = echarts.init(userOrderTrendChartRef.value, 'macarons');
    userOrderTrendChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'line' },
        padding: [5, 10],
      },
      grid: {
        left: 10,
        right: 20,
        bottom: 20,
        top: 30,
        containLabel: true,
      },
      xAxis: {
        data: dates,
        boundaryGap: false,
        axisTick: { show: false },
      },
      yAxis: {
        axisTick: { show: false },
        minInterval: 1,
      },
      series: [
        {
          name: '我的订单',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: { color: '#409EFF' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64,158,255,0.35)' },
              { offset: 1, color: 'rgba(64,158,255,0.05)' },
            ]),
          },
          data: myOrders,
          animationDuration: 2000,
          animationEasing: 'cubicInOut',
        },
      ],
    });
  } catch (e) {
    console.error('初始化用户订单趋势图失败：', e);
  }
}

/** 快速查询 */
function handleQuickQuery() {
  ElMessageBox.prompt('请输入序列号（SN）', '快速查询', {
    confirmButtonText: '查询',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入有效的序列号',
  })
    .then(({ value }) => {
      router.push({ path: '/order/list', query: { sn: value } });
    })
    .catch(() => {});
}

// ==================== 公共方法 ====================

/** 获取06API余额 */
async function fetchApiBalance() {
  try {
    const res = await getApiBalance();
    if (res.code === 200 && res.data) {
      apiBalance.value = res.data.balance;
    }
  } catch (e) {
    console.error('获取API余额失败：', e);
    apiBalance.value = '--';
  }
}

/** 格式化业务日期 */
function formatBusinessDate(row) {
  if (!row) {
    return '-';
  }
  if (row.sysTime) {
    return row.sysTime;
  }
  if (row.createTime) {
    return String(row.createTime).slice(0, 10);
  }
  return '-';
}

/** 监听窗口 resize */
function initListener() {
  $_resizeHandler = debounce(() => {
    resizeAllCharts();
  }, 100);
  window.addEventListener('resize', $_resizeHandler);
  $_sidebarElm = document.getElementsByClassName('sidebar-container')[0];
  if ($_sidebarElm) {
    $_sidebarElm.addEventListener('transitionend', handleSidebarResize);
  }
}

/** 销毁监听 */
function destroyListener() {
  window.removeEventListener('resize', $_resizeHandler);
  $_resizeHandler = null;
  if ($_sidebarElm) {
    $_sidebarElm.removeEventListener('transitionend', handleSidebarResize);
  }
}

/** 侧边栏 resize */
function handleSidebarResize(e) {
  if (e.propertyName === 'width') {
    resizeAllCharts();
  }
}

/** 所有图表 resize */
function resizeAllCharts() {
  orderTrendChart.value?.resize();
  deviceModelChart.value?.resize();
  warrantyChart.value?.resize();
  monthlyChart.value?.resize();
  userOrderTrendChart.value?.resize();
}

/** 销毁所有图表 */
function disposeAllCharts() {
  const charts = [
    orderTrendChart.value,
    deviceModelChart.value,
    warrantyChart.value,
    monthlyChart.value,
    userOrderTrendChart.value,
  ];
  charts.forEach((chart) => {
    chart?.dispose();
  });
  orderTrendChart.value = null;
  deviceModelChart.value = null;
  warrantyChart.value = null;
  monthlyChart.value = null;
  userOrderTrendChart.value = null;
}
</script>

<style scoped lang="scss">
.dashboard {
  padding: 12px;

  /* 欢迎卡片（普通用户） */
  .welcome-card {
    :deep(.el-card__body) {
      padding: 24px 30px;
    }
  }

  .welcome-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .welcome-title {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 8px 0;
  }

  .welcome-desc {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }

  .welcome-avatar {
    flex-shrink: 0;
    margin-left: 20px;
  }

  .avatar-img {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid #f0f2f5;
  }

  /* 快捷操作（普通用户） */
  .shortcut-card {
    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .shortcut-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .shortcut-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 16px 8px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: #f5f7fa;
      transform: translateY(-2px);
    }
  }

  .shortcut-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8px;

    i {
      font-size: 22px;
      color: #fff;
    }
  }

  .shortcut-text {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
  }

  /* 余额卡片 */
  .balance-row {
    margin-bottom: 16px;
  }

  .balance-card {
    :deep(.el-card__body) {
      padding: 20px 24px;
    }
  }

  .balance-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .balance-left {
    display: flex;
    align-items: center;
  }

  .balance-icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: linear-gradient(135deg, #8b5cf6, #a78bfa);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;
    flex-shrink: 0;

    i {
      font-size: 24px;
      color: #fff;
    }
  }

  .balance-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 4px;
  }

  .balance-value {
    display: flex;
    align-items: baseline;
  }

  .balance-symbol {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-right: 2px;
  }

  .balance-amount {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
    font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
  }

  .balance-right {
    flex-shrink: 0;
    margin-left: 16px;
  }

  /* 统计卡片 */
  .stat-cards {
    margin-bottom: 16px;
  }

  .stat-card {
    margin-bottom: 12px;

    :deep(.el-card__body) {
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
      color: #67c23a;
    }

    &.down {
      color: #f56c6c;
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

    :deep(.el-card__header) {
      padding: 14px 20px;
      border-bottom: 1px solid #f0f0f0;
    }

    :deep(.el-card__body) {
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
