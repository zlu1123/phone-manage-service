<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form
      v-if="searchFields && searchFields.length > 0"
      ref="queryForm"
      :model="queryParams"
      size="small"
      :inline="true"
      v-show="showSearch"
      :label-width="searchLabelWidth"
    >
      <el-form-item v-for="field in searchFields" :key="field.prop" :label="field.label" :prop="field.prop">
        <!-- 输入框 -->
        <el-input
          v-if="field.type === 'input' || !field.type"
          v-model="queryParams[field.prop]"
          :placeholder="field.placeholder || `请输入${field.label}`"
          clearable
          :style="{ width: field.width || '200px' }"
          @keyup.enter.native="handleQuery"
        />
        <!-- 下拉选择 -->
        <el-select
          v-else-if="field.type === 'select'"
          v-model="queryParams[field.prop]"
          :placeholder="field.placeholder || `请选择${field.label}`"
          clearable
          :style="{ width: field.width || '200px' }"
        >
          <el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <!-- 日期选择 -->
        <el-date-picker
          v-else-if="field.type === 'date'"
          v-model="queryParams[field.prop]"
          type="date"
          :placeholder="field.placeholder || `请选择${field.label}`"
          clearable
          value-format="yyyy-MM-dd"
          :style="{ width: field.width || '200px' }"
        />
        <!-- 日期范围 -->
        <el-date-picker
          v-else-if="field.type === 'daterange'"
          v-model="queryParams[field.prop]"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          clearable
          value-format="yyyy-MM-dd"
          :style="{ width: field.width || '340px' }"
        />
        <!-- 自定义搜索项插槽 -->
        <slot v-else-if="field.type === 'slot'" :name="`search-${field.prop}`" :queryParams="queryParams" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <!-- 工具栏左侧插槽：放置操作按钮 -->
      <slot name="toolbar" :queryParams="queryParams" :loading="loading" />
      <right-toolbar :showSearch.sync="showSearch" @queryTable="fetchData"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" v-bind="$attrs" v-on="tableListeners">
      <template v-for="col in columns">
        <!-- 展开列 -->
        <el-table-column
          v-if="col.slot === 'expand'"
          :key="'expand'"
          type="expand"
          :width="col.width"
          :align="col.align || 'center'"
        >
          <template slot-scope="scope">
            <slot name="expand" :row="scope.row" :index="scope.$index" />
          </template>
        </el-table-column>
        <!-- 有自定义插槽的列 -->
        <el-table-column
          v-else-if="col.slot"
          :key="col.prop || col.slot"
          :label="col.label"
          :prop="col.prop"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align || 'center'"
          :show-overflow-tooltip="col.showOverflowTooltip !== false"
          :render-header="col.renderHeader"
          v-bind="col.attrs"
        >
          <template slot-scope="scope">
            <slot :name="col.slot" :row="scope.row" :index="scope.$index" />
          </template>
        </el-table-column>
        <!-- 普通列 -->
        <el-table-column
          v-else
          :key="col.prop"
          :label="col.label"
          :prop="col.prop"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align || 'center'"
          :show-overflow-tooltip="col.showOverflowTooltip !== false"
          :render-header="col.renderHeader"
          v-bind="col.attrs"
        />
      </template>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="fetchData"
    />
  </div>
</template>

