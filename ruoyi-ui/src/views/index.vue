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
              <span class="balance-status-tag" :class="balanceTagType">
                <el-icon :size="14"><component :is="balanceTagIcon" /></el-icon>
                <span>{{ balanceStatusText }}</span>
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 管理员视图 ==================== -->
    <template v-if="isAdmin">
      <!-- 筛选行：门店维度 + 趋势周期，作用于下方所有统计 -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <span class="filter-label">
            <el-icon style="vertical-align: -2px"><Shop /></el-icon>
            所属门店
          </span>
          <el-select
            v-model="selectedStoreId"
            placeholder="全部门店"
            clearable
            size="small"
            style="width: 220px"
            @change="handleStoreChange"
          >
            <el-option v-for="store in storeList" :key="store.deptId" :label="store.storeName" :value="store.deptId" />
          </el-select>
          <span class="filter-divider"></span>
          <span class="filter-label">趋势周期</span>
          <el-button-group>
            <el-button size="small" :type="trendType === 'week' ? 'primary' : ''" @click="trendType = 'week'"
              >近7天</el-button
            >
            <el-button size="small" :type="trendType === 'month' ? 'primary' : ''" @click="trendType = 'month'"
              >近30天</el-button
            >
          </el-button-group>
          <span class="filter-count">共 {{ storeComparison.length }} 家门店</span>
        </div>
      </el-card>

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
                      ><component :is="statistics.totalOrdersTrend === 'down' ? ArrowDown : ArrowUp"
                    /></el-icon>
                    {{ statistics.totalOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #2a78d6, #66b1ff)">
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
                      ><component :is="statistics.todayOrdersTrend === 'down' ? ArrowDown : ArrowUp"
                    /></el-icon>
                    {{ statistics.todayOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #008300, #52b32a)">
                <el-icon :size="28" color="#fff"><DocumentAdd /></el-icon>
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
                      ><component :is="statistics.signedRateTrend === 'down' ? ArrowDown : ArrowUp"
                    /></el-icon>
                    {{ statistics.signedRateLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #e34948, #f78989)">
                <el-icon :size="28" color="#fff"><EditPen /></el-icon>
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
                      ><component :is="statistics.activationRateTrend === 'down' ? ArrowDown : ArrowUp"
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
      </el-row>

      <!-- 图表区域 - 第一行：订单趋势 + 门店订单对比 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :sm="24" :md="14" :lg="14">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">
                  {{ trendType === 'week' ? '订单趋势（近7天）' : '订单趋势（近30天）' }}
                  <span v-if="selectedStoreName" class="chart-scope">· {{ selectedStoreName }}</span>
                </span>
              </div>
            </template>
            <div ref="orderTrendChartRef" class="chart-container" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="10" :lg="10">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">门店订单对比（TOP10）</span>
              </div>
            </template>
            <div ref="storeCompareChartRef" class="chart-container" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 - 第二行：门店签约构成 + 月度订单统计 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :sm="24" :md="10" :lg="10">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">门店签约构成（TOP8）</span>
              </div>
            </template>
            <div ref="storeSignedChartRef" class="chart-container" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="14" :lg="14">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">
                  月度订单统计（近6个月）
                  <span v-if="selectedStoreName" class="chart-scope">· {{ selectedStoreName }}</span>
                </span>
              </div>
            </template>
            <div ref="monthlyChartRef" class="chart-container" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 门店数据总览 + 最近订单 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :sm="24" :md="14" :lg="14">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">门店数据总览</span>
                <span class="table-hint">点击行可按门店筛选</span>
              </div>
            </template>
            <el-table
              :data="storeComparison"
              style="width: 100%"
              size="small"
              :header-cell-style="{ background: '#fafafa' }"
              :row-style="storeRowStyle"
              class="store-table"
              @row-click="handleStoreRowClick"
            >
              <el-table-column prop="storeName" label="门店" min-width="140" />
              <el-table-column prop="totalOrders" label="订单数" min-width="80" align="center" sortable />
              <el-table-column prop="activatedCount" label="已激活" min-width="80" align="center" sortable />
              <el-table-column prop="signedCount" label="已签约" min-width="80" align="center" sortable />
              <el-table-column
                label="签约率"
                min-width="90"
                align="center"
                sortable
                :sort-by="(row) => signedRateOf(row)"
              >
                <template #default="scope">{{ formatRate(signedRateOf(scope.row)) }}</template>
              </el-table-column>
              <el-table-column
                label="激活率"
                min-width="90"
                align="center"
                sortable
                :sort-by="(row) => activationRateOf(row)"
              >
                <template #default="scope">{{ formatRate(activationRateOf(scope.row)) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="10" :lg="10">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">
                  最近订单
                  <span v-if="selectedStoreName" class="chart-scope">· {{ selectedStoreName }}</span>
                </span>
                <el-button size="mini" type="text" :icon="DArrowRight" @click="router.push('/info/list')"
                  >查看更多</el-button
                >
              </div>
            </template>
            <el-table
              :data="recentOrders"
              style="width: 100%"
              size="small"
              :header-cell-style="{ background: '#fafafa' }"
            >
              <el-table-column prop="signatureModel" label="签约型号" min-width="110" />
              <el-table-column prop="signatureImei" label="签约IMEI" min-width="140" />
              <el-table-column prop="createBy" label="创建人" min-width="80" align="center" />
              <el-table-column label="签约日期" min-width="100" align="center">
                <template #default="scope">
                  {{ scope.row.signatureDate || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="剩余保修" min-width="110" align="center">
                <template #default="scope">
                  <template v-if="scope.row.signatureDate">
                    <el-tag :type="getWarrantyTagType(scope.row)" size="small">
                      {{ getRemainingWarranty(scope.row) }}
                    </el-tag>
                  </template>
                  <span v-else style="color: #909399">-</span>
                </template>
              </el-table-column>
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
                      ><component :is="userStatistics.myTotalOrdersTrend === 'down' ? ArrowDown : ArrowUp"
                    /></el-icon>
                    {{ userStatistics.myTotalOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #2a78d6, #66b1ff)">
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
                      ><component :is="userStatistics.myTodayOrdersTrend === 'down' ? ArrowDown : ArrowUp"
                    /></el-icon>
                    {{ userStatistics.myTodayOrdersGrowthLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #008300, #52b32a)">
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
                      ><component :is="userStatistics.myActivationRateTrend === 'down' ? ArrowDown : ArrowUp"
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
                      ><component :is="userStatistics.mySignedRateTrend === 'down' ? ArrowDown : ArrowUp"
                    /></el-icon>
                    {{ userStatistics.mySignedRateLabel }}</span
                  >
                </div>
              </div>
              <div class="stat-icon" style="background: linear-gradient(135deg, #e34948, #f78989)">
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
              <div class="shortcut-item" @click="router.push('/info/list')">
                <div class="shortcut-icon" style="background: linear-gradient(135deg, #2a78d6, #66b1ff)">
                  <el-icon :size="22" color="#fff"><Tickets /></el-icon>
                </div>
                <span class="shortcut-text">订单列表</span>
              </div>
              <div class="shortcut-item" @click="router.push('/user/profile')">
                <div class="shortcut-icon" style="background: linear-gradient(135deg, #008300, #52b32a)">
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

      <!-- 最近订单（我的 / 门店维度切换） -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">
                  <el-radio-group v-model="userOrderScope" size="small">
                    <el-radio-button value="mine">我的订单</el-radio-button>
                    <el-radio-button value="store">门店订单</el-radio-button>
                  </el-radio-group>
                  <span v-if="userOrderScope === 'store' && userStoreName" class="chart-scope"
                    >· {{ userStoreName }}</span
                  >
                </span>
                <el-button size="mini" type="text" :icon="DArrowRight" @click="router.push('/info/list')"
                  >查看更多</el-button
                >
              </div>
            </template>
            <el-table
              :data="displayedUserRecentOrders"
              style="width: 100%"
              size="medium"
              :header-cell-style="{ background: '#fafafa' }"
            >
              <el-table-column prop="signatureModel" label="签约型号" min-width="140" />
              <el-table-column prop="signatureImei" label="签约IMEI" min-width="160" />
              <el-table-column v-if="userOrderScope === 'store'" label="创建人" min-width="100" align="center">
                <template #default="scope">
                  {{ scope.row.nickName || scope.row.createBy || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="签约日期" min-width="120" align="center">
                <template #default="scope">
                  {{ scope.row.signatureDate || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="剩余保修" min-width="130" align="center">
                <template #default="scope">
                  <template v-if="scope.row.signatureDate">
                    <el-tag :type="getWarrantyTagType(scope.row)" size="small">
                      {{ getRemainingWarranty(scope.row) }}
                    </el-tag>
                  </template>
                  <span v-else style="color: #909399">-</span>
                </template>
              </el-table-column>
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
  Shop,
  Loading,
  WarningFilled,
  InfoFilled,
  CircleCheckFilled,
} from '@element-plus/icons-vue';
import useUserStore from '@/store/modules/user';
import {
  getStatistics,
  getOrderTrend,
  getMonthlyOrderStats,
  getRecentOrders,
  getStoreList,
  getStoreComparison,
  getUserStatistics,
  getUserOrderTrend,
  getUserRecentOrders,
  getStoreRecentOrders,
  getApiBalance,
} from '@/api/dashboard';

const router = useRouter();
const userStore = useUserStore();

// ==================== 图表配色 ====================
// 系列色（蓝/绿/红，已通过色盲安全校验，需配合点形状等二级编码）
const COLOR_BLUE = '#2a78d6';
const COLOR_GREEN = '#008300';
const COLOR_RED = '#e34948';
const COLOR_GRAY = '#dcdfe6';
// 文本墨水色（轴/标签，不与系列色混用）
const INK_MUTED = '#909399';
const INK_SECONDARY = '#606266';

// ==================== 响应式数据 ====================

// 公共数据
const apiBalance = ref('--');

// 门店筛选
const storeList = ref([]);
const selectedStoreId = ref(null);
const storeComparison = ref([]);

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
const storeCompareChart = ref(null);
const storeSignedChart = ref(null);
const monthlyChart = ref(null);

// 管理员图表 ref 引用
const orderTrendChartRef = ref(null);
const storeCompareChartRef = ref(null);
const storeSignedChartRef = ref(null);
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
// 门店维度最近订单（所属门店全部人员）
const storeRecentOrders = ref([]);
// 最近订单展示维度：mine=我的订单，store=门店订单
const userOrderScope = ref('mine');
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

/** 当前选中门店名称（未选返回空） */
const selectedStoreName = computed(() => {
  if (!selectedStoreId.value) return '';
  const store = storeList.value.find((item) => item.deptId === selectedStoreId.value);
  return store ? store.storeName : '';
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
  if (isNaN(val)) return Loading;
  if (val <= 1) return WarningFilled;
  if (val <= 5) return InfoFilled;
  return CircleCheckFilled;
});

/** 获取员工姓名 */
const nickName = computed(() => {
  return userStore.nickName || userStore.name || '用户';
});

/** 获取用户头像 */
const avatar = computed(() => {
  return userStore.avatar;
});

/** 普通用户最近订单展示列表（按 我的/门店 维度切换） */
const displayedUserRecentOrders = computed(() =>
  userOrderScope.value === 'store' ? storeRecentOrders.value : userRecentOrders.value
);

/** 门店维度下所属门店名称（取第一条数据的门店名） */
const userStoreName = computed(() => storeRecentOrders.value[0]?.storeName || '');

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
  await Promise.all([fetchStoreList(), fetchStoreComparison(), fetchStatistics(), fetchRecentOrders()]);
  nextTick(() => {
    initOrderTrendChart();
    initStoreCompareChart();
    initStoreSignedChart();
    initMonthlyChart();
  });
}

/** 门店筛选变更：重新拉取门店维度下的所有统计 */
async function handleStoreChange() {
  await Promise.all([fetchStatistics(), fetchRecentOrders()]);
  nextTick(() => {
    initOrderTrendChart();
    initMonthlyChart();
    initStoreCompareChart();
  });
}

/** 门店总览表行点击：切换门店筛选 */
function handleStoreRowClick(row) {
  selectedStoreId.value = selectedStoreId.value === row.deptId ? null : row.deptId;
  handleStoreChange();
}

/** 门店总览表选中行高亮 */
function storeRowStyle({ row }) {
  if (row.deptId === selectedStoreId.value) {
    return { background: '#ecf5ff', cursor: 'pointer' };
  }
  return { cursor: 'pointer' };
}

/** 签约率（百分比数值） */
function signedRateOf(row) {
  return row.totalOrders > 0 ? (row.signedCount / row.totalOrders) * 100 : 0;
}

/** 激活率（百分比数值） */
function activationRateOf(row) {
  return row.totalOrders > 0 ? (row.activatedCount / row.totalOrders) * 100 : 0;
}

/** 百分比格式化 */
function formatRate(rate) {
  return rate.toFixed(1) + '%';
}

/** 获取门店列表 */
async function fetchStoreList() {
  try {
    const res = await getStoreList();
    storeList.value = res.data || [];
  } catch (e) {
    console.error('获取门店列表失败：', e);
  }
}

/** 获取门店对比统计 */
async function fetchStoreComparison() {
  try {
    const res = await getStoreComparison();
    storeComparison.value = res.data || [];
  } catch (e) {
    console.error('获取门店对比统计失败：', e);
  }
}

/** 获取统计卡片数据 */
async function fetchStatistics() {
  try {
    const res = await getStatistics(selectedStoreId.value);
    statistics.value = res.data;
  } catch (e) {
    console.error('获取统计数据失败：', e);
  }
}

/** 获取最近订单 */
async function fetchRecentOrders() {
  try {
    const res = await getRecentOrders(selectedStoreId.value);
    recentOrders.value = res.data;
  } catch (e) {
    console.error('获取最近订单失败：', e);
  }
}

/** 初始化订单趋势折线图 */
async function initOrderTrendChart() {
  try {
    const res = await getOrderTrend(trendType.value, selectedStoreId.value);
    const { dates, newOrders, completedOrders, signedOrders } = res.data;
    if (!orderTrendChart.value) {
      orderTrendChart.value = echarts.init(orderTrendChartRef.value);
    }
    orderTrendChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        padding: [5, 10],
      },
      legend: {
        data: ['新增订单', '激活订单', '签约订单'],
        right: 10,
        top: 0,
      },
      grid: {
        left: 10,
        right: 20,
        bottom: 20,
        top: 36,
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: dates,
        boundaryGap: false,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#c3c2b7' } },
        axisLabel: { color: INK_MUTED },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: INK_MUTED },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'solid' } },
      },
      series: [
        {
          name: '新增订单',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { width: 2, color: COLOR_BLUE },
          itemStyle: { color: COLOR_BLUE, borderColor: '#fff', borderWidth: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(42,120,214,0.15)' },
              { offset: 1, color: 'rgba(42,120,214,0.02)' },
            ]),
          },
          data: newOrders,
          animationDuration: 1000,
        },
        {
          name: '激活订单',
          type: 'line',
          smooth: true,
          symbol: 'triangle',
          symbolSize: 9,
          lineStyle: { width: 2, color: COLOR_GREEN },
          itemStyle: { color: COLOR_GREEN, borderColor: '#fff', borderWidth: 2 },
          data: completedOrders,
          animationDuration: 1000,
        },
        {
          name: '签约订单',
          type: 'line',
          smooth: true,
          symbol: 'diamond',
          symbolSize: 9,
          lineStyle: { width: 2, color: COLOR_RED },
          itemStyle: { color: COLOR_RED, borderColor: '#fff', borderWidth: 2 },
          data: signedOrders,
          animationDuration: 1000,
        },
      ],
    });
  } catch (e) {
    console.error('初始化订单趋势图失败：', e);
  }
}

/** 初始化门店订单对比横向柱状图（TOP10，选中门店高亮，其余置灰） */
function initStoreCompareChart() {
  try {
    // 按订单数降序取前10，横向柱状图反转后最大值在顶部
    const top = [...storeComparison.value].sort((a, b) => b.totalOrders - a.totalOrders).slice(0, 10).reverse();
    const hasSelection = !!selectedStoreId.value;
    if (!storeCompareChart.value) {
      storeCompareChart.value = echarts.init(storeCompareChartRef.value);
    }
    storeCompareChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
      },
      grid: {
        left: 10,
        right: 36,
        bottom: 4,
        top: 4,
        containLabel: true,
      },
      xAxis: {
        type: 'value',
        minInterval: 1,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: INK_MUTED },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'solid' } },
      },
      yAxis: {
        type: 'category',
        data: top.map((item) => item.storeName),
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#c3c2b7' } },
        axisLabel: { color: INK_SECONDARY },
      },
      series: [
        {
          name: '订单数',
          type: 'bar',
          barWidth: 16,
          data: top.map((item) => ({
            value: item.totalOrders,
            itemStyle: {
              color:
                !hasSelection || item.deptId === selectedStoreId.value ? COLOR_BLUE : COLOR_GRAY,
              borderRadius: [0, 4, 4, 0],
            },
          })),
          label: {
            show: true,
            position: 'right',
            color: INK_MUTED,
            formatter: (params) => (params.value > 0 ? params.value : ''),
          },
          animationDuration: 800,
        },
      ],
    });
  } catch (e) {
    console.error('初始化门店订单对比图失败：', e);
  }
}

