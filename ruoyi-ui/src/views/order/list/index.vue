<template>
  <div class="app-container">
    <pro-table
      ref="proTable"
      :fetch-api="queryOrderList"
      :search-fields="searchFields"
      :columns="columns"
      :extra-params="extraParams"
    >
      <!-- 工具栏：导出按钮 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-tooltip
            content="导出时会按照当前筛选条件导出数据"
            placement="top"
            :open-delay="500"
          >
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
      </template>

      <!-- 自定义列：手机品牌 -->
      <template #phoneType="{ row }">
        <span>{{ getPhoneTypeName(row.phoneType) }}</span>
      </template>

      <!-- 自定义列：手机型号 -->
      <template #model="{ row }">
        <span>{{ row.model || "-" }}</span>
      </template>

      <!-- 自定义列：图片 -->
      <template #imagePath="{ row }">
        <el-image
          v-if="row.imagePath"
          :src="baseApi + row.imagePath"
          :preview-src-list="[baseApi + row.imagePath]"
          style="width: 40px; height: 40px"
          fit="cover"
        />
        <span v-else>-</span>
      </template>

      <!-- 自定义列：IMEI1 -->
      <template #imei1="{ row }">
        <span>{{ row.imei1 || "-" }}</span>
      </template>

      <!-- 自定义列：IMEI2 -->
      <template #imei2="{ row }">
        <span>{{ row.imei2 || "-" }}</span>
      </template>

      <!-- 自定义列：激活状态 -->
      <template #activated="{ row }">
        <el-tag :type="row.activated ? 'success' : 'info'" size="small">
          {{ row.activated ? "已激活" : "未激活" }}
        </el-tag>
      </template>

      <!-- 自定义列：是否过期 -->
      <template #expired="{ row }">
        <el-tag
          :type="isExpired(row.coverage) ? 'danger' : 'success'"
          size="small"
        >
          {{ isExpired(row.coverage) ? "已过期" : "未过期" }}
        </el-tag>
      </template>

      <!-- 自定义列：用户协议 -->
      <template #contract="{ row }">
        <el-button
          v-if="row.contractContent"
          size="mini"
          type="text"
          icon="el-icon-view"
          @click="handleViewContract(row)"
          >查看协议</el-button
        >
        <el-link
          v-else-if="row.contractPath"
          type="primary"
          :href="baseApi + row.contractPath"
          target="_blank"
          :underline="false"
        >
          查看协议
        </el-link>
        <span v-else>-</span>
      </template>

      <!-- 自定义列：用户签名 -->
      <template #signature="{ row }">
        <el-image
          v-if="row.signaturePath"
          :src="baseApi + row.signaturePath"
          :preview-src-list="[baseApi + row.signaturePath]"
          style="width: 40px; height: 40px"
          fit="contain"
        />
        <span v-else>-</span>
      </template>

      <!-- 自定义列：创建时间 -->
      <template #createTime="{ row }">
        <span>{{ parseTime(row.createTime) }}</span>
      </template>
    </pro-table>

    <!-- 协议内容预览抽屉 -->
    <el-drawer
      :title="drawerTitle"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="50%"
      append-to-body
    >
      <div class="drawer-toolbar">
        <el-button
          type="primary"
          size="small"
          icon="el-icon-printer"
          @click="handlePrint"
          >打印协议</el-button
        >
      </div>
      <div id="printArea" ref="printArea" class="contract-content-wrapper">
        <div v-html="drawerContent"></div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { queryOrderList, queryPhoneTypeList } from "@/api/order/list";
import { exportExcel } from "@/utils/export";

