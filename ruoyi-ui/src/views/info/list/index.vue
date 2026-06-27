<template>
  <div class="app-container">
    <pro-table
      ref="proTable"
      :fetch-api="queryOrderList"
      :search-fields="searchFields"
      :columns="columns"
      :extra-params="extraParams"
      row-key="id"
    >
      <!-- 工具栏：导出/导入按钮 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-tooltip content="导出时会按照当前筛选条件导出数据" placement="top" :open-delay="500">
            <el-button
              type="warning"
              plain
              icon="el-icon-download"
              size="mini"
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
            icon="el-icon-upload2"
            size="mini"
            @click="handleImport"
            v-hasRole="['admin']"
            >导入</el-button
          >
        </el-col>
      </template>

      <!-- 自定义列：订单类型 -->
      <template #skipApiCall="{ row }">
        <el-tag v-if="row.skipApiCall === 1 &amp;&amp; row.oldPhoneStatus === 0" type="danger" size="small"
          >无旧手机新手机签约</el-tag
        >
        <el-tag v-else-if="row.skipApiCall === 1 &amp;&amp; row.oldPhoneStatus === 1" type="danger" size="small"
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

      <!-- 自定义列：IMEI1 -->

      <!-- 展开行：旧手机详情 + 签约信息 -->
      <template #expand="{ row }">
        <div class="expand-content">
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
                <span v-else-if="row.oldPhoneUsageMonths">> {{ row.oldPhoneUsageMonths }}</span>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="图片">
                <el-button
                  v-if="row.imagePath"
                  size="mini"
                  type="text"
                  icon="el-icon-picture-outline"
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
                  size="mini"
                  type="text"
                  icon="el-icon-view"
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
                  size="mini"
                  type="text"
                  icon="el-icon-picture-outline"
                  @click="handleViewImage(row.signaturePath)"
                  >查看</el-button
                >
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
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

    <!-- 全局图片预览器：使用 el-image-viewer，避免 <el-image> 缩略图 + 预览图双重加载 -->
    <el-image-viewer v-if="previewVisible" :url-list="previewUrlList" :on-close="handleClosePreview" :z-index="3000" />

    <!-- 协议内容预览抽屉 -->
    <el-drawer :title="drawerTitle" :visible.sync="drawerVisible" direction="rtl" size="50%" append-to-body>
      <div class="drawer-toolbar">
        <el-button type="primary" size="small" icon="el-icon-printer" @click="handlePrint">打印协议</el-button>
      </div>
      <div id="printArea" ref="printArea" class="contract-content-wrapper">
        <div v-html="drawerContent"></div>
      </div>
    </el-drawer>

    <!-- 订单导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已存在的订单数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <br />
          <span style="color: #e6a23c">必填列：旧手机序列号、创建时间（模板中已红色加粗标注）</span>
          <br />
          <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { queryOrderList, queryPhoneTypeList, getOrderContractContent } from '@/api/order/list';
import { exportExcel } from '@/utils/export';
import { getToken } from '@/utils/auth';
import * as XLSX from 'xlsx';
// element-ui 内部的全局图片预览器（和 <el-image> 点击后弹出的是同一个组件），
// 直接使用可以避免 <el-image> 缩略图 + 预览图的重复请求
import ElImageViewer from 'element-ui/packages/image/src/image-viewer';

