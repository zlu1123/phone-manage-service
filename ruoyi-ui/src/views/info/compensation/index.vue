<template>
  <div class="app-container">
    <pro-table
      ref="proTable"
      :fetch-api="getListApi"
      :search-fields="searchFields"
      :columns="columns"
    >
      <!-- 工具栏：新增按钮 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            >新增</el-button
          >
        </el-col>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <el-tag :type="getStatusTagType(row.status)" size="small">
          {{ getStatusText(row.status) }}
        </el-tag>
      </template>

      <!-- 赔付金额列 -->
      <template #amount="{ row }">
        <span>{{ row.amount != null ? "¥" + row.amount : "-" }}</span>
      </template>

      <!-- 创建时间列 -->
      <template #createTime="{ row }">
        <span>{{ parseTime(row.createTime) }}</span>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <el-button
          size="mini"
          type="text"
          icon="el-icon-edit"
          @click="handleReview(row)"
          >审核</el-button
        >
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          @click="handleDelete(row)"
          >删除</el-button
        >
      </template>
    </pro-table>

    <!-- 新增对话框 -->
    <el-dialog
      title="新增赔付订单"
      :visible.sync="addDialogVisible"
      width="450px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="addForm"
        :model="addForm"
        :rules="addRules"
        label-width="100px"
        size="small"
      >
        <el-form-item label="订单ID" prop="orderId">
          <el-input
            v-model.number="addForm.orderId"
            placeholder="请输入订单ID"
            clearable
          />
        </el-form-item>
        <el-form-item label="留资人ID" prop="infoId">
          <el-input
            v-model.number="addForm.infoId"
            placeholder="请输入留资人ID"
            clearable
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="addDialogVisible = false"
          >取 消</el-button
        >
        <el-button
          type="primary"
          size="small"
          :loading="submitLoading"
          @click="handleAddSubmit"
          >确 定</el-button
        >
      </div>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      title="审核赔付订单"
      :visible.sync="reviewDialogVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="reviewForm"
        :model="reviewForm"
        :rules="reviewRules"
        label-width="100px"
        size="small"
      >
        <el-form-item label="赔付金额" prop="amount">
          <el-input-number
            v-model="reviewForm.amount"
            :precision="2"
            :min="0"
            :max="999999.99"
            placeholder="请输入赔付金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="审核状态" prop="status">
          <el-select
            v-model="reviewForm.status"
            placeholder="请选择审核状态"
            style="width: 100%"
          >
            <el-option label="审核通过" :value="1" />
            <el-option label="审核不通过" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="reviewForm.status === 2"
          label="拒绝原因"
          prop="rejectionReason"
        >
          <el-input
            v-model="reviewForm.rejectionReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="reviewForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="reviewDialogVisible = false"
          >取 消</el-button
        >
        <el-button
          type="primary"
          size="small"
          :loading="submitLoading"
          @click="handleReviewSubmit"
          >确 定</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getCompensationList,
  addCompensation,
  updateCompensation,
  deleteCompensation,
} from "@/api/order/compensation";