<script>
export default {
  name: 'ProTable',
  props: {
    /**
     * 列表请求接口函数
     * 接收查询参数对象，返回 Promise
     */
    fetchApi: {
      type: Function,
      required: true,
    },
    /**
     * 搜索字段配置数组，每项包含：
     * - prop: 字段名（绑定到 queryParams）
     * - label: 表单标签
     * - type: 'input' | 'select' | 'date' | 'daterange' | 'slot'，默认 'input'
     * - placeholder: 占位文本（可选）
     * - width: 控件宽度（可选）
     * - options: 下拉选项数组 [{ label, value }]（type 为 select 时必填）
     * - defaultValue: 默认值（可选）
     */
    searchFields: {
      type: Array,
      default: () => [],
    },
    /**
     * 表格列配置数组，每项包含：
     * - label: 列标题
     * - prop: 字段名
     * - width: 列宽（可选）
     * - minWidth: 最小列宽（可选）
     * - align: 对齐方式，默认 'center'
     * - showOverflowTooltip: 是否超出隐藏，默认 true
     * - slot: 自定义插槽名（可选，有此项时使用具名插槽渲染）
     * - attrs: 其他 el-table-column 属性（可选）
     */
    columns: {
      type: Array,
      required: true,
    },
    /**
     * 搜索表单 label 宽度
     */
    searchLabelWidth: {
      type: String,
      default: '90px',
    },
    /**
     * 每页条数，默认 10
     */
    pageSize: {
      type: Number,
      default: 10,
    },
    /**
     * 自定义解析接口返回数据的函数
     * 接收 response，返回 { rows: [], total: 0 }
     */
    parseResponse: {
      type: Function,
      default: null,
    },
    /**
     * 额外的固定查询参数（不会被重置）
     */
    extraParams: {
      type: Object,
      default: () => ({}),
    },
    /**
     * 是否开启静默刷新（轮询），默认 false
     * 静默刷新不会显示 loading 状态，在后台定时请求数据并更新表格
     */
    silentRefresh: {
      type: Boolean,
      default: false,
    },
    /**
     * 静默刷新间隔时间（毫秒），默认 30000ms（30秒）
     * 仅在 silentRefresh 为 true 时生效
     */
    silentRefreshInterval: {
      type: Number,
      default: 30000,
    },
  },
  data() {
    return {
      // 是否显示搜索
      showSearch: true,
      // 加载状态
      loading: false,
      // 表格数据
      tableData: [],
      // 总条数
      total: 0,
      // 查询参数
      queryParams: this.buildInitialParams(),
      // 静默刷新定时器
      silentRefreshTimer: null,
    };
  },
  computed: {
    // 过滤掉分页相关的事件，避免冲突
    tableListeners() {
      const { pagination, ...rest } = this.$listeners;
      return rest;
    },
  },
  watch: {
    // 监听 silentRefresh 变化，动态开启/关闭轮询
    silentRefresh(val) {
      if (val) {
        this.startSilentRefresh();
      } else {
        this.stopSilentRefresh();
      }
    },
    // 监听轮询间隔变化，重启轮询
    silentRefreshInterval() {
      if (this.silentRefresh) {
        this.stopSilentRefresh();
        this.startSilentRefresh();
      }
    },
  },
  created() {
    this.fetchData();
    // 如果开启了静默刷新，启动定时器
    if (this.silentRefresh) {
      this.startSilentRefresh();
    }
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    this.stopSilentRefresh();
  },
  methods: {
    /** 构建初始查询参数 */
    buildInitialParams() {
      const params = {
        pageNum: 1,
        pageSize: this.pageSize,
      };
      // 根据 searchFields 初始化各字段
      if (this.searchFields && this.searchFields.length > 0) {
        this.searchFields.forEach((field) => {
          params[field.prop] = field.defaultValue !== undefined ? field.defaultValue : undefined;
        });
      }
      return params;
    },
    /** 请求列表数据 */
    fetchData() {
      this.loading = true;
      const params = { ...this.queryParams, ...this.extraParams };
      this.fetchApi(params)
        .then((response) => {
          let rows, total;
          if (this.parseResponse) {
            const result = this.parseResponse(response);
            rows = result.rows;
            total = result.total;
          } else {
            // 默认兼容两种返回格式
            const data = response.data || response;
            rows = data.rows || [];
            total = data.total || 0;
          }
          this.tableData = rows;
          this.total = total;
          this.loading = false;
          this.$emit('data-loaded', { rows, total });
        })
        .catch((err) => {
          console.error('ProTable 查询列表失败：', err);
          this.loading = false;
        });
    },
    /** 搜索 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.fetchData();
    },
    /** 重置搜索 */
    handleResetQuery() {
      this.$refs.queryForm && this.$refs.queryForm.resetFields();
      // 手动重置所有搜索字段
      this.searchFields.forEach((field) => {
        this.$set(this.queryParams, field.prop, field.defaultValue !== undefined ? field.defaultValue : undefined);
      });
      this.handleQuery();
    },
    /** 静默请求列表数据（不显示 loading） */
    fetchDataSilently() {
      const params = { ...this.queryParams, ...this.extraParams };
      this.fetchApi(params)
        .then((response) => {
          let rows, total;
          if (this.parseResponse) {
            const result = this.parseResponse(response);
            rows = result.rows;
            total = result.total;
          } else {
            const data = response.data || response;
            rows = data.rows || [];
            total = data.total || 0;
          }
          this.tableData = rows;
          this.total = total;
          this.$emit('data-loaded', { rows, total });
        })
        .catch((err) => {
          console.error('ProTable 静默刷新失败：', err);
        });
    },
    /** 启动静默刷新定时器 */
    startSilentRefresh() {
      this.stopSilentRefresh();
      this.silentRefreshTimer = setInterval(() => {
        this.fetchDataSilently();
      }, this.silentRefreshInterval);
    },
    /** 停止静默刷新定时器 */
    stopSilentRefresh() {
      if (this.silentRefreshTimer) {
        clearInterval(this.silentRefreshTimer);
        this.silentRefreshTimer = null;
      }
    },
    /** 刷新列表（供父组件调用） */
    refresh() {
      this.fetchData();
    },
    /** 获取当前查询参数（供父组件调用，如导出时使用） */
    getQueryParams() {
      return { ...this.queryParams, ...this.extraParams };
    },
  },
};
</script>