export default {
  name: "Order",
  data() {
    return {
      // 图片资源前缀地址
      baseApi: process.env.VUE_APP_BASE_API,
      // 导出loading
      exportLoading: false,
      // 手机品牌列表
      phoneTypeList: [],
      // 列表请求接口
      queryOrderList,
      // 额外参数（如从路由携带的 sn）
      extraParams: {},
      // 抽屉相关
      drawerVisible: false,
      drawerTitle: "",
      drawerContent: "",
      currentRow: null,
      // 搜索字段配置
      searchFields: [
        { prop: "sn", label: "序列号", type: "input" },
        {
          prop: "phoneType",
          label: "手机品牌",
          type: "select",
          options: [], // 动态加载
        },
        { prop: "createBy", label: "创建者", type: "input" },
        { prop: "nickName", label: "昵称", type: "input" },
        {
          prop: "activated",
          label: "激活状态",
          type: "select",
          options: [
            { label: "已激活", value: true },
            { label: "未激活", value: false },
          ],
        },
      ],
      // 表格列配置
      columns: [
        { label: "ID", prop: "id", width: "50" },
        {
          label: "手机品牌",
          prop: "phoneType",
          slot: "phoneType",
          minWidth: "80",
        },
        { label: "手机型号", prop: "model", slot: "model", minWidth: "80" },
        {
          label: "图片",
          prop: "imagePath",
          slot: "imagePath",
          width: "80",
          showOverflowTooltip: false,
        },
        { label: "序列号", prop: "sn", minWidth: "110" },
        { label: "IMEI1", prop: "imei1", slot: "imei1", minWidth: "110" },
        { label: "IMEI2", prop: "imei2", slot: "imei2", minWidth: "110" },
        {
          label: "激活状态",
          prop: "activated",
          slot: "activated",
          width: "90",
        },
        { label: "激活日期", prop: "activateDate", width: "110" },
        { label: "保修到期时间", prop: "coverage", width: "110" },
        { label: "是否过期", slot: "expired", width: "90" },
        { label: "查询时系统时间", prop: "sysTime", width: "110" },
        { label: "创建者", prop: "createBy", width: "100" },
        { label: "昵称", prop: "nickName", width: "100" },
        {
          label: "用户协议",
          prop: "contractPath",
          slot: "contract",
          width: "100",
          showOverflowTooltip: false,
        },
        {
          label: "用户签名",
          prop: "signaturePath",
          slot: "signature",
          width: "90",
          showOverflowTooltip: false,
        },
        { label: "签名型号", prop: "signatureModel", width: "120" },
        { label: "签名IMEI", prop: "signatureImei", width: "140" },
        { label: "签名日期", prop: "signatureDate", width: "110" },
        {
          label: "创建时间",
          prop: "createTime",
          slot: "createTime",
          width: "180",
        },
      ],
    };
  },
  created() {
    // 从路由参数中读取 sn（快速查询跳转时携带）
    if (this.$route.query.sn) {
      this.extraParams = { sn: this.$route.query.sn };
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
          const phoneTypeField = this.searchFields.find(
            (f) => f.prop === "phoneType"
          );
          if (phoneTypeField) {
            phoneTypeField.options = this.phoneTypeList.map((item) => ({
              label: item.name,
              value: item.code,
            }));
          }
        })
        .catch((err) => {
          console.error("获取手机品牌列表失败：", err);
        });
    },
    /** 根据品牌code获取品牌名称 */
    getPhoneTypeName(code) {
      if (!code) return "-";
      const item = this.phoneTypeList.find((t) => t.code === code);
      return item ? item.name : code;
    },
    /** 判断是否过期 */
    isExpired(coverage) {
      if (!coverage) return false;
      const coverageDate = new Date(coverage);
      const now = new Date();
      return coverageDate < now;
    },
    /** 生成水印Canvas */
    generateWatermark(text) {
      const canvas = document.createElement("canvas");
      canvas.width = 200;
      canvas.height = 200;
      const ctx = canvas.getContext("2d");
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.font = "16px Microsoft YaHei";
      ctx.fillStyle = "rgba(180, 180, 180, 0.3)";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.translate(100, 100);
      ctx.rotate((-30 * Math.PI) / 180);
      ctx.fillText(text, 0, 0);
      return canvas.toDataURL();
    },
    /** 查看协议内容（抽屉展示） */
    handleViewContract(row) {
      this.drawerTitle = "用户协议内容";
      this.currentRow = row;
      // 将签名数据填充到协议HTML中对应的占位符位置
      let content = row.contractContent || "";
      // 替换设备型号占位符
      if (row.signatureModel) {
        content = content.replace(
          /(<strong>设备型号：<\/strong>)_+/,
          `$1<span style="text-decoration:underline;padding:0 4px;">${row.signatureModel}</span>`
        );
      }
      // 替换设备IMEI占位符
      if (row.signatureImei) {
        content = content.replace(
          /(<strong>设备IMEI：<\/strong>)_+/,
          `$1<span style="text-decoration:underline;padding:0 4px;">${row.signatureImei}</span>`
        );
      }
      // 替换签字确认占位符（插入签名图片）
      if (row.signaturePath) {
        content = content.replace(
          /(<strong>签字确认：<\/strong>)_+/,
          `$1<img src="${
            this.baseApi + row.signaturePath
          }" style="max-width:200px;max-height:80px;vertical-align:middle;" />`
        );
      }
      // 替换日期占位符
      if (row.signatureDate) {
        content = content.replace(
          /(<strong>日(?:\s|&nbsp;)*期：<\/strong>)_+/,
          `$1<span style="text-decoration:underline;padding:0 4px;">${row.signatureDate}</span>`
        );
      }
      this.drawerContent = content;
      this.drawerVisible = true;
      // 设置水印（以当前行的昵称为水印）
      this.$nextTick(() => {
        const watermarkText = row.nickName || row.createBy || "用户";
        const watermarkUrl = this.generateWatermark(watermarkText);
        if (this.$refs.printArea) {
          this.$refs.printArea.style.backgroundImage = `url(${watermarkUrl})`;
        }
      });
    },
    /** 打印协议 */
    handlePrint() {
      // 获取协议正文内容
      const contentDiv = this.$refs.printArea.querySelector("div");
      const printContent = contentDiv
        ? contentDiv.innerHTML
        : this.$refs.printArea.innerHTML;
      // 打印时以当前登录用户昵称作为水印
      const currentUserName =
        this.$store.getters.nickName || this.$store.getters.name || "用户";
      const watermarkUrl = this.generateWatermark(currentUserName);
      const printWindow = window.open("", "_blank");
      if (!printWindow) {
        this.$message.warning("请允许弹出窗口后重试");
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
      this.$confirm("是否确认导出订单数据？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          this.exportLoading = true;
          // 获取当前 ProTable 的查询参数（含筛选条件）
          const queryParams = this.$refs.proTable.getQueryParams();
          exportExcel({
            fetchApi: queryOrderList,
            queryParams,
            columns: [
              { label: "ID", prop: "id", width: 6 },
              {
                label: "手机品牌",
                prop: "phoneType",
                formatter: (row) => this.getPhoneTypeName(row.phoneType),
                width: 12,
              },
              { label: "手机型号", prop: "model" },
              { label: "序列号", prop: "sn", width: 18 },
              { label: "IMEI1", prop: "imei1", width: 18 },
              { label: "IMEI2", prop: "imei2", width: 18 },
              {
                label: "激活状态",
                prop: "activated",
                formatter: (row) => (row.activated ? "已激活" : "未激活"),
              },
              { label: "激活日期", prop: "activateDate" },
              { label: "保修到期时间", prop: "coverage" },
              {
                label: "是否过期",
                formatter: (row) =>
                  this.isExpired(row.coverage) ? "已过期" : "未过期",
              },
              { label: "查询时系统时间", prop: "sysTime" },
              { label: "创建者", prop: "createBy" },
              { label: "昵称", prop: "nickName" },
              {
                label: "用户协议",
                prop: "contractPath",
                formatter: (row) =>
                  row.contractPath ? this.baseApi + row.contractPath : "-",
                width: 40,
              },
              {
                label: "用户签名",
                prop: "signaturePath",
                formatter: (row) =>
                  row.signaturePath ? this.baseApi + row.signaturePath : "-",
                width: 40,
              },
              { label: "签名型号", prop: "signatureModel" },
              { label: "签名IMEI", prop: "signatureImei", width: 18 },
              { label: "签名日期", prop: "signatureDate" },
              { label: "创建时间", prop: "createTime", width: 20 },
            ],
            fileName: "订单数据",
            sheetName: "订单数据",
          })
            .then((count) => {
              this.$message.success(`导出成功，共 ${count} 条数据`);
              this.exportLoading = false;
            })
            .catch((err) => {
              if (err.message === "EMPTY_DATA") {
                this.$message.warning("没有可导出的数据");
              } else {
                console.error("导出订单数据失败：", err);
                this.$message.error("导出失败，请稍后重试");
              }
              this.exportLoading = false;
            });
        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped>
.drawer-toolbar {
  padding: 0 20px 10px;
  border-bottom: 1px solid #ebeef5;
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
