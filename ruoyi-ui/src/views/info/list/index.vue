<template>
  <div class="app-container">
    <pro-table
      ref="proTableRef"
      :fetch-api="queryOrderList"
      :search-fields="searchFields"
      :columns="columns"
      :extra-params="extraParams"
      row-key="id"
      @expand-change="handleExpandChange"
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
      </template>

      <!-- 表格顶部说明：质保状态计算方案 -->
      <template #table-tip>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 10px">
          <template #title>
            <span style="font-size: 13px">
              质保状态计算规则：<strong>未激活</strong> → 显示「未激活」； <strong>已激活但无保修到期时间</strong> →
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

      <!-- 自定义列：渠道 -->
      <template #channel="{ row }">
        <el-tag :type="row.skipApiCall === 1 ? '' : 'warning'" size="small">
          {{ row.skipApiCall === 1 ? '自有' : '亚丁' }}
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
              <el-descriptions-item label="保修到期">{{ formatDate(row.coverage) }}</el-descriptions-item>
              <el-descriptions-item label="系统时间">{{ row.sysTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="expand-section">
            <h4 class="expand-title">签约信息</h4>
            <el-descriptions :column="3" size="small" border>
              <el-descriptions-item label="昵称">{{ row.nickName || '-' }}</el-descriptions-item>
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

      <!-- 自定义列：激活状态 -->
      <template #activated="{ row }">
        <el-tag :type="row.activated ? 'success' : 'info'" size="small">
          {{ row.activated ? '已激活' : '--' }}
        </el-tag>
      </template>

      <!-- 自定义列：质保状态 -->
      <template #expired="{ row }">
        <el-tag :type="getWarrantyTagType(row)" size="small">
          {{ getWarrantyInfo(row).status }}
        </el-tag>
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
    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xls,.xlsx"
        :headers="upload.headers"
        :action="upload.url"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
        @change="handleFileChange"
      >
        <el-icon :size="67" color="#C0C4CC"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <br />
          <span style="color: #e6a23c">必填列：旧手机序列号、创建时间（模板中已红色加粗标注）</span>
          <br />
          <el-link
            type="primary"
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            @click="importTemplate"
            >下载模板</el-link
          >
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">确 定</el-button>
          <el-button
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
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Download, Upload, Picture, View, Printer, UploadFilled } from '@element-plus/icons-vue';
import { queryOrderList, queryPhoneTypeList, getOrderContractContent, getOrderDetail } from '@/api/order/list';
import { exportExcel } from '@/utils/export';
import { getToken } from '@/utils/auth';
import * as XLSX from 'xlsx';
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

/** 处理展开行：按需加载详情数据 */
const handleExpandChange = (row, expanded) => {
  if (!expanded) return;
  // 已加载过详情或正在加载中，则跳过
  if (detailLoadingMap[row.id] || row.activated !== undefined) return;
  detailLoadingMap[row.id] = true;
  getOrderDetail(row.id)
    .then((res) => {
      const detail = res.data || res;
      // 将详情字段合并到 row 中，展开面板即可自动展示
      Object.assign(row, detail);
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

// 搜索字段配置
const searchFields = [
  { prop: 'sn', label: '序列号', type: 'input' },
  {
    prop: 'phoneType',
    label: '旧手机品牌',
    type: 'select',
    options: [],
  },
  { prop: 'createBy', label: '创建者', type: 'input' },
  { prop: 'nickName', label: '昵称', type: 'input' },
  { prop: 'storeId', label: '所属门店', type: 'input' },
  { prop: 'name', label: '留资人姓名', type: 'input' },
  { prop: 'phoneNum', label: '留资人电话', type: 'input' },
  {
    prop: 'activated',
    label: '激活状态',
    type: 'select',
    options: [
      { label: '全部', value: null },
      { label: '已激活', value: true },
      { label: '未激活', value: false },
    ],
  },
  {
    prop: 'oldPhoneStatus',
    label: '订单类型',
    type: 'select',
    options: [
      { label: '全部', value: null },
      { label: '无旧手机', value: 0 },
      { label: '旧手机已损坏/遗失', value: 1 },
    ],
  },
  {
    prop: 'skipApiCall',
    label: '签约渠道',
    type: 'select',
    options: [
      { label: '全部', value: null },
      { label: '自有', value: 1 },
      { label: '亚丁', value: 0 },
    ],
  },
];

// 表格列配置
const columns = [
  { label: '', slot: 'expand', width: '50' },
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
  { label: '创建者', prop: 'createBy' },
  { label: '所属门店', prop: 'storeName', width: '130' },
  {
    label: '鸭宝激活状态',
    prop: 'activated',
    slot: 'activated',
    width: '120',
  },
  { label: '质保状态', slot: 'expired' },
  {
    label: '创建时间',
    prop: 'createTime',
    slot: 'createTime',
    width: '180',
  },
];

/** 获取手机品牌列表 */
const getPhoneTypeList = () => {
  queryPhoneTypeList()
    .then((response) => {
      phoneTypeList.value = response.data || [];
      const phoneTypeField = searchFields.find((f) => f.prop === 'phoneType');
      if (phoneTypeField) {
        phoneTypeField.options = phoneTypeList.value.map((item) => ({
          label: item.name,
          value: item.code,
        }));
      }
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

/** 计算质保状态 */
const getWarrantyInfo = (row) => {
  if (!row || !row.activated) {
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

/** 质保状态 tag 类型 */
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

/** 导出 */
const handleExport = () => {
  ElMessageBox.confirm('是否确认导出订单数据？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      exportLoading.value = true;
      const queryParams = proTableRef.value.getQueryParams();
      exportExcel({
        fetchApi: queryOrderList,
        queryParams,
        columns: [
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
            formatter: (row) => (row.skipApiCall === 1 ? '自有' : '亚丁'),
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
          { label: '旧手机保修到期时间', prop: 'coverage', formatter: (row) => formatDate(row.coverage) },
          { label: '质保状态', formatter: (row) => getWarrantyInfo(row).status },
          { label: '查询时系统时间', prop: 'sysTime' },
          { label: '创建者', prop: 'createBy' },
          { label: '所属门店', prop: 'storeName' },
          { label: '昵称', prop: 'nickName' },
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
        ],
        fileName: '订单数据',
        sheetName: '订单数据',
      })
        .then((count) => {
          ElMessage.success(`导出成功，共 ${count} 条数据`);
          exportLoading.value = false;
        })
        .catch((err) => {
          if (err.message === 'EMPTY_DATA') {
            ElMessage.warning('没有可导出的数据');
          } else {
            console.error('导出订单数据失败：', err);
            ElMessage.error('导出失败，请稍后重试');
          }
          exportLoading.value = false;
        });
    })
    .catch(() => {});
};

/** 导入 */
const handleImport = () => {
  upload.title = '订单导入';
  upload.open = true;
  selectedFile.value = null;
};

/** 下载模板 */
const importTemplate = () => {
  proxy.download('system/order/importTemplate', {}, `order_template_${new Date().getTime()}.xlsx`);
};

/** 文件上传进度 */
const handleFileUploadProgress = () => {
  upload.isUploading = true;
};

/** 文件上传成功 */
const handleFileSuccess = (response) => {
  upload.open = false;
  upload.isUploading = false;
  selectedFile.value = null;
  uploadRef.value.clearFiles();
  const msg = typeof response === 'string' ? response : response.data || response.msg || '';
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
    proxy.$modal.msgError('请先选择文件');
    return;
  }
  const fileName = (file.name || '').toLowerCase();
  if (!fileName.endsWith('.xls') && !fileName.endsWith('.xlsx')) {
    proxy.$modal.msgError('请选择后缀为"xls"或"xlsx"的文件。');
    return;
  }
  const raw = file.raw;
  if (!raw) {
    proxy.$modal.msgError('文件读取失败，请重新选择文件');
    return;
  }
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 });
      if (!rows || rows.length < 2) {
        proxy.$modal.msgError('Excel 文件中没有数据行，请填写至少一行数据');
        return;
      }
      const headerRow = rows[0];
      const columnMap = {};
      headerRow.forEach((h, idx) => {
        const clean = String(h || '')
          .replace(/[（(]必填[）)]/g, '')
          .trim();
        if (clean) columnMap[clean] = idx;
      });
      const requiredColumns = ['旧手机序列号', '创建时间'];
      const missingCols = requiredColumns.filter((col) => !(col in columnMap));
      if (missingCols.length > 0) {
        proxy.$modal.msgError('Excel 缺必填列：' + missingCols.join('、') + '，请使用最新模板');
        return;
      }
      for (let r = 1; r < rows.length; r++) {
        const row = rows[r];
        if (!row || row.every((cell) => cell === undefined || String(cell).trim() === '')) {
          continue;
        }
        for (const col of requiredColumns) {
          const idx = columnMap[col];
          const val = row[idx];
          if (val === undefined || val === null || String(val).trim() === '') {
            proxy.$modal.msgError(`第 ${r + 1} 行「${col}」不能为空，请修正后重新上传`);
            return;
          }
        }
      }
      uploadRef.value.submit();
    } catch (err) {
      console.error('Excel 解析失败：', err);
      proxy.$modal.msgError('Excel 文件解析失败，请检查文件格式');
    }
  };
  reader.readAsArrayBuffer(raw);
};

onMounted(() => {
  if (route.query.sn) {
    const params = proTableRef.value?.queryParams;
    if (params) params.sn = route.query.sn;
    proTableRef.value?.refresh();
  }
  getPhoneTypeList();
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