/** 初始化门店签约构成横向堆叠柱状图（TOP8） */
function initStoreSignedChart() {
  try {
    const top = [...storeComparison.value].sort((a, b) => b.totalOrders - a.totalOrders).slice(0, 8).reverse();
    const signed = top.map((item) => item.signedCount);
    const unsigned = top.map((item) => Math.max(item.totalOrders - item.signedCount, 0));
    if (!storeSignedChart.value) {
      storeSignedChart.value = echarts.init(storeSignedChartRef.value);
    }
    storeSignedChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
      },
      legend: {
        data: ['已签约', '未签约'],
        right: 10,
        top: 0,
      },
      grid: {
        left: 10,
        right: 36,
        bottom: 4,
        top: 36,
        containLabel: true,
      },
      xAxis: {
        type: 'value',
        minInterval: 1,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: INK_MUTED },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'solid' } },
      },
      yAxis: {
        type: 'category',
        data: top.map((item) => item.storeName),
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#c3c2b7' } },
        axisLabel: { color: INK_SECONDARY },
      },
      series: [
        {
          name: '已签约',
          type: 'bar',
          stack: 'total',
          barWidth: 16,
          itemStyle: { color: COLOR_BLUE },
          data: signed,
          animationDuration: 800,
        },
        {
          name: '未签约',
          type: 'bar',
          stack: 'total',
          itemStyle: { color: COLOR_GRAY, borderRadius: [0, 4, 4, 0] },
          data: unsigned,
          animationDuration: 800,
        },
      ],
    });
  } catch (e) {
    console.error('初始化门店签约构成图失败：', e);
  }
}

