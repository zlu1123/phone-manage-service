<template>
  <div class="app-container">
    <pro-table
      ref="proTableRef"
      :fetch-api="fetchOrderList"
      :search-fields="searchFields"
      :columns="columns"
      :extra-params="extraParams"
      row-key="id"
      @expand-change="handleExpandChange"
      @selection-change="handleSelectionChange"
    >
      <!-- 工具栏：导出/导入按钮 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-tooltip content="导出时会按照当前筛选条件导出数据" placement="top" :open-delay="500">
            <el-button
              type="warning"
              plain
              :icon="Download"
              size="small"
              @click="handleExport"
              :loading="exportLoading"
              v-hasRole="['admin', 'user']"
              >导出</el-button
            >
          </el-tooltip>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            :icon="Upload"
            size="small"
            @click="handleImport"
            v-hasRole="['admin', 'user']"
            >导入</el-button
          >
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            :icon="Delete"
            size="small"
            :disabled="selectedIds.length === 0"
            @click="handleMarkTestData"
            v-hasRole="['admin']"
            >标记测试数据</el-button
          >
        </el-col>
      </template>

      <!-- 表格顶部说明：旧手机质保状态计算方案 -->
      <template #table-tip>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 10px">
          <template #title>
            <span style="font-size: 13px">
              旧手机质保状态计算规则：<strong>未激活</strong> → 显示「未激活」； <strong>已激活但无保修到期时间</strong> →
              「已激活」（视为未过期）； <strong>保修到期时间 ≤ 查询时间</strong> → 「已过保」；
              <strong>保修到期时间 &gt; 查询时间</strong> → 「保修中」
            </span>
          </template>
        </el-alert>
      </template>

      <!-- 自定义列：订单类型 -->
      <template #skipApiCall="{ row }">
        <el-tag v-if="row.skipApiCall === 1 && row.oldPhoneStatus === 0" type="danger" size="small"
          >无旧手机新手机签约</el-tag
        >
        <el-tag v-else-if="row.oldPhoneStatus === 0" type="info" size="small">无旧手机</el-tag>
        <el-tag v-else-if="row.skipApiCall === 1 && row.oldPhoneStatus === 1" type="danger" size="small"
          >旧手机丢失/遗失新签约</el-tag
        >
        <el-tag v-else-if="row.oldPhoneStatus === 1" type="warning" size="small">旧手机丢失/遗失去亚丁</el-tag>
        <el-tag v-else type="success" size="small">旧手机识别</el-tag>
      </template>

      <!-- 自定义列：渠道（参考小程序：在保→亚丁，过保→自有；无旧手机→自有；≤24月→亚丁，>24月→自有） -->
      <template #channel="{ row }">
        <el-tag :type="getChannel(row) === '自有' ? '' : 'warning'" size="small">
          {{ getChannel(row) }}
        </el-tag>
      </template>

      <!-- 展开行：旧手机详情 + 签约信息 -->
      <template #expand="{ row }">
        <div class="expand-content" v-loading="detailLoadingMap[row.id]">
          <!-- 加载中占位 -->
          <div v-if="detailLoadingMap[row.id]" class="expand-loading-tip">正在加载详情...</div>
          <template v-else>
          <div class="expand-section">
            <h4 class="expand-title">旧手机详情</h4>
            <el-descriptions :column="3" size="small" border>
              <el-descriptions-item label="手机品牌">{{ getPhoneTypeName(row.phoneType) }}</el-descriptions-item>
              <el-descriptions-item label="手机型号">{{ row.model || '-' }}</el-descriptions-item>
              <el-descriptions-item label="序列号">{{ row.sn || '-' }}</el-descriptions-item>
              <el-descriptions-item label="IMEI1">{{ row.imei1 || '-' }}</el-descriptions-item>
              <el-descriptions-item label="IMEI2">{{ row.imei2 || '-' }}</el-descriptions-item>
              <el-descriptions-item label="旧手机使用月数">
                <span v-if="row.oldPhoneUsageMonths === 0">小于24个月</span>
                <span v-else-if="row.oldPhoneUsageMonths === 24">大于24个月</span>
                <span v-else-if="row.oldPhoneUsageMonths">&gt; {{ row.oldPhoneUsageMonths }}</span>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="图片">
                <el-button
                  v-if="row.imagePath"
                  size="small"
                  type="primary"
                  link
                  :icon="Picture"
                  @click="handleViewImage(row.imagePath)"
                  >查看</el-button
                >
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="激活日期">{{ formatDate(row.activateDate) }}</el-descriptions-item>
              <el-descriptions-item label="鸭宝查询保修到期时间">{{ formatDate(row.coverage) }}</el-descriptions-item>
              <el-descriptions-item label="系统时间">{{ row.sysTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="鸭宝激活状态">
                <el-tag :type="row.activated ? 'success' : 'info'" size="small">
                  {{ row.activated ? '已激活' : '--' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="旧手机质保状态">
                <el-tag :type="getWarrantyTagType(row)" size="small">
                  {{ getWarrantyInfo(row).status }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="expand-section">
            <h4 class="expand-title">签约信息</h4>
            <el-descriptions :column="3" size="small" border>
              <el-descriptions-item label="店员名称">{{ row.nickName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="签名型号">{{ row.signatureModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="签名IMEI">{{ row.signatureImei || '-' }}</el-descriptions-item>
              <el-descriptions-item label="签名日期">{{ row.signatureDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="用户协议">
                <el-button
                  v-if="row.contractId || row.contractContent"
                  size="small"
                  type="primary"
                  link
                  :icon="View"
                  @click="handleViewContract(row)"
                  >查看</el-button
                >
                <el-link
                  v-else-if="row.contractPath"
                  type="primary"
                  :href="baseApi + row.contractPath"
                  target="_blank"
                  :underline="false"
                  >查看</el-link
                >
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="用户签名">
                <el-button
                  v-if="row.signaturePath"
                  size="small"
                  type="primary"
                  link
                  :icon="Picture"
                  @click="handleViewImage(row.signaturePath)"
                  >查看</el-button
                >
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          </template>
        </div>
      </template>

      <!-- 自定义列：创建时间 -->
      <template #createTime="{ row }">
        <span>{{ parseTime(row.createTime) }}</span>
      </template>
    </pro-table>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="80%"
      :close-on-click-modal="true"
      append-to-body
      @close="handleClosePreview"
    >
      <div style="text-align: center">
        <img :src="previewUrlList[0]" style="max-width: 100%; max-height: 70vh; object-fit: contain" />
      </div>
    </el-dialog>

    <!-- 协议内容预览抽屉 -->
    <el-drawer :title="drawerTitle" v-model="drawerVisible" direction="rtl" size="50%" append-to-body>
      <div class="drawer-toolbar">
        <el-button type="primary" size="small" :icon="Printer" @click="handlePrint">打印协议</el-button>
      </div>
      <Watermark :text="drawerWatermarkText">
        <div id="printArea" ref="printAreaRef" class="contract-content-wrapper">
          <div v-html="drawerContent"></div>
        </div>
      </Watermark>
    </el-drawer>

    <!-- 订单导入对话框 -->
    <el-dialog :title="upload.title" v-model="upload.open" width="450px" append-to-body :before-close="handleUploadDialogClose">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          <ol style="margin: 0; padding-left: 18px; font-size: 13px; line-height: 1.8">
            <li>
              <strong>跳过API</strong>：填<strong>自有</strong>或<strong>亚丁</strong>（也兼容旧模板的 1=自有、0=亚丁）；旧手机信息（序列号、照片等）可不填
            </li>
            <li>
              <strong>所属门店</strong>填门店名称即可，系统自动匹配（也可填门店ID，留空则归属当前账号所属门店）
            </li>
            <li>先进入<a href="javascript:void(0)" style="color: #409eff; text-decoration: underline" @click="goToLeaveInfo">「留资用户」</a>页面导入留资人（姓名 + 电话）；留资库中不存在时，填写「留资人电话 + 留资人姓名」系统会自动创建</li>
            <li>下载模板，按格式填写订单数据，上传后系统自动根据<strong>留资人电话</strong>匹配留资记录</li>
          </ol>
        </template>
      </el-alert>
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xls,.xlsx"
        :headers="upload.headers"
        :action="upload.url"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :on-error="handleFileError"
        :auto-upload="false"
        drag
        @change="handleFileChange"
      >
        <el-icon :size="67" color="#C0C4CC"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <br />
          <span style="color: #e6a23c">
            必填列：创建时间（yyyy-MM-dd）、跳过API（填 自有/亚丁）；签名日期未填时默认取创建时间
          </span>
          <br />
          <span style="color: #909399; font-size: 12px">
            跳过API：自有=自有渠道，亚丁=亚丁渠道（旧模板的 1/0 仍可填写）；所属门店填门店名称即可，系统自动匹配
          </span>
          <br />
          <span style="color: #909399; font-size: 12px">
            留资人电话用于匹配已有留资记录，匹配不到且填写了留资人姓名时系统自动创建留资记录
          </span>
          <br />
          <el-link
            type="primary"
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            @click="importTemplate"
            >下载模板（含填写说明和示例数据）</el-link
          >
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="upload.isUploading" :disabled="upload.isUploading" @click="submitFileForm"
            >确 定</el-button
          >
          <el-button
            :disabled="upload.isUploading"
            @click="
              upload.open = false;
              selectedFile = null;
              uploadRef.value?.clearFiles();
            "
            >取 消</el-button
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Download, Upload, Picture, View, Printer, UploadFilled, Delete } from '@element-plus/icons-vue';
import { queryOrderList, queryPhoneTypeList, getOrderContractContent, getOrderDetail, exportOrderList, markTestData } from '@/api/order/list';
import { getStoreList } from '@/api/dashboard';
import { getToken } from '@/utils/auth';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';
import useUserStore from '@/store/modules/user';

const { proxy } = getCurrentInstance();
const route = useRoute();

// 图片资源前缀地址
const baseApi = import.meta.env.VITE_APP_BASE_API;

// 导出loading
const exportLoading = ref(false);

// 导入参数
const upload = reactive({
  open: false,
  title: '',
  isUploading: false,
  headers: { Authorization: 'Bearer ' + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + '/system/order/importData',
});

// 手机品牌列表
const phoneTypeList = ref([]);

// 额外参数
const extraParams = ref({});

// 抽屉相关
const drawerVisible = ref(false);
const drawerTitle = ref('');
const drawerContent = ref('');
const currentRow = ref(null);
const printAreaRef = ref(null);

// 图片预览器
const previewVisible = ref(false);
const previewUrlList = ref([]);

// 展开行详情加载状态（key: row.id, value: true=loading）
const detailLoadingMap = reactive({});

// 展开行详情已加载标记（key: row.id, value: true=已加载）
const detailLoadedMap = reactive({});

/** 处理展开行：按需加载详情数据 */
const handleExpandChange = (row, expanded) => {
  if (!expanded) return;
  // 已加载过详情或正在加载中，则跳过
  if (detailLoadingMap[row.id] || detailLoadedMap[row.id]) return;
  detailLoadingMap[row.id] = true;
  getOrderDetail(row.id)
    .then((res) => {
      const detail = res.data || res;
      // 将详情字段合并到 row 中，展开面板即可自动展示
      Object.assign(row, detail);
      detailLoadedMap[row.id] = true;
    })
    .catch((err) => {
      console.error('加载订单详情失败：', err);
    })
    .finally(() => {
      detailLoadingMap[row.id] = false;
    });
};

/** 抽屉水印文本 */
const drawerWatermarkText = computed(() => {
  const row = currentRow.value;
  if (!row) return '';
  return row.nickName || row.createBy || '用户';
});

// 表格引用
const proTableRef = ref(null);
const uploadRef = ref(null);
const selectedFile = ref(null);

// 门店列表（所属门店下拉数据源）
const storeOptions = ref([]);

// 搜索字段配置
const searchFields = computed(() => [
  {
    prop: 'orderSource',
    label: '订单来源',
    type: 'select',
    options: [
      { label: '全部', value: null },
      { label: '旧手机识别', value: 'old_phone' },
      { label: '无旧手机签约', value: 'no_old_phone' },
      { label: '损坏/遗失签约', value: 'damaged' },
    ],
  },
  { prop: 'createBy', label: '创建者', type: 'input' },
  { prop: 'nickName', label: '店员名称', type: 'input' },
  {
    prop: 'storeId',
    label: '所属门店',
    type: 'select',
    options: [{ label: '全部', value: null }, ...storeOptions.value],
  },
  { prop: 'name', label: '留资人姓名', type: 'input' },
  { prop: 'phoneNum', label: '留资人电话', type: 'input' },
]);

/** 获取门店列表（所属门店下拉数据源） */
const fetchStoreList = () => {
  getStoreList()
    .then((res) => {
      storeOptions.value = (res.data || []).map((store) => ({
        label: store.storeName,
        value: store.deptId,
      }));
    })
    .catch((err) => {
      console.error('获取门店列表失败：', err);
    });
};

/** 将前端「订单来源」映射为后端 skipApiCall + oldPhoneStatus 参数 */
const mapOrderSource = (params) => {
  const mapped = { ...params };
  const source = mapped.orderSource;
  delete mapped.orderSource;

  if (source === 'old_phone') {
    mapped.skipApiCall = 0;
  } else if (source === 'no_old_phone') {
    mapped.skipApiCall = 1;
    mapped.oldPhoneStatus = 0;
  } else if (source === 'damaged') {
    mapped.skipApiCall = 1;
    mapped.oldPhoneStatus = 1;
  }
  return mapped;
};

/**
 * 判断渠道：参考 phone-little-app 小程序逻辑
 *   skipApiCall=0（OCR识别）：coverage > sysTime（在保）→ 亚丁，否则 → 自有
 *   skipApiCall=1 + oldPhoneStatus=0（无旧手机）→ 自有
 *   skipApiCall=1 + oldPhoneStatus=1（丢失/损坏）：
 *     oldPhoneUsageMonths=0（≤24个月）→ 亚丁，否则 → 自有
 */
const getChannel = (row) => {
  // Excel 导入的订单直接指定了渠道，优先使用；历史订单按下方规则推导
  if (row.channel) return row.channel;
  if (row.skipApiCall === 0) {
    // OCR 路径：根据保修到期时间 vs 查询时系统时间判断
    const coverage = (row.coverage || '').substring(0, 10);
    const sysTime = (row.sysTime || '').substring(0, 10);
    if (!coverage || !sysTime) return '自有'; // 无保期数据视为已过保
    return coverage > sysTime ? '亚丁' : '自有';
  }
  // skipApiCall === 1
  if (row.oldPhoneStatus === 0) return '自有';       // 无旧手机
  return row.oldPhoneUsageMonths === 0 ? '亚丁' : '自有'; // 丢失/损坏：≤24月 → 亚丁
};

/** 列表查询：映射 orderSource 后调 queryOrderList，渠道字段已由后端列表 SQL 直接返回 */
const fetchOrderList = (params) => {
  return queryOrderList(mapOrderSource(params));
};

// 表格列配置
const columns = [
  { label: '', slot: 'expand', width: '50' },
  { type: 'selection', width: '50' },
  { label: 'ID', prop: 'id', width: '50' },
  {
    label: '订单类型',
    prop: 'skipApiCall',
    slot: 'skipApiCall',
    width: '150',
  },
  {
    label: '渠道',
    prop: 'channel',
    slot: 'channel',
    width: '80',
  },
  { label: '留资人姓名', prop: 'name', width: '100' },
  { label: '留资人电话', prop: 'phoneNum' },
  { label: '店员名称', prop: 'nickName' },
  { label: '所属门店', prop: 'storeName', width: '130' },
  {
    label: '创建时间',
    prop: 'createTime',
    slot: 'createTime',
    width: '180',
  },
];

/** 获取手机品牌列表（展开面板展示品牌名称用） */
const getPhoneTypeList = () => {
  queryPhoneTypeList()
    .then((response) => {
      phoneTypeList.value = response.data || [];
    })
    .catch((err) => {
      console.error('获取手机品牌列表失败：', err);
    });
};

/** 根据品牌code获取品牌名称 */
const getPhoneTypeName = (code) => {
  if (!code) return '-';
  const item = phoneTypeList.value.find((t) => t.code === code);
  return item ? item.name : code;
};

/** 查看图片 */
const handleViewImage = (relativePath) => {
  if (!relativePath) return;
  previewUrlList.value = [baseApi + relativePath];
  previewVisible.value = true;
};

/** 关闭预览器 */
const handleClosePreview = () => {
  previewVisible.value = false;
  previewUrlList.value = [];
};

/** 格式化日期 */
const formatDate = (value) => {
  if (!value && value !== 0) return '-';
  let date;
  if (typeof value === 'number') {
    date = new Date(value);
  } else if (typeof value === 'string') {
    const normalized = value.replace(/\//g, '-').trim();
    date = new Date(normalized);
    if (isNaN(date.getTime())) {
      date = new Date(value);
    }
  } else {
    date = new Date(value);
  }
  if (!date || isNaN(date.getTime())) {
    return String(value);
  }
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

/** 计算旧手机质保状态 */
const getWarrantyInfo = (row) => {
  if (!row) {
    return { status: '--', expired: false };
  }
  // 鸭宝没有返回激活状态
  if (row.activated === undefined || row.activated === null) {
    return { status: '--', expired: false };
  }
  if (!row.activated) {
    return { status: '未激活', expired: true };
  }
  const coverage = row.coverage;
  if (!coverage) {
    return { status: '已激活', expired: false };
  }
  if (!row.sysTime) {
    return { status: '已激活', expired: false };
  }
  const normalize = (v) => (typeof v === 'string' ? v.replace(/\//g, '-') : v);
  const coverageTime = new Date(normalize(coverage)).getTime();
  const sysTime = new Date(normalize(row.sysTime)).getTime();
  if (isNaN(coverageTime) || isNaN(sysTime)) {
    return { status: '已激活', expired: false };
  }
  const expired = coverageTime <= sysTime;
  return { status: expired ? '已过保' : '保修中', expired };
};

/** 旧手机质保状态 tag 类型 */
const getWarrantyTagType = (row) => {
  const info = getWarrantyInfo(row);
  if (info.expired) return 'danger';
  return info.status === '保修中' ? 'success' : 'info';
};

/** 查看协议内容 */
const handleViewContract = (row) => {
  drawerTitle.value = '用户协议内容';
  currentRow.value = row;

  if (row.contractId) {
    getOrderContractContent(row.id)
      .then((res) => {
        const content = res.data;
        if (!content) {
          ElMessage.warning('协议内容不存在');
          return;
        }
        renderContractContent(content);
        drawerVisible.value = true;
      })
      .catch(() => {
        ElMessage.error('获取协议内容失败，请稍后重试');
      });
    return;
  }

  if (row.contractContent) {
    renderContractContent(row.contractContent);
    drawerVisible.value = true;
    return;
  }

  if (row.contractPath) {
    window.open(baseApi + row.contractPath, '_blank');
    return;
  }

  ElMessage.warning('暂无协议内容');
};

/** 渲染协议内容 */
const renderContractContent = (content) => {
  const row = currentRow.value;

  const injectAfterKeyword = (html, keywordPattern, valueHtml) => {
    if (!valueHtml) return html;
    const regexWithU = new RegExp(
      `(${keywordPattern})((?:\\s|&nbsp;|</span>|<span[^>]*>)*)<u\\b[^>]*>[\\s\\S]*?</u>`,
      'i',
    );
    if (regexWithU.test(html)) {
      return html.replace(regexWithU, `$1$2${valueHtml}`);
    }
    const regexEmpty = new RegExp(`(${keywordPattern})((?:\\s|&nbsp;)*)(?=<|$)`, 'i');
    const match = html.match(regexEmpty);
    if (match) {
      return html.replace(regexEmpty, `$1$2${valueHtml}`);
    }
    return html;
  };

  if (row.signatureModel) {
    content = injectAfterKeyword(
      content,
      '设备型号[：:]',
      `<span style="text-decoration:underline;padding:0 4px;">${row.signatureModel}</span>`,
    );
  }

  if (row.signatureImei) {
    content = injectAfterKeyword(
      content,
      '设备IME[I]?[：:]',
      `<span style="text-decoration:underline;padding:0 4px;">${row.signatureImei}</span>`,
    );
  }

  if (row.signaturePath) {
    content = injectAfterKeyword(
      content,
      '签字确认[：:]',
      `<img src="${baseApi + row.signaturePath}" style="max-width:200px;max-height:80px;vertical-align:middle;" />`,
    );
  }

  if (row.signatureDate) {
    content = injectAfterKeyword(
      content,
      '日(?:\\s|&nbsp;)*期(?:\\s|&nbsp;)*[：:]',
      `<span style="text-decoration:underline;padding:0 4px;">${row.signatureDate}</span>`,
    );
  }

  drawerContent.value = content;
};

/** 生成水印 SVG CSS（用于打印窗口） */
const generateWatermarkCss = (text) => {
  if (!text) return '';
  const w = 180;
  const h = 126;
  const cx = 90;
  const cy = 63;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">
    <text x="${cx}" y="${cy}" transform="rotate(-22,${cx},${cy})" font-size="14" fill="rgba(180,180,180,0.2)" font-family="Microsoft YaHei, PingFang SC, sans-serif" text-anchor="middle" dominant-baseline="middle">${text}</text>
  </svg>`;
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
};

/** 打印协议 */
const handlePrint = () => {
  const contentDiv = printAreaRef.value.querySelector('div');
  const printContent = contentDiv ? contentDiv.innerHTML : printAreaRef.value.innerHTML;
  const userStore = useUserStore();
  const currentUserName = userStore.nickName || userStore.name || '用户';
  const watermarkBg = generateWatermarkCss(currentUserName);
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    ElMessage.warning('请允许弹出窗口后重试');
    return;
  }
  let printed = false;
  const doPrint = () => {
    if (printed || printWindow.closed) return;
    printed = true;
    printWindow.print();
  };

  const htmlContent = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>用户协议</title><style>
    body { padding: 40px; font-family: "Microsoft YaHei", "PingFang SC", sans-serif; line-height: 1.8; font-size: 14px; color: #333; position: relative; }
    .watermark { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-image: ${watermarkBg}; background-repeat: repeat; pointer-events: none; z-index: 9999; }
    h2, h3 { margin: 16px 0 8px; color: #1a1a1a; }
    p { margin: 8px 0; }
    img { max-width: 200px; max-height: 80px; vertical-align: middle; }
    table { border-collapse: collapse; width: 100%; margin: 16px 0; }
    table th, table td { border: 1px solid #ccc; padding: 8px 12px; text-align: left; }
    table th { background-color: #f5f5f5; font-weight: bold; }
    @media print { body { padding: 20px; } .watermark { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-image: ${watermarkBg}; background-repeat: repeat; pointer-events: none; z-index: 9999; -webkit-print-color-adjust: exact; print-color-adjust: exact; } }
  </style></head><body><div class="watermark"></div><div class="content">${printContent}</div></body></html>`;
  printWindow.document.open();
  printWindow.document.write(htmlContent);
  printWindow.document.close();
  printWindow.onload = function () {
    setTimeout(doPrint, 300);
  };
  // 兜底：3 秒后如果 onload 未触发则补打印
  setTimeout(() => {
    if (!printed && !printWindow.closed) {
      doPrint();
    }
  }, 3000);
};

/** 导出列配置 */
const exportColumns = [
  { label: 'ID', prop: 'id', width: 6 },
  {
    label: '订单类型',
    formatter: (row) => {
      if (row.skipApiCall === 1 && row.oldPhoneStatus === 0) return '无旧手机新手机签约';
      if (row.oldPhoneStatus === 0) return '无旧手机';
      if (row.skipApiCall === 1 && row.oldPhoneStatus === 1) return '旧手机丢失/遗失新签约';
      if (row.oldPhoneStatus === 1) return '旧手机丢失/遗失去亚丁';
      return '旧手机识别';
    },
    width: 10,
  },
  {
    label: '渠道',
    formatter: (row) => getChannel(row),
    width: 8,
  },
  {
    label: '旧手机使用月数',
    prop: 'oldPhoneUsageMonths',
    width: 12,
    formatter: (row) => {
      if (row.oldPhoneUsageMonths === 0) return '小于24个月';
      if (row.oldPhoneUsageMonths === 24) return '大于24个月';
      if (row.oldPhoneUsageMonths) return '> ' + row.oldPhoneUsageMonths;
      return '-';
    },
  },
  { label: '旧手机品牌', prop: 'phoneType', formatter: (row) => getPhoneTypeName(row.phoneType), width: 12 },
  { label: '旧手机型号', prop: 'model' },
  { label: '旧手机序列号', prop: 'sn', width: 18 },
  { label: '旧手机IMEI1', prop: 'imei1', width: 18 },
  { label: '旧手机IMEI2', prop: 'imei2', width: 18 },
  { label: '旧手机激活状态', prop: 'activated', formatter: (row) => (row.activated ? '已激活' : '--') },
  { label: '旧手机激活日期', prop: 'activateDate', formatter: (row) => formatDate(row.activateDate) },
  { label: '鸭宝查询保修到期时间', prop: 'coverage', formatter: (row) => formatDate(row.coverage) },
  { label: '旧手机质保状态', formatter: (row) => getWarrantyInfo(row).status },
  { label: '查询时系统时间', prop: 'sysTime' },
  { label: '店员名称', prop: 'nickName' },
  { label: '留资人姓名', prop: 'name' },
  { label: '留资人电话', prop: 'phoneNum' },
  {
    label: '用户协议',
    prop: 'contractPath',
    formatter: (row) => (row.contractPath ? baseApi + row.contractPath : '-'),
    width: 40,
  },
  {
    label: '用户签名',
    prop: 'signaturePath',
    formatter: (row) => (row.signaturePath ? baseApi + row.signaturePath : '-'),
    width: 40,
  },
  { label: '签名型号', prop: 'signatureModel' },
  { label: '签名IMEI', prop: 'signatureImei', width: 18 },
  { label: '签名日期', prop: 'signatureDate' },
  { label: '创建时间', prop: 'createTime', width: 20 },
];

/** 生成 Excel 并下载 */
const generateExcel = (rows, fileName) => {
  if (!rows || rows.length === 0) {
    ElMessage.warning('没有可导出的数据');
    return;
  }
  const exportData = rows.map((row) => {
    const mapped = {};
    exportColumns.forEach((col) => {
      let val;
      if (typeof col.formatter === 'function') {
        val = col.formatter(row);
      } else if (col.prop) {
        val = row[col.prop];
        if (val === undefined || val === null) val = '-';
      } else {
        val = '-';
      }
      mapped[col.label] = val;
    });
    return mapped;
  });

  const ws = XLSX.utils.json_to_sheet(exportData);
  ws['!cols'] = exportColumns.map((col) => ({ wch: col.width || 14 }));
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, '订单数据');
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
  saveAs(new Blob([wbout], { type: 'application/octet-stream' }), `${fileName}_${new Date().getTime()}.xlsx`);
};

/** 导出 */
const handleExport = () => {
  ElMessageBox.confirm('请选择导出范围', '导出订单数据', {
    confirmButtonText: '导出全部数据',
    cancelButtonText: '导出当前页',
    distinguishCancelAndClose: true,
    type: 'info',
  })
    .then(() => {
      // 导出全部数据
      exportLoading.value = true;
      const queryParams = proTableRef.value.getQueryParams();
      exportOrderList(mapOrderSource(queryParams))
        .then((res) => {
          const rows = res.data || res || [];
          generateExcel(rows, '订单数据');
          ElMessage.success(`导出成功，共 ${rows.length} 条数据`);
        })
        .catch((err) => {
          console.error('导出订单数据失败：', err);
          ElMessage.error('导出失败，请稍后重试');
        })
        .finally(() => {
          exportLoading.value = false;
        });
    })
    .catch((action) => {
      if (action === 'cancel') {
        // 导出当前页
        const rows = proTableRef.value.tableData || [];
        generateExcel(rows, '订单数据_当前页');
        if (rows.length > 0) {
          ElMessage.success(`导出成功，共 ${rows.length} 条数据`);
        }
      }
      // dismiss（点叉关闭）：不操作
    });
};

/** 导入 */
const handleImport = () => {
  upload.title = '订单导入';
  upload.open = true;
  selectedFile.value = null;
  upload.isUploading = false;
};

/** 勾选的订单ID（标记测试数据用） */
const selectedIds = ref([]);

/** 表格多选变化 */
const handleSelectionChange = (rows) => {
  selectedIds.value = (rows || []).map((r) => r.id);
};

/** 标记测试数据：逻辑删除（列表/导出/统计不再展示），支持单条/批量，仅管理员 */
const handleMarkTestData = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选要标记的订单');
    return;
  }
  ElMessageBox.confirm(
    `确定将选中的 ${selectedIds.value.length} 条订单标记为测试数据吗？标记后列表将不再展示（数据库中仅逻辑删除，数据仍保留）。`,
    '标记测试数据',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
  )
    .then(() => {
      markTestData(selectedIds.value)
        .then((res) => {
          const count = res && res.data != null ? res.data : selectedIds.value.length;
          ElMessage.success(`已标记 ${count} 条测试数据`);
          selectedIds.value = [];
          proTableRef.value.queryParams.pageNum = 1;
          proTableRef.value.refresh();
        })
        .catch(() => {
          ElMessage.error('标记失败，请稍后重试');
        });
    })
    .catch(() => {
      // 用户取消，不操作
    });
};

/** 跳转到留资用户页面 */
const goToLeaveInfo = () => {
  upload.open = false;
  const router = useRouter();
  router.push('/info/user');
};

/** 导入模板列定义（列名必须匹配后端 @Excel 注解的 name 值） */
const importTemplateHeaders = [
  '创建时间',
  '跳过API',
  '旧手机状态',
  '旧手机序列号',
  '旧手机品牌',
  '旧手机型号',
  '旧手机IMEI1',
  '旧手机IMEI2',
  '旧手机使用月数',
  '店员名称',
  '所属门店',
  '留资人电话',
  '留资人姓名',
  '鸭宝激活状态',
  '旧手机激活日期',
  '旧手机保修到期时间',
  '查询时系统时间',
  '签名型号',
  '签名IMEI',
  '签名日期',
];

/** 导入模板示例行（含两个场景示例：亚丁渠道 / 自有渠道） */
const importTemplateExample1 = [
  '2025-03-08',
  '亚丁',
  '',
  '',
  '',
  '',
  '',
  '',
  '',
  '张向玉',
  '示例门店',
  '13800000000',
  '张三',
  '',
  '',
  '',
  '',
  'iPhone 16ProMax 512G',
  'SFXWPK9HLQ7',
  '',
];

const importTemplateExample2 = [
  '2025-03-08',
  '自有',
  '',
  '',
  '',
  '',
  '',
  '',
  '',
  '张浩',
  '',
  '18189201567',
  '张浩',
  '',
  '',
  '',
  '',
  '平板3',
  '12312312312312312',
  '2025-03-08',
];

/** 模板「填写说明」sheet 内容（放在第二个sheet，不影响后端按第一个sheet解析） */
const importTemplateHelpRows = [
  ['字段名', '是否必填', '填写说明'],
  ['创建时间', '必填', '订单创建时间，格式：yyyy-MM-dd（只填年月日即可）'],
  ['跳过API', '必填', '订单渠道：填 自有 或 亚丁（也兼容旧模板的 1=自有、0=亚丁）'],
  ['旧手机状态', '选填', '旧手机信息（照片/序列号）已不存在，可留空；如填写：0=无旧手机，1=丢失/损坏'],
  ['旧手机序列号', '选填', '旧手机序列号（sn），无旧手机信息时留空'],
  ['旧手机品牌', '选填', '旧手机品牌名称'],
  ['旧手机型号', '选填', '旧手机型号'],
  ['旧手机IMEI1', '选填', '旧手机IMEI1号'],
  ['旧手机IMEI2', '选填', '旧手机IMEI2号'],
  ['旧手机使用月数', '选填', '旧手机已使用月数，正整数'],
  ['店员名称', '选填', '店员姓名（创建该订单的店员）'],
  ['所属门店', '选填', '填写门店名称即可，系统自动匹配；也可填门店ID；留空则归属当前操作人所属门店'],
  ['留资人电话', '选填', '用于匹配留资库中的留资记录；匹配不到且填写了留资人姓名时，系统自动创建留资记录'],
  ['留资人姓名', '选填', '留资人姓名（配合留资人电话使用）'],
  ['鸭宝激活状态', '选填', '可填：已激活/未激活（或 true/false、1/0）'],
  ['旧手机激活日期', '选填', '旧手机激活日期，格式：yyyy-MM-dd'],
  ['旧手机保修到期时间', '选填', '保修到期时间，格式：yyyy-MM-dd'],
  ['查询时系统时间', '选填', '查询时的系统时间'],
  ['签名型号', '选填', '签约手机型号'],
  ['签名IMEI', '选填', '签约手机IMEI'],
  ['签名日期', '选填', '签约日期，格式：yyyy-MM-dd；未填写时默认取「创建时间」'],
];

/** 下载模板（前端生成，含两个场景示例数据 + 填写说明） */
const importTemplate = () => {
  // 所属门店示例使用系统真实门店：示例1填门店名称、示例2填门店ID（门店列表未加载到时退回占位文案）
  const firstStore = storeOptions.value[0];
  const example1 = [...importTemplateExample1];
  const example2 = [...importTemplateExample2];
  example1[10] = firstStore?.label || '示例门店';
  example2[10] = firstStore?.value != null ? String(firstStore.value) : '';
  const sheetData = [importTemplateHeaders, example1, example2];
  const ws = XLSX.utils.aoa_to_sheet(sheetData);
  ws['!cols'] = [
    { wch: 22 }, { wch: 10 }, { wch: 12 }, { wch: 18 },
    { wch: 14 }, { wch: 16 }, { wch: 18 }, { wch: 18 },
    { wch: 16 }, { wch: 12 }, { wch: 10 },
    { wch: 16 }, { wch: 12 },
    { wch: 14 }, { wch: 16 }, { wch: 18 }, { wch: 18 },
    { wch: 14 }, { wch: 22 }, { wch: 14 },
  ];
  // 填写说明单独一个sheet，置于模板之后，避免影响后端按第一个sheet解析导入
  const helpWs = XLSX.utils.aoa_to_sheet(importTemplateHelpRows);
  helpWs['!cols'] = [{ wch: 18 }, { wch: 10 }, { wch: 80 }];
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, '订单导入模板');
  XLSX.utils.book_append_sheet(wb, helpWs, '填写说明');
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
  saveAs(new Blob([wbout], { type: 'application/octet-stream' }), '订单导入模板.xlsx');
};

/** 文件上传进度（仅用于锁定按钮和展示loading） */
const handleFileUploadProgress = () => {
  upload.isUploading = true;
};

/** 上传失败（网络/服务端错误）：恢复按钮状态，否则 loading 会卡死且弹窗无法关闭 */
const handleFileError = () => {
  upload.isUploading = false;
  ElMessage.error('文件上传失败，请稍后重试');
};

/** 导入过程中禁止关闭弹窗（X 按钮、ESC、遮罩点击均会触发此钩子） */
const handleUploadDialogClose = (done) => {
  if (upload.isUploading) {
    ElMessage.warning('数据导入中，请稍候…');
    return;
  }
  done();
};

/** 文件上传成功 */
const handleFileSuccess = (response) => {
  upload.open = false;
  upload.isUploading = false;
  selectedFile.value = null;
  uploadRef.value.clearFiles();
  // 后端返回结构化导入结果：data.message 为结果摘要（含错误/提示明细）
  const result = response && response.data;
  const msg = (result && result.message) || (typeof result === 'string' ? result : '') || (response && response.msg) || '';
  ElMessageBox.alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + msg + '</div>',
    '导入结果',
    { dangerouslyUseHTMLString: true },
  );
  proTableRef.value.queryParams.pageNum = 1;
  proTableRef.value.refresh();
};

/** 文件变化时记录选中文件 */
const handleFileChange = (uploadFile) => {
  selectedFile.value = uploadFile;
};

/** 提交上传 */
const submitFileForm = () => {
  const file = selectedFile.value;
  if (!file) {
    ElMessage.warning('请先选择文件');
    return;
  }
  const fileName = (file.name || '').toLowerCase();
  if (!fileName.endsWith('.xls') && !fileName.endsWith('.xlsx')) {
    ElMessage.warning('请选择后缀为"xls"或"xlsx"的文件。');
    return;
  }
  const raw = file.raw;
  if (!raw) {
    ElMessage.warning('文件读取失败，请重新选择文件');
    return;
  }
  // 本地解析 + 校验 + 上传全程保持按钮 loading，避免解析大文件时界面无反馈
  upload.isUploading = true;
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 });
      if (!rows || rows.length < 2) {
        upload.isUploading = false;
        ElMessage.warning('Excel 文件中没有数据行，请填写至少一行数据');
        return;
      }

      // 解析表头，构建列索引映射
      const headerRow = rows[0];
      const colMap = {}; // { '列名' : 列索引 }
      const requiredHeaders = ['创建时间', '跳过API'];
      headerRow.forEach((h, idx) => {
        const clean = String(h || '').trim();
        if (clean) colMap[clean] = idx;
      });

      // 检查必填列是否存在
      const missingHeaders = requiredHeaders.filter((col) => !(col in colMap));
      if (missingHeaders.length > 0) {
        upload.isUploading = false;
        ElMessage.warning('Excel 缺少必填列：' + missingHeaders.join('、') + '，请使用最新模板');
        return;
      }

      // 逐行校验（规则与后端校验保持一致），全部通过才允许上传
      const errors = [];
      const dateRegex = /^\d{4}-\d{1,2}-\d{1,2}$/;
      const datetimeRegex = /^\d{4}-\d{1,2}-\d{1,2}( \d{1,2}:\d{1,2}(:\d{1,2})?)?$/;

      for (let r = 1; r < rows.length; r++) {
        const row = rows[r];
        // 跳过全空行
        if (!row || row.every((cell) => cell === undefined || String(cell).trim() === '')) {
          continue;
        }
        const rowLabel = `第 ${r + 1} 行`;

        // 辅助取值函数
        const getVal = (colName) => {
          const idx = colMap[colName];
          if (idx === undefined) return '';
          const v = row[idx];
          return v !== undefined && v !== null ? cleanCell(v) : '';
        };
        // 日期列取值：单元格为真正的Excel日期（序列号）时转换为 yyyy-MM-dd 字符串
        const getDateVal = (colName) => {
          const idx = colMap[colName];
          if (idx === undefined) return '';
          const v = row[idx];
          if (v === undefined || v === null || v === '') return '';
          if (typeof v === 'number') {
            try {
              return XLSX.SSF.format('yyyy-mm-dd', v);
            } catch (e) {
              return String(v);
            }
          }
          return cleanCell(v);
        };
        // 去除首尾空白（含 Excel 常见的不间断空格  、全角空格 　），与后端 trim 规则一致
        const cleanCell = (v) => String(v).replace(/^[\s 　]+|[\s 　]+$/g, '');

        // 1. 创建时间必填 + 格式（yyyy-MM-dd，可带时分秒）
        const createTimeVal = getDateVal('创建时间');
        if (!createTimeVal) {
          errors.push(`${rowLabel}「创建时间」不能为空`);
        } else if (!datetimeRegex.test(createTimeVal)) {
          errors.push(`${rowLabel}「创建时间」格式无效（${createTimeVal}），应为 yyyy-MM-dd`);
        }

        // 2. 跳过API 必填 + 值域校验（填 自有/亚丁，兼容旧模板的 0/1）
        const skipApiCallVal = getVal('跳过API');
        if (!skipApiCallVal) {
          errors.push(`${rowLabel}「跳过API」不能为空（填 自有 或 亚丁）`);
        } else if (!['0', '1', '自有', '亚丁'].includes(skipApiCallVal)) {
          errors.push(`${rowLabel}「跳过API」值无效（${skipApiCallVal}），必须为 自有/亚丁（或旧模板的 0/1）`);
        }

        // 旧手机状态：选填（旧手机照片已不存在），填了必须为 0/1
        const oldPhoneStatusVal = getVal('旧手机状态');
        if (oldPhoneStatusVal && oldPhoneStatusVal !== '0' && oldPhoneStatusVal !== '1') {
          errors.push(`${rowLabel}「旧手机状态」值无效（${oldPhoneStatusVal}），必须为 0 或 1`);
        }

        // 3. 旧手机使用月数：选填，填了必须为正整数
        const usageMonthsVal = getVal('旧手机使用月数');
        if (usageMonthsVal && !/^\d+(\.0+)?$/.test(usageMonthsVal)) {
          errors.push(`${rowLabel}「旧手机使用月数」无效（${usageMonthsVal}），必须为正整数`);
        }

        // 4. 鸭宝激活状态：选填，填了必须在枚举范围内
        const activatedVal = getVal('鸭宝激活状态');
        if (activatedVal && !['已激活', '未激活', '是', '否', '1', '0'].includes(activatedVal) && !/^(true|false)$/i.test(activatedVal)) {
          errors.push(`${rowLabel}「鸭宝激活状态」值无效（${activatedVal}），可填写：已激活/未激活（或 true/false、1/0）`);
        }

        // 5. 日期列：选填，填了必须为 yyyy-MM-dd
        ['旧手机激活日期', '旧手机保修到期时间'].forEach((colName) => {
          const v = getDateVal(colName);
          if (v && !dateRegex.test(v)) {
            errors.push(`${rowLabel}「${colName}」格式无效（${v}），应为 yyyy-MM-dd`);
          }
        });

        // 6. 查询时系统时间：选填，填了必须为日期或日期时间
        const sysTimeVal = getDateVal('查询时系统时间');
        if (sysTimeVal && !datetimeRegex.test(sysTimeVal)) {
          errors.push(`${rowLabel}「查询时系统时间」格式无效（${sysTimeVal}），应为 yyyy-MM-dd HH:mm:ss`);
        }

        // 7. 签名日期选填（未填默认取创建时间），填了必须为 yyyy-MM-dd
        const signatureDateVal = getDateVal('签名日期');
        if (signatureDateVal && !dateRegex.test(signatureDateVal)) {
          errors.push(`${rowLabel}「签名日期」格式无效（${signatureDateVal}），应为 yyyy-MM-dd`);
        }
      }

      // 存在校验错误：不上传，弹出全部错误明细
      if (errors.length > 0) {
        upload.isUploading = false;
        ElMessageBox.alert(
          "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
            '文件校验未通过，未导入任何数据，请修改后重新上传：<br/>' +
            errors.join('<br/>') +
            '</div>',
          '导入校验失败',
          { dangerouslyUseHTMLString: true },
        );
        return;
      }

      uploadRef.value.submit();
    } catch (err) {
      console.error('Excel 解析失败：', err);
      upload.isUploading = false;
      ElMessage.warning('Excel 文件解析失败，请检查文件格式');
    }
  };
  reader.readAsArrayBuffer(raw);
};

onMounted(() => {
  getPhoneTypeList();
  fetchStoreList();
});
</script>

<style scoped>
.drawer-toolbar {
  padding: 0 20px 10px;
  border-bottom: 1px solid #ebeef5;
}

.expand-content {
  padding: 12px 20px;
  background: #fafafa;
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.expand-section {
  flex: 1;
  min-width: 360px;
}
.expand-title {
  margin: 0 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #e8e8e8;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.expand-loading-tip {
  padding: 20px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.contract-content-wrapper {
  padding: 20px;
  line-height: 1.8;
  font-size: 14px;
  color: #333;
  overflow-y: auto;
  height: calc(100% - 60px);
  position: relative;
  background-repeat: repeat;
}
.contract-content-wrapper h2,
.contract-content-wrapper h3 {
  margin: 16px 0 8px;
  color: #1a1a1a;
}
.contract-content-wrapper p {
  margin: 8px 0;
}
.contract-content-wrapper :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}
.contract-content-wrapper :deep(table th),
.contract-content-wrapper :deep(table td) {
  border: 1px solid #ccc;
  padding: 8px 12px;
  text-align: left;
}
.contract-content-wrapper :deep(table th) {
  background-color: #f5f5f5;
  font-weight: bold;
}
</style>
