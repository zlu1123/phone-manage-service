<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="90px"
    >
      <el-form-item label="序列号" prop="sn">
        <el-input
          v-model="queryParams.sn"
          placeholder="请输入序列号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机品牌" prop="phoneType">
        <el-select
          v-model="queryParams.phoneType"
          placeholder="请选择手机品牌"
          clearable
        >
          <el-option
            v-for="item in phoneTypeList"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建者" prop="createBy">
        <el-input
          v-model="queryParams.createBy"
          placeholder="请输入创建者"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="激活状态" prop="activated">
        <el-select
          v-model="queryParams.activated"
          placeholder="请选择激活状态"
          clearable
        >
          <el-option label="已激活" :value="true" />
          <el-option label="未激活" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="mini"
          @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="el-icon-refresh" size="mini" @click="handleResetQuery"
          >重置</el-button
        >
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <right-toolbar
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="orderList">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column
        label="手机品牌"
        align="center"
        prop="phoneType"
        min-width="120"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <span>{{ getPhoneTypeName(scope.row.phoneType) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="手机型号"
        align="center"
        prop="model"
        min-width="120"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.model || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="序列号"
        align="center"
        prop="sn"
        min-width="130"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="IMEI1"
        align="center"
        prop="imei1"
        min-width="130"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.imei1 || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="IMEI2"
        align="center"
        prop="imei2"
        min-width="130"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.imei2 || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="激活状态"
        align="center"
        prop="activated"
        width="90"
      >
        <template slot-scope="scope">
          <el-tag :type="scope.row.activated ? 'success' : 'info'" size="small">
            {{ scope.row.activated ? "已激活" : "未激活" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="激活日期"
        align="center"
        prop="activateDate"
        width="110"
      />
      <el-table-column
        label="保修到期"
        align="center"
        prop="coverage"
        width="110"
      />
      <el-table-column
        label="系统时间"
        align="center"
        prop="sysTime"
        width="110"
      />
      <el-table-column
        label="创建者"
        align="center"
        prop="createBy"
        width="100"
      />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
      >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { queryOrderList, queryPhoneTypeList } from "@/api/order/list";

export default {
  name: "Order",
  data() {
    return {
      // 遮罩层
      loading: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 订单表格数据
      orderList: [],
      // 手机品牌列表
      phoneTypeList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        sn: undefined,
        phoneType: undefined,
        createBy: undefined,
        activated: undefined,
      },
    };
  },
  created() {
    this.getPhoneTypeList();
    this.getList();
  },
  methods: {
    /** 查询订单列表 */
    getList() {
      this.loading = true;
      queryOrderList(this.queryParams)
        .then((response) => {
          // 兼容两种返回格式：response.data.rows 或 response.rows
          const data = response.data || response;
          this.orderList = data.rows || [];
          this.total = data.total || 0;
          this.loading = false;
        })
        .catch((err) => {
          console.error("查询订单列表失败：", err);
          this.loading = false;
        });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    handleResetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 获取手机品牌列表 */
    getPhoneTypeList() {
      queryPhoneTypeList()
        .then((response) => {
          this.phoneTypeList = response.data || [];
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
  },
};
</script>