/** 初始化月度订单柱状图 */
async function initMonthlyChart() {
  try {
    const res = await getMonthlyOrderStats(selectedStoreId.value);
    const { months, orderCounts, activatedCounts, signedCounts } = res.data;
    if (!monthlyChart.value) {
      monthlyChart.value = echarts.init(monthlyChartRef.value);
    }
    monthlyChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
      },
      legend: {
        data: ['订单数量', '激活数量', '签约数量'],
        right: 10,
        top: 0,
      },
      grid: {
        left: 10,
        right: 20,
        bottom: 20,
        top: 36,
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: months,
        axisTick: { alignWithLabel: true },
        axisLine: { lineStyle: { color: '#c3c2b7' } },
        axisLabel: { color: INK_MUTED },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: INK_MUTED },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'solid' } },
      },
      series: [
        {
          name: '订单数量',
          type: 'bar',
          barWidth: 14,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: COLOR_BLUE },
              { offset: 1, color: '#66b1ff' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
          data: orderCounts,
          animationDuration: 1000,
        },
        {
          name: '激活数量',
          type: 'bar',
          barWidth: 14,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: COLOR_GREEN },
              { offset: 1, color: '#4db82e' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
          data: activatedCounts,
          animationDuration: 1000,
        },
        {
          name: '签约数量',
          type: 'bar',
          barWidth: 14,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: COLOR_RED },
              { offset: 1, color: '#f78989' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
          data: signedCounts,
          animationDuration: 1000,
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
  await Promise.all([fetchUserStatistics(), fetchUserRecentOrders(), fetchStoreRecentOrders()]);
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

/** 获取门店最近订单（所属门店全部人员） */
async function fetchStoreRecentOrders() {
  try {
    const res = await getStoreRecentOrders();
    storeRecentOrders.value = res.data || [];
  } catch (e) {
    console.error('获取门店最近订单失败：', e);
  }
}

/** 初始化普通用户订单趋势图 */
async function initUserOrderTrendChart() {
  try {
    const res = await getUserOrderTrend();
    const { dates, myOrders } = res.data;
    userOrderTrendChart.value = echarts.init(userOrderTrendChartRef.value);
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
        type: 'category',
        data: dates,
        boundaryGap: false,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#c3c2b7' } },
        axisLabel: { color: INK_MUTED },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: INK_MUTED },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'solid' } },
      },
      series: [
        {
          name: '我的订单',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { width: 2, color: COLOR_BLUE },
          itemStyle: { color: COLOR_BLUE, borderColor: '#fff', borderWidth: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(42,120,214,0.15)' },
              { offset: 1, color: 'rgba(42,120,214,0.02)' },
            ]),
          },
          data: myOrders,
          animationDuration: 1000,
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