export default {
  name: 'Order',
  components: { ElImageViewer },
  data() {
    return {
      // 图片资源前缀地址
      baseApi: process.env.VUE_APP_BASE_API,
      // 导出loading
      exportLoading: false,
      // 导入参数
      upload: {
        open: false,
        title: '',
        isUploading: false,
        updateSupport: false,
        headers: { Authorization: 'Bearer ' + getToken() },
        url: process.env.VUE_APP_BASE_API + '/system/order/importData',
      },
      // 手机品牌列表
      phoneTypeList: [],
      // 列表请求接口
      queryOrderList,
      // 额外参数（如从路由携带的 sn）
      extraParams: {},
      // 抽屉相关
      drawerVisible: false,
      drawerTitle: '',
      drawerContent: '',
      currentRow: null,
      // 图片预览器控制：默认不加载，点击查看后才渲染 el-image-viewer 并请求 1 次原图
      previewVisible: false,
      previewUrlList: [],
      // 搜索字段配置
      searchFields: [
        { prop: 'sn', label: '序列号', type: 'input' },
        {
          prop: 'phoneType',
          label: '旧手机品牌',
          type: 'select',
          options: [], // 动态加载
        },
        { prop: 'createBy', label: '创建者', type: 'input' },
        { prop: 'nickName', label: '昵称', type: 'input' },
        { prop: 'name', label: '留资人姓名', type: 'input' },
        { prop: 'phoneNum', label: '留资人电话', type: 'input' },
        {
          prop: 'activated',
          label: '激活状态',
          type: 'select',
          options: [
            { label: '已激活', value: true },
            { label: '未激活', value: false },
          ],
        },
        {
          prop: 'oldPhoneStatus',
          label: '订单类型',
          type: 'select',
          options: [
            { label: '全部', value: '' },
            { label: '有旧手机（正常）', value: 2 },
            { label: '有旧手机但已损坏/遗失', value: 1 },
            { label: '无旧手机', value: 0 },
          ],
        },
      ],
      // 表格列配置
      columns: [
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
        {
          label: '鸭宝激活状态',
          prop: 'activated',
          slot: 'activated',
          width: '120',
        },
        { label: '质保状态', slot: 'expired'},
        {
          label: '创建时间',
          prop: 'createTime',
          slot: 'createTime',
          width: '180',
        },
      ],
    };
  },
  created() {
    // 从路由参数中读取 sn（快速查询跳转时携带）
    if (this.$route.query.sn) {
      this.extraParams = { sn: this.$route.query.sn };
    }
    // 给质保状态列表头注入 render-header（含问号提示）
    const expiredCol = this.columns.find((c) => c.slot === 'expired');
    if (expiredCol) {
      expiredCol.renderHeader = this.renderWarrantyHeader;
    }
    this.getPhoneTypeList();
  },
  methods: {
    /** 获取手机品牌列表 */
    getPhoneTypeList() {
      queryPhoneTypeList()
        .then((response) => {
          this.phoneTypeList = response.data || [];
          // 动态更新搜索字段的 options
          const phoneTypeField = this.searchFields.find((f) => f.prop === 'phoneType');
          if (phoneTypeField) {
            phoneTypeField.options = this.phoneTypeList.map((item) => ({
              label: item.name,
              value: item.code,
            }));
          }
        })
        .catch((err) => {
          console.error('获取手机品牌列表失败：', err);
        });
    },
    /** 根据品牌code获取品牌名称 */
    getPhoneTypeName(code) {
      if (!code) return '-';
      const item = this.phoneTypeList.find((t) => t.code === code);
      return item ? item.name : code;
    },
    /** 点击「查看」按钮：直接使用全局 el-image-viewer 预览图片，仅请求 1 次
     * @param {string} relativePath 服务器返回的图片相对路径
     */
    handleViewImage(relativePath) {
      if (!relativePath) return;
      this.previewUrlList = [this.baseApi + relativePath];
      this.previewVisible = true;
    },
    /** 关闭预览器 */
    handleClosePreview() {
      this.previewVisible = false;
      this.previewUrlList = [];
    },
    /**
     * 将任意日期入参统一格式化为 YYYY-MM-DD（本地时区）
     * 兼容后端返回的多种形式：'2021-08-28' / '2023/12/31' / ISO 字符串 / 毫秒数等
     */
    formatDate(value) {
      if (!value && value !== 0) return '-';
      // 优先使用上下文可用的 parseTime（若公共工具提供）以保证跨项目一致性
      let date;
      if (typeof value === 'number') {
        date = new Date(value);
      } else if (typeof value === 'string') {
        // 将 'YYYY/MM/DD' 转为 'YYYY-MM-DD'，避免 Safari 及不同浏览器的时区解析差异
        const normalized = value.replace(/\//g, '-').trim();
        date = new Date(normalized);
        if (isNaN(date.getTime())) {
          // 再试一次原始入参
          date = new Date(value);
        }
      } else {
        date = new Date(value);
      }
      if (!date || isNaN(date.getTime())) {
        // 解析失败，原样返回，不破坏数据
        return String(value);
      }
      const yyyy = date.getFullYear();
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd}`;
    },
    /**
     * 计算质保状态（逻辑与小程序保持一致）
     * 注意：每条数据的过保判断仅以该条记录返回的 sysTime 字段为准，
     *      不使用本地时间或其他行的时间，避免被本地系统时间篑改影响。
     * 返回 { status, expired }
     *  - 未激活：activated 为 false → 已过期
     *  - 已激活但无 coverage：仅显示“已激活”，不视为过期
     *  - 已激活但缺失 sysTime：保守显示“已激活”，不视为过期
     *  - 已激活且 coverage <= row.sysTime：已过保（过期）
     *  - 已激活且 coverage >  row.sysTime：保修中（未过期）
     */
    getWarrantyInfo(row) {
      if (!row || !row.activated) {
        return { status: '未激活', expired: true };
      }
      const coverage = row.coverage;
      if (!coverage) {
        return { status: '已激活', expired: false };
      }
      // 必须使用本行返回的 sysTime，缺失则不判断过期，保守显示为“已激活”
      if (!row.sysTime) {
        return { status: '已激活', expired: false };
      }
      // 先将 '/' 统一为 '-'，避免不同浏览器解析为不同时区造成边界差异
      const normalize = (v) => (typeof v === 'string' ? v.replace(/\//g, '-') : v);
      const coverageTime = new Date(normalize(coverage)).getTime();
      const sysTime = new Date(normalize(row.sysTime)).getTime();
      if (isNaN(coverageTime) || isNaN(sysTime)) {
        return { status: '已激活', expired: false };
      }
      const expired = coverageTime <= sysTime;
      return { status: expired ? '已过保' : '保修中', expired };
    },
    /** 根据质保状态返回 el-tag 的 type */
    getWarrantyTagType(row) {
      const info = this.getWarrantyInfo(row);
      if (info.expired) return 'danger';
      // “已激活”作为中性状态显示为 info；“保修中”显示为 success
      return info.status === '保修中' ? 'success' : 'info';
    },
    /** 自定义表头渲染：质保状态 + 问号提示 */
    renderWarrantyHeader(h, { column }) {
      return h('span', { style: { display: 'inline-flex', alignItems: 'center' } }, [
        h('span', column.label),
        h('el-tooltip', {
          props: { placement: 'top', effect: 'dark' },
        }, [
          h('div', {
            slot: 'content',
            style: { maxWidth: '280px', lineHeight: '1.8' },
          }, [
            h('div', '质保状态根据查询时的系统时间判定：'),
            h('div', '· 未激活 → 显示「未激活」'),
            h('div', '· 已激活但无保修到期时间 → 「已激活」'),
            h('div', '\u2003（视为未过期）'),
            h('div', '· 保修到期时间 ≤ 查询系统时间 → 「已过保」'),
            h('div', '· 保修到期时间 > 查询系统时间 → 「保修中」'),
          ]),
          h('svg', {
            attrs: { viewBox: '0 0 1024 1024', width: '14', height: '14' },
            style: { marginLeft: '4px', cursor: 'pointer', verticalAlign: 'middle' },
          }, [
            h('path', { attrs: { d: 'M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64z m0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z', fill: '#909399' } }),
            h('path', { attrs: { d: 'M464 688a48 48 0 1 0 96 0 48 48 0 1 0-96 0z m24-112h48c4.4 0 8-3.6 8-8v-16c0-61.8 35.2-115.4 85.8-141.6 19.6-10 31.8-29.8 31.8-51.8 0-34.2-27.8-62-62-62h-0.6c-32 0.2-58.6 25-61.4 56.6-0.8 9.2-8.4 16.4-17.6 16.4h-48c-10.6 0-19-9.2-17.6-19.8C421 297.6 461.2 256 512.2 256h0.6c51 0.2 92.8 40.6 95.2 91.4 2.6 55.6-33 103.2-85.2 126.2-19.4 8.6-32.8 28.2-32.8 50.4v16c0 4.4-3.6 8-8 8z', fill: '#fff' } }),
          ]),
        ]),
      ]);
    },
    /** 生成水印Canvas */
    generateWatermark(text) {
      const canvas = document.createElement('canvas');
      canvas.width = 200;
      canvas.height = 200;
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.font = '16px Microsoft YaHei';
      ctx.fillStyle = 'rgba(180, 180, 180, 0.3)';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.translate(100, 100);
      ctx.rotate((-30 * Math.PI) / 180);
      ctx.fillText(text, 0, 0);
      return canvas.toDataURL();
    },
    /** 查看协议内容（抽屉展示）—— 按需异步加载合同内容 */
    handleViewContract(row) {
      this.drawerTitle = '用户协议内容';
      this.currentRow = row;

      // 如果列表数据已不再携带 contractContent，则通过接口按需加载
      if (row.contractId) {
        getOrderContractContent(row.id).then((res) => {
          const content = res.data;
          if (!content) {
            this.$message.warning('协议内容不存在');
            return;
          }
          this.renderContractContent(content);
          this.drawerVisible = true;
        }).catch(() => {
          this.$message.error('获取协议内容失败，请稍后重试');
        });
        return;
      }

      // 兜底：旧数据可能仍有 contractContent（兼容）
      if (row.contractContent) {
        this.renderContractContent(row.contractContent);
        this.drawerVisible = true;
        return;
      }

      // 只有合同文件路径，无富文本内容
      if (row.contractPath) {
        window.open(this.baseApi + row.contractPath, '_blank');
        return;
      }

      this.$message.warning('暂无协议内容');
    },
    /** 渲染协议内容（将签名数据注入协议HTML） */
    renderContractContent(content) {
      const row = this.currentRow;

      // 将签名数据填充到协议HTML中对应的占位符位置
      // 调试：打印原始协议HTML
      console.log('[协议调试] 原始HTML：', content);
      console.log('[协议调试] 行数据：', {
        signatureModel: row.signatureModel,
        signatureImei: row.signatureImei,
        signaturePath: row.signaturePath,
        signatureDate: row.signatureDate,
      });

      // 协议中关键词（设备型号/设备IME/签字确认/日期）后面可能存在以下几种形态：
      // 1) <u>&nbsp;...&nbsp;</u> 占位下划线  →  需要替换为实际值
      // 2) 紧跟空白/&nbsp;（无 <u> 占位） →  需要直接注入实际值
      // 3) 已经填充了实际值                 →  保持不变（避免重复注入）
      //
      // 通用注入函数：
      // - 在 keywordPattern（含冒号）匹配位置之后插入 valueHtml
      // - 若紧跟着的是 <u>...</u> 占位（可被空白/&nbsp;/空 span 包裹），则一并替换掉该占位
      // - 若紧跟着的是纯空白/&nbsp;（无实质内容直到下一个非空标签开始），也视为占位，覆盖掉
      // - 若紧跟着已经是实际文本/图片（非空白），则不重复注入
      const injectAfterKeyword = (html, keywordPattern, valueHtml) => {
        if (!valueHtml) return html;
        // 关键词后允许出现：</span>、空白、&nbsp;、<span ...>，然后是占位 <u>...</u> 或下一个有意义内容
        // case A: 紧跟 <u>...</u> 占位 → 整体替换占位
        const regexWithU = new RegExp(
          `(${keywordPattern})((?:\\s|&nbsp;|</span>|<span[^>]*>)*)<u\\b[^>]*>[\\s\\S]*?</u>`,
          'i'
        );
        if (regexWithU.test(html)) {
          return html.replace(regexWithU, `$1$2${valueHtml}`);
        }
        // case B: 紧跟空白/&nbsp;且后续没有实际文本前先遇到下一个块 → 注入
        // 检测冒号后到下一个非空白字符之间是否仅由 空白/&nbsp;/空标签 组成
        const regexEmpty = new RegExp(`(${keywordPattern})((?:\\s|&nbsp;)*)(?=<|$)`, 'i');
        const match = html.match(regexEmpty);
        if (match) {
          // 进一步判断：冒号后第一个非空白字符是否是结束/换行类（说明位置为空，可注入）
          // 简化处理：直接在冒号后注入
          return html.replace(regexEmpty, `$1$2${valueHtml}`);
        }
        return html;
      };

      // 设备型号
      if (row.signatureModel) {
        content = injectAfterKeyword(
          content,
          '设备型号[：:]',
          `<span style="text-decoration:underline;padding:0 4px;">${row.signatureModel}</span>`
        );
      }

      // 设备IMEI（兼容 IME / IMEI）
      if (row.signatureImei) {
        content = injectAfterKeyword(
          content,
          '设备IME[I]?[：:]',
          `<span style="text-decoration:underline;padding:0 4px;">${row.signatureImei}</span>`
        );
      }

      // 签字确认 → 插入签名图片
      if (row.signaturePath) {
        content = injectAfterKeyword(
          content,
          '签字确认[：:]',
          `<img src="${
            this.baseApi + row.signaturePath
          }" style="max-width:200px;max-height:80px;vertical-align:middle;" />`
        );
      }

      // 日期
      if (row.signatureDate) {
        content = injectAfterKeyword(
          content,
          '日(?:\\s|&nbsp;)*期(?:\\s|&nbsp;)*[：:]',
          `<span style="text-decoration:underline;padding:0 4px;">${row.signatureDate}</span>`
        );
      }

      // 调试：打印替换后的HTML
      console.log('[协议调试] 替换后HTML：', content);
      this.drawerContent = content;
      // 设置水印（以当前行的昵称为水印）
      this.$nextTick(() => {
        const watermarkText = row.nickName || row.createBy || '用户';
        const watermarkUrl = this.generateWatermark(watermarkText);
        if (this.$refs.printArea) {
          this.$refs.printArea.style.backgroundImage = `url(${watermarkUrl})`;
        }
      });
    },
    /** 打印协议 */
    handlePrint() {
      // 获取协议正文内容
      const contentDiv = this.$refs.printArea.querySelector('div');
      const printContent = contentDiv ? contentDiv.innerHTML : this.$refs.printArea.innerHTML;
      // 打印时以当前登录用户昵称作为水印
      const currentUserName = this.$store.getters.nickName || this.$store.getters.name || '用户';
      const watermarkUrl = this.generateWatermark(currentUserName);
      const printWindow = window.open('', '_blank');
      if (!printWindow) {
        this.$message.warning('请允许弹出窗口后重试');
        return;
      }
      const htmlContent = `
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <title>用户协议</title>
          <style>
            body {
              padding: 40px;
              font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
              line-height: 1.8;
              font-size: 14px;
              color: #333;
              position: relative;
            }
            .watermark {
              position: fixed;
              top: 0;
              left: 0;
              width: 100%;
              height: 100%;
              background-image: url(${watermarkUrl});
              background-repeat: repeat;
              pointer-events: none;
              z-index: 9999;
            }
            h2, h3 {
              margin: 16px 0 8px;
              color: #1a1a1a;
            }
            p {
              margin: 8px 0;
            }
            img {
              max-width: 200px;
              max-height: 80px;
              vertical-align: middle;
            }
            table {
              border-collapse: collapse;
              width: 100%;
              margin: 16px 0;
            }
            table th, table td {
              border: 1px solid #ccc;
              padding: 8px 12px;
              text-align: left;
            }
            table th {
              background-color: #f5f5f5;
              font-weight: bold;
            }
            @media print {
              body { padding: 20px; }
              .watermark {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background-image: url(${watermarkUrl});
                background-repeat: repeat;
                pointer-events: none;
                z-index: 9999;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
              }
            }
          </style>
        </head>
        <body>
          <div class="watermark"></div>
          <div class="content">${printContent}</div>
        </body>
        </html>
      `;
      printWindow.document.open();
      printWindow.document.write(htmlContent);
      printWindow.document.close();
      // 等待内容和图片加载完成后再打印
      printWindow.onload = function () {
        setTimeout(() => {
          printWindow.print();
        }, 300);
      };
      // 兜底：如果 onload 没触发，3秒后强制打印
      setTimeout(() => {
        if (!printWindow.closed) {
          printWindow.print();
        }
      }, 3000);
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$confirm('是否确认导出订单数据？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          this.exportLoading = true;
          // 获取当前 ProTable 的查询参数（含筛选条件）
          const queryParams = this.$refs.proTable.getQueryParams();
          exportExcel({
            fetchApi: queryOrderList,
            queryParams,
            columns: [
              { label: 'ID', prop: 'id', width: 6 },
              {
                label: '订单类型',
                formatter: (row) => {
                  if (row.skipApiCall === 1 && row.oldPhoneStatus === 0) return '无旧手机新手机签约';
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
              {
                label: '旧手机品牌',
                prop: 'phoneType',
                formatter: (row) => this.getPhoneTypeName(row.phoneType),
                width: 12,
              },
              { label: '旧手机型号', prop: 'model' },
              { label: '旧手机序列号', prop: 'sn', width: 18 },
              { label: '旧手机IMEI1', prop: 'imei1', width: 18 },
              { label: '旧手机IMEI2', prop: 'imei2', width: 18 },
              {
                label: '旧手机激活状态',
                prop: 'activated',
                formatter: (row) => (row.activated ? '已激活' : '--'),
              },
              {
                label: '旧手机激活日期',
                prop: 'activateDate',
                formatter: (row) => this.formatDate(row.activateDate),
              },
              {
                label: '旧手机保修到期时间',
                prop: 'coverage',
                formatter: (row) => this.formatDate(row.coverage),
              },
              {
                label: '质保状态',
                formatter: (row) => this.getWarrantyInfo(row).status,
              },
              { label: '查询时系统时间', prop: 'sysTime' },
              { label: '创建者', prop: 'createBy' },
              { label: '昵称', prop: 'nickName' },
              { label: '留资人姓名', prop: 'name' },
              { label: '留资人电话', prop: 'phoneNum' },
              {
                label: '用户协议',
                prop: 'contractPath',
                formatter: (row) => (row.contractPath ? this.baseApi + row.contractPath : '-'),
                width: 40,
              },
              {
                label: '用户签名',
                prop: 'signaturePath',
                formatter: (row) => (row.signaturePath ? this.baseApi + row.signaturePath : '-'),
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
              this.$message.success(`导出成功，共 ${count} 条数据`);
              this.exportLoading = false;
            })
            .catch((err) => {
              if (err.message === 'EMPTY_DATA') {
                this.$message.warning('没有可导出的数据');
              } else {
                console.error('导出订单数据失败：', err);
                this.$message.error('导出失败，请稍后重试');
              }
              this.exportLoading = false;
            });
        })
        .catch(() => {});
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = '订单导入';
      this.upload.open = true;
    },
    /** 下载导入模板 */
    importTemplate() {
      this.download('system/order/importTemplate', {}, `order_template_${new Date().getTime()}.xlsx`);
    },
    /** 文件上传中处理 */
    handleFileUploadProgress() {
      this.upload.isUploading = true;
    },
    /** 文件上传成功处理 */
    handleFileSuccess(response) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      const msg = typeof response === 'string' ? response : (response.data || response.msg || '');
      this.$alert(
        "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + msg + '</div>',
        '导入结果',
        { dangerouslyUseHTMLString: true }
      );
      // 刷新列表（重置到第一页）
      this.$refs.proTable.queryParams.pageNum = 1;
      this.$refs.proTable.refresh();
    },
    /** 提交上传文件 —— 前端校验必填列 */
    submitFileForm() {
      const file = this.$refs.upload.uploadFiles;
      if (
        !file ||
        file.length === 0 ||
        (!file[0].name.toLowerCase().endsWith('.xls') && !file[0].name.toLowerCase().endsWith('.xlsx'))
      ) {
        this.$modal.msgError('请选择后缀为"xls"或"xlsx"的文件。');
        return;
      }
      // 前端读取Excel校验必填列
      const raw = file[0].raw;
      if (!raw) {
        this.$modal.msgError('文件读取失败，请重新选择文件');
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
            this.$modal.msgError('Excel 文件中没有数据行，请填写至少一行数据');
            return;
          }
          // 表头行（匹配带不带（必填）后缀的列名）
          const headerRow = rows[0];
          const columnMap = {};
          headerRow.forEach((h, idx) => {
            const clean = String(h || '').replace(/[（(]必填[）)]/g, '').trim();
            if (clean) columnMap[clean] = idx;
          });
          // 必填列定义（与后端 @Excel(required=true) 保持一致）
          const requiredColumns = ['旧手机序列号', '创建时间'];
          const missingCols = requiredColumns.filter((col) => !(col in columnMap));
          if (missingCols.length > 0) {
            this.$modal.msgError('Excel 缺必填列：' + missingCols.join('、') + '，请使用最新模板');
            return;
          }
          // 逐行校验必填字段
          for (let r = 1; r < rows.length; r++) {
            const row = rows[r];
            if (!row || row.every((cell) => cell === undefined || String(cell).trim() === '')) {
              continue; // 跳过完全空行
            }
            for (const col of requiredColumns) {
              const idx = columnMap[col];
              const val = row[idx];
              if (val === undefined || val === null || String(val).trim() === '') {
                this.$modal.msgError(`第 ${r + 1} 行「${col}」不能为空，请修正后重新上传`);
                return;
              }
            }
          }
          // 校验通过，执行上传
          this.$refs.upload.submit();
        } catch (err) {
          console.error('Excel 解析失败：', err);
          this.$modal.msgError('Excel 文件解析失败，请检查文件格式');
        }
      };
      reader.readAsArrayBuffer(raw);
    },
  },
};
</script>

<style scoped>
.drawer-toolbar {
  padding: 0 20px 10px;
  border-bottom: 1px solid #ebeef5;
}

/* 展开行内容 */
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
/* 穿透 v-html 中的表格样式 */
.contract-content-wrapper /deep/ table {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}
.contract-content-wrapper /deep/ table th,
.contract-content-wrapper /deep/ table td {
  border: 1px solid #ccc;
  padding: 8px 12px;
  text-align: left;
}
.contract-content-wrapper /deep/ table th {
  background-color: #f5f5f5;
  font-weight: bold;
}
</style>
  background-color: #f5f5f5;
  font-weight: bold;
}
</style>
