<template>
  <div class="app-container">
    <pro-table
      ref="proTable"
      :fetch-api="getListApi"
      :search-fields="searchFields"
      :columns="columns"
    >
      <!-- 工具栏 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['system:leaveInfo:add']"
            >新增</el-button
          >
        </el-col>
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
          @click="handleUpdate(row)"
          v-hasPermi="['system:leaveInfo:edit']"
          >修改</el-button
        >
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          @click="handleDelete(row)"
          v-hasPermi="['system:leaveInfo:remove']"
          >删除</el-button
        >
      </template>
    </pro-table>

    <!-- 新增/修改对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="100px"
        size="small"
      >
        <el-form-item label="留资人姓名" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入留资人姓名"
            clearable
          />
        </el-form-item>
        <el-form-item label="留资人电话" prop="phoneNum">
          <el-input
            v-model="form.phoneNum"
            placeholder="请输入留资人电话"
            clearable
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="dialogVisible = false">取 消</el-button>
        <el-button
          type="primary"
          size="small"
          :loading="submitLoading"
          @click="handleSubmit"
          >确 定</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getLeaveInfoList,
  addLeaveInfo,
  updateLeaveInfo,
  deleteLeaveInfo,
} from "@/api/order/user";

export default {
  name: "LeaveInfoUser",
  data() {
    return {
      // 对话框相关
      dialogVisible: false,
      dialogTitle: "",
      submitLoading: false,
      // 表单数据
      form: {},
      // 是否编辑模式
      isEdit: false,
      // 列表请求 API
      getListApi: getLeaveInfoList,
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: "请输入留资人姓名", trigger: "blur" },
        ],
        phoneNum: [
          { required: true, message: "请输入留资人电话", trigger: "blur" },
        ],
      },
      // 搜索字段配置
      searchFields: [
        { prop: "name", label: "留资人姓名", type: "input" },
        { prop: "phoneNum", label: "留资人电话", type: "input" },
      ],
      // 表格列配置
      columns: [
        { label: "ID", prop: "id", width: "80" },
        { label: "留资人姓名", prop: "name", minWidth: "120" },
        { label: "留资人电话", prop: "phoneNum", minWidth: "130" },
        {
          label: "创建时间",
          prop: "createTime",
          slot: "createTime",
          width: "180",
        },
        {
          label: "操作",
          slot: "action",
          width: "150",
          fixed: "right",
          showOverflowTooltip: false,
        },
      ],
    };
  },
  methods: {
    /** 重置表单 */
    resetForm() {
      this.form = {
        id: undefined,
        name: "",
        phoneNum: "",
      };
      this.isEdit = false;
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate();
        }
      });
    },

    /** 新增按钮操作 */
    handleAdd() {
      this.resetForm();
      this.dialogTitle = "新增留资用户";
      this.dialogVisible = true;
    },

    /** 修改按钮操作 */
    handleUpdate(row) {
      this.resetForm();
      this.dialogTitle = "修改留资用户";
      this.isEdit = true;
      this.form = {
        id: row.id,
        name: row.name,
        phoneNum: row.phoneNum,
      };
      this.dialogVisible = true;
    },

    /** 提交表单 */
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        this.submitLoading = true;
        const api = this.isEdit ? updateLeaveInfo : addLeaveInfo;
        api(this.form)
          .then(() => {
            this.$message.success(this.isEdit ? "修改成功" : "新增成功");
            this.dialogVisible = false;
            this.$refs.proTable.refresh();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      this.$confirm(`是否确认删除留资用户"${row.name}"？`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          return deleteLeaveInfo(row.id);
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