/** 保修期（年） */
const WARRANTY_YEARS = 2;

/** 计算剩余保修天数（从签约日期起保2年） */
function getRemainingWarranty(row) {
  if (!row || !row.signatureDate) return '-';
  const signDate = new Date(row.signatureDate);
  if (isNaN(signDate.getTime())) return '-';
  const expireDate = new Date(signDate);
  expireDate.setFullYear(expireDate.getFullYear() + WARRANTY_YEARS);
  const now = new Date();
  const diffMs = expireDate.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays <= 0) {
    return '已过期' + Math.abs(diffDays) + '天';
  }
  if (diffDays <= 30) {
    return '仅剩' + diffDays + '天';
  }
  const diffMonths = Math.floor(diffDays / 30);
  if (diffMonths < 12) {
    return '约' + diffMonths + '个月';
  }
  const years = Math.floor(diffMonths / 12);
  const remainMonths = diffMonths % 12;
  if (remainMonths > 0) {
    return '约' + years + '年' + remainMonths + '个月';
  }
  return '约' + years + '年';
}

/** 根据剩余保修天数返回标签类型 */
function getWarrantyTagType(row) {
  if (!row || !row.signatureDate) return 'info';
  const signDate = new Date(row.signatureDate);
  if (isNaN(signDate.getTime())) return 'info';
  const expireDate = new Date(signDate);
  expireDate.setFullYear(expireDate.getFullYear() + WARRANTY_YEARS);
  const now = new Date();
  const diffMs = expireDate.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays <= 0) return 'danger';
  if (diffDays <= 30) return 'warning';
  if (diffDays <= 180) return '';
  return 'success';
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
  storeCompareChart.value?.resize();
  storeSignedChart.value?.resize();
  monthlyChart.value?.resize();
  userOrderTrendChart.value?.resize();
}