export default {
  name: "CompensationOrder",
  data() {
    return {
      // 列表请求 API
      getListApi: getCompensationList,
      // 新增对话框
      addDialogVisible: false,
      addForm: {},
      addRules: {
        orderId: [
          { required: true, message: "请输入订单ID", trigger: "blur" },
          { type: "number", message: "订单ID必须为数字", trigger: "blur" },
        ],
        infoId: [
          { required: true, message: "请输入留资人ID", trigger: "blur" },
          { type: "number", message: "留资人ID必须为数字", trigger: "blur" },
        ],
      },
      // 审核对话框
      reviewDialogVisible: false,
      reviewForm: {},
      reviewRules: {
        amount: [
          { required: true, message: "请输入赔付金额", trigger: "blur" },
        ],
        status: [
          { required: true, message: "请选择审核状态", trigger: "change" },
        ],
        rejectionReason: [
          { required: true, message: "请输入拒绝原因", trigger: "blur" },
        ],
      },
      // 提交按钮loading
      submitLoading: false,
      // 搜索字段配置
      searchFields: [
        { prop: "name", label: "留资人姓名", type: "input" },
        { prop: "phoneNum", label: "留资人电话", type: "input" },
        { prop: "signatureImei", label: "签约IMEI", type: "input" },
        { prop: "createBy", label: "创建人", type: "input" },
      ],
      // 表格列配置
      columns: [
        { label: "ID", prop: "id", width: "60" },
        { label: "订单ID", prop: "orderId", width: "80" },
        { label: "留资人ID", prop: "infoId", width: "90" },
        { label: "留资人姓名", prop: "name", minWidth: "100" },
        { label: "留资人电话", prop: "phoneNum", minWidth: "120" },
        { label: "签约IMEI", prop: "signatureImei", minWidth: "140" },
        { label: "赔付金额", prop: "amount", slot: "amount", width: "110" },
        { label: "状态", prop: "status", slot: "status", width: "100" },
        {
          label: "拒绝原因",
          prop: "rejectionReason",
          minWidth: "150",
          showOverflowTooltip: true,
        },
        {
          label: "备注",
          prop: "remark",
          minWidth: "120",
          showOverflowTooltip: true,
        },
        { label: "创建人", prop: "createBy", width: "100" },
        {
          label: "创建时间",
          prop: "createTime",
          slot: "createTime",
          width: "160",
        },
        {
          label: "操作",
          slot: "action",
          width: "120",
          fixed: "right",
          showOverflowTooltip: false,
        },
      ],
    };
  },
  methods: {
    /** 获取状态文本 */
    getStatusText(status) {
      const map = { 0: "待审核", 1: "审核通过", 2: "审核不通过" };
      return map[Number(status)] || "未知";
    },
    /** 获取状态标签类型 */
    getStatusTagType(status) {
      const map = { 0: "info", 1: "success", 2: "danger" };
      return map[Number(status)] || "info";
    },
    /** 重置新增表单 */
    resetAddForm() {
      this.addForm = {
        orderId: undefined,
        infoId: undefined,
      };
      this.$nextTick(() => {
        if (this.$refs.addForm) {
          this.$refs.addForm.clearValidate();
        }
      });
    },
    /** 重置审核表单 */
    resetReviewForm() {
      this.reviewForm = {
        id: undefined,
        amount: undefined,
        status: undefined,
        rejectionReason: "",
        remark: "",
      };
      this.$nextTick(() => {
        if (this.$refs.reviewForm) {
          this.$refs.reviewForm.clearValidate();
        }
      });
    },
    /** 新增按钮 */
    handleAdd() {
      this.resetAddForm();
      this.addDialogVisible = true;
    },
    /** 提交新增 */
    handleAddSubmit() {
      this.$refs.addForm.validate((valid) => {
        if (!valid) return;
        this.submitLoading = true;
        addCompensation(this.addForm)
          .then(() => {
            this.$message.success("新增成功");
            this.addDialogVisible = false;
            this.$refs.proTable.refresh();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    /** 审核按钮 */
    handleReview(row) {
      this.resetReviewForm();
      // 打开审核对话框时不预填充审核状态，由管理员主动选择
      this.reviewForm = {
        id: row.id,
        amount: row.amount,
        rejectionReason: row.rejectionReason || "",
        remark: row.remark || "",
      };
      this.reviewDialogVisible = true;
    },
    /** 提交审核 */
    handleReviewSubmit() {
      this.$refs.reviewForm.validate((valid) => {
        if (!valid) return;
        this.submitLoading = true;
        updateCompensation(this.reviewForm)
          .then(() => {
            this.$message.success("审核提交成功");
            this.reviewDialogVisible = false;
            this.$refs.proTable.refresh();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    /** 删除按钮 */
    handleDelete(row) {
      this.$confirm(`是否确认删除该赔付订单（ID：${row.id}）？`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          return deleteCompensation(row.id);
        })
        .then(() => {
          this.$message.success("删除成功");
          this.$refs.proTable.refresh();
        })
        .catch(() => {});
    },
  },
};
</script>
