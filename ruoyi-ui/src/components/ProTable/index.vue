<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form
      v-if="searchFields && searchFields.length > 0"
      ref="queryFormRef"
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
          @keyup.enter="handleQuery"
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
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <!-- 工具栏左侧插槽：放置操作按钮 -->
      <slot name="toolbar" :queryParams="queryParams" :loading="loading" />
      <right-toolbar v-model:showSearch="showSearch" @queryTable="fetchData"></right-toolbar>
    </el-row>

    <!-- 表格提示信息 -->
    <slot name="table-tip" />

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" v-bind="tableAttrs">
      <template v-for="col in columns" :key="col.prop || col.slot">
        <!-- 展开列 -->
        <el-table-column
          v-if="col.slot === 'expand'"
          key="expand"
          type="expand"
          :width="col.width"
          :align="col.align || 'center'"
        >
          <template #default="scope">
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
          <template v-if="col.useHeaderSlot" #header>
            <slot :name="`header-${col.slot}`" />
          </template>
          <template #default="scope">
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
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, useAttrs } from 'vue';

const props = defineProps({
  /**
   * 列表请求接口函数
   */
  fetchApi: {
    type: Function,
    required: true,
  },
  /**
   * 搜索字段配置数组
   */
  searchFields: {
    type: Array,
    default: () => [],
  },
  /**
   * 表格列配置数组
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
   * 是否开启静默刷新（轮询）
   */
  silentRefresh: {
    type: Boolean,
    default: false,
  },
  /**
   * 静默刷新间隔时间（毫秒）
   */
  silentRefreshInterval: {
    type: Number,
    default: 30000,
  },
});

const emit = defineEmits(['data-loaded']);

const attrs = useAttrs();

// 表格属性（过滤掉分页相关事件）
const tableAttrs = computed(() => {
  const { onPagination, ...rest } = attrs;
  return rest;
});

// 是否显示搜索
const showSearch = ref(true);
// 加载状态
const loading = ref(false);
// 表格数据
const tableData = ref([]);
// 总条数
const total = ref(0);
// 查询表单引用
const queryFormRef = ref(null);
// 静默刷新定时器
let silentRefreshTimer = null;

/** 构建初始查询参数 */
const buildInitialParams = () => {
  const params = reactive({
    pageNum: 1,
    pageSize: props.pageSize,
  });
  if (props.searchFields && props.searchFields.length > 0) {
    props.searchFields.forEach((field) => {
      params[field.prop] = field.defaultValue !== undefined ? field.defaultValue : undefined;
    });
  }
  return params;
};

// 查询参数
const queryParams = buildInitialParams();

/** 清洗参数：移除 undefined / null / 空字符串 */
const cleanParams = (params) => {
  const result = { ...params };
  Object.keys(result).forEach((key) => {
    const val = result[key];
    if (val === undefined || val === null || val === '') {
      delete result[key];
    }
  });
  return result;
};

/** 请求列表数据 */
const fetchData = () => {
  loading.value = true;
  const params = cleanParams({ ...props.extraParams, ...queryParams });
  props
    .fetchApi(params)
    .then((response) => {
      let rows, totalVal;
      if (props.parseResponse) {
        const result = props.parseResponse(response);
        rows = result.rows;
        totalVal = result.total;
      } else {
        const data = response.data || response;
        rows = data.rows || [];
        totalVal = data.total || 0;
      }
      tableData.value = rows;
      total.value = totalVal;
      loading.value = false;
      emit('data-loaded', { rows, total: totalVal });
    })
    .catch((err) => {
      console.error('ProTable 查询列表失败：', err);
      loading.value = false;
    });
};

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  fetchData();
};

/** 重置搜索 */
const handleResetQuery = () => {
  queryFormRef.value && queryFormRef.value.resetFields();
  props.searchFields.forEach((field) => {
    queryParams[field.prop] = field.defaultValue !== undefined ? field.defaultValue : undefined;
  });
  handleQuery();
};

/** 静默请求列表数据（不显示 loading） */
const fetchDataSilently = () => {
  const params = cleanParams({ ...props.extraParams, ...queryParams });
  props
    .fetchApi(params)
    .then((response) => {
      let rows, totalVal;
      if (props.parseResponse) {
        const result = props.parseResponse(response);
        rows = result.rows;
        totalVal = result.total;
      } else {
        const data = response.data || response;
        rows = data.rows || [];
        totalVal = data.total || 0;
      }
      tableData.value = rows;
      total.value = totalVal;
      emit('data-loaded', { rows, total: totalVal });
    })
    .catch((err) => {
      console.error('ProTable 静默刷新失败：', err);
    });
};

/** 启动静默刷新定时器 */
const startSilentRefresh = () => {
  stopSilentRefresh();
  silentRefreshTimer = setInterval(() => {
    fetchDataSilently();
  }, props.silentRefreshInterval);
};

/** 停止静默刷新定时器 */
const stopSilentRefresh = () => {
  if (silentRefreshTimer) {
    clearInterval(silentRefreshTimer);
    silentRefreshTimer = null;
  }
};

/** 刷新列表（供父组件调用） */
const refresh = () => {
  fetchData();
};

/** 获取当前查询参数（供父组件调用） */
const getQueryParams = () => {
  return cleanParams({ ...props.extraParams, ...queryParams });
};

// 监听 silentRefresh 变化
watch(
  () => props.silentRefresh,
  (val) => {
    if (val) {
      startSilentRefresh();
    } else {
      stopSilentRefresh();
    }
  },
);

// 监听轮询间隔变化
watch(
  () => props.silentRefreshInterval,
  () => {
    if (props.silentRefresh) {
      stopSilentRefresh();
      startSilentRefresh();
    }
  },
);

onMounted(() => {
  fetchData();
  if (props.silentRefresh) {
    startSilentRefresh();
  }
});

onBeforeUnmount(() => {
  stopSilentRefresh();
});

// 暴露方法给父组件
defineExpose({ refresh, getQueryParams, queryParams, tableData });
</script>