/** 销毁所有图表 */
function disposeAllCharts() {
  const charts = [
    orderTrendChart.value,
    storeCompareChart.value,
    storeSignedChart.value,
    monthlyChart.value,
    userOrderTrendChart.value,
  ];
  charts.forEach((chart) => {
    chart?.dispose();
  });
  orderTrendChart.value = null;
  storeCompareChart.value = null;
  storeSignedChart.value = null;
  monthlyChart.value = null;
  userOrderTrendChart.value = null;
}
</script>

<style scoped lang="scss">
.dashboard {
  padding: 12px;

  /* 门店筛选行 */
  .filter-card {
    margin-bottom: 16px;

    :deep(.el-card__body) {
      padding: 12px 20px;
    }
  }

  .filter-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }

  .filter-label {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
    margin-right: 8px;
  }

  .filter-divider {
    width: 1px;
    height: 16px;
    background: #dcdfe6;
    margin: 0 16px;
  }

  .filter-count {
    margin-left: auto;
    font-size: 12px;
    color: #909399;
  }

  /* 图表标题后的门店范围提示 */
  .chart-scope {
    font-size: 12px;
    font-weight: 400;
    color: #909399;
  }

  /* 门店总览表提示 */
  .table-hint {
    font-size: 12px;
    color: #909399;
  }

  .store-table {
    cursor: pointer;
  }

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

  .balance-status-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 500;
    border: 1px solid;

    &.success {
      color: #67c23a;
      background: #f0f9eb;
      border-color: #c2e7b0;
    }

    &.warning {
      color: #e6a23c;
      background: #fdf6ec;
      border-color: #f5dab1;
    }

    &.danger {
      color: #f56c6c;
      background: #fef0f0;
      border-color: #fbc4c4;
    }

    &.info {
      color: #909399;
      background: #f4f4f5;
      border-color: #d3d4d6;
    }
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
