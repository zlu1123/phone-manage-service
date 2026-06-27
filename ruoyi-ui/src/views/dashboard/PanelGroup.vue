<template>
  <el-row :gutter="20" class="panel-group">
    <!-- API余额 -->
    <el-col :xs="12" :sm="12" :lg="4" class="card-panel-col">
      <div class="card-panel">
        <div class="card-panel-icon-wrapper icon-balance">
          <svg-icon icon-class="money" class-name="card-panel-icon" />
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">API余额</div>
          <div class="card-panel-num">
            <span class="card-panel-unit">¥</span>
            <count-to :start-val="0" :end-val="statistics.apiBalance || 0" :duration="2000" :decimals="2" />
          </div>
          <div class="card-panel-sub">{{ statistics.balanceStatus || '余额充足' }}</div>
        </div>
      </div>
    </el-col>
    <!-- 订单总数 -->
    <el-col :xs="12" :sm="12" :lg="4" class="card-panel-col">
      <div class="card-panel">
        <div class="card-panel-icon-wrapper icon-order">
          <svg-icon icon-class="list" class-name="card-panel-icon" />
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">订单总数</div>
          <count-to :start-val="0" :end-val="statistics.orderTotal || 0" :duration="2000" class="card-panel-num" />
        </div>
      </div>
    </el-col>
    <!-- 今日新增 -->
    <el-col :xs="12" :sm="12" :lg="4" class="card-panel-col">
      <div class="card-panel">
        <div class="card-panel-icon-wrapper icon-today">
          <svg-icon icon-class="date" class-name="card-panel-icon" />
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">今日新增</div>
          <count-to :start-val="0" :end-val="statistics.todayNew || 0" :duration="2000" class="card-panel-num" />
          <div class="card-panel-sub">{{ statistics.todayNewChange || '较昨日没有变化' }}</div>
        </div>
      </div>
    </el-col>
    <!-- 已激活设备 -->
    <el-col :xs="12" :sm="12" :lg="4" class="card-panel-col">
      <div class="card-panel">
        <div class="card-panel-icon-wrapper icon-activate">
          <svg-icon icon-class="component" class-name="card-panel-icon" />
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">已激活设备</div>
          <count-to :start-val="0" :end-val="statistics.activatedDevices || 0" :duration="2000" class="card-panel-num" />
          <div class="card-panel-sub">激活率 {{ statistics.activationRate || 0 }}%</div>
        </div>
      </div>
    </el-col>
    <!-- 已签约 -->
    <el-col :xs="12" :sm="12" :lg="4" class="card-panel-col">
      <div class="card-panel">
        <div class="card-panel-icon-wrapper icon-sign">
          <svg-icon icon-class="documentation" class-name="card-panel-icon" />
        </div>
        <div class="card-panel-description">
          <div class="card-panel-text">已签约</div>
          <count-to :start-val="0" :end-val="statistics.signedDevices || 0" :duration="2000" class="card-panel-num" />
          <div class="card-panel-sub">签约率 {{ statistics.signRate || 0 }}%</div>
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import CountTo from '@/components/CountTo/index.vue'

defineProps({
  statistics: {
    type: Object,
    default: () => ({
      apiBalance: 0,
      balanceStatus: '余额充足',
      orderTotal: 0,
      todayNew: 0,
      todayNewChange: '较昨日没有变化',
      activatedDevices: 0,
      activationRate: 0,
      signedDevices: 0,
      signRate: 0,
    }),
  },
})
</script>

<style lang="scss" scoped>
.panel-group {
  margin-top: 18px;

  .card-panel-col {
    margin-bottom: 24px;
  }

  .card-panel {
    height: 130px;
    font-size: 12px;
    position: relative;
    overflow: hidden;
    color: #666;
    background: #fff;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, .05);
    border-radius: 4px;

    &:hover {
      .card-panel-icon-wrapper {
        color: #fff;
      }
      .icon-balance { background: #40c9c6; }
      .icon-order { background: #36a3f7; }
      .icon-today { background: #f4516c; }
      .icon-activate { background: #34bfa3; }
      .icon-sign { background: #ffa800; }
    }

    .icon-balance { color: #40c9c6; }
    .icon-order { color: #36a3f7; }
    .icon-today { color: #f4516c; }
    .icon-activate { color: #34bfa3; }
    .icon-sign { color: #ffa800; }

    .card-panel-icon-wrapper {
      float: left;
      margin: 14px 0 0 14px;
      padding: 16px;
      transition: all 0.38s ease-out;
      border-radius: 6px;
    }

    .card-panel-icon {
      float: left;
      font-size: 48px;
    }

    .card-panel-description {
      float: right;
      font-weight: bold;
      margin: 18px 20px 18px 0;

      .card-panel-text {
        line-height: 18px;
        color: rgba(0, 0, 0, 0.45);
        font-size: 14px;
        margin-bottom: 8px;
      }

      .card-panel-num {
        font-size: 24px;
        color: #333;
      }

      .card-panel-unit {
        font-size: 16px;
        color: #666;
      }

      .card-panel-sub {
        font-size: 12px;
        font-weight: normal;
        color: #999;
        margin-top: 4px;
      }
    }
  }
}

@media (max-width: 768px) {
  .card-panel-description {
    display: none;
  }
  .card-panel-icon-wrapper {
    float: none !important;
    display: block;
    margin: 20px auto 0 !important;
    text-align: center;
    .svg-icon {
      display: inline-block;
      float: none !important;
    }
  }
}
</style>
