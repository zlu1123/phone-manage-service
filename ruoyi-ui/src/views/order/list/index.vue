<template>
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
      <el-link
        v-if="row.contractPath"
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
