<template>
  <div class="app-container">
    <ProTable
      ref="proTable"
      :fetch-api="getContractList"
      :search-fields="searchFields"
      :columns="columns"
      search-label-width="68px"
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
        <el-tag :type="row.status ? 'success' : 'info'">
          {{ row.status ? "生效中" : "未生效" }}
        </el-tag>
      </template>

      <!-- 协议内容列 -->
      <template #content="{ row }">
        <el-button
          v-if="row.content"
          size="mini"
          type="text"
          icon="el-icon-view"
          @click="handleViewContent(row)"
          >查看</el-button
        >
        <el-tag v-else type="info" size="mini">暂无内容</el-tag>
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
          >修改</el-button
        >
        <el-button
          v-if="!row.status"
          size="mini"
          type="text"
          icon="el-icon-circle-check"
          @click="handleEnable(row)"
          >设为生效</el-button
        >
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          @click="handleDelete(row)"
          >删除</el-button
        >
      </template>
    </ProTable>

    <!-- 协议内容预览抽屉 -->
    <el-drawer
      :title="drawerTitle"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="50%"
      append-to-body
    >
      <div class="contract-content-wrapper" v-html="drawerContent"></div>
    </el-drawer>

    <!-- 添加或修改协议对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="协议名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入协议名称" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input
            v-model="form.version"
            placeholder="请输入版本号，如：v1.0.0"
          />
        </el-form-item>
        <el-form-item label="协议内容" prop="content">
          <WangEditor v-model="form.content" :height="300" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitLoading" @click="submitForm"
          >确 定</el-button
        >
        <el-button @click="cancel" :disabled="submitLoading">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getContractList,
  addContract,
  updateContract,
  deleteContract,
  updateContractStatus,
} from "@/api/system/contract";
import ProTable from "@/components/ProTable/index.vue";
import WangEditor from "@/components/WangEditor/index.vue";
import { defaultContractContent } from "./defaultContract";

export default {
  name: "Contract",
  components: { ProTable, WangEditor },
  data() {
    return {
      // 接口方法
      getContractList,
      // 搜索字段配置
      searchFields: [
        { prop: "name", label: "协议名称", type: "input" },
        { prop: "version", label: "版本号", type: "input" },
        { prop: "createBy", label: "创建人", type: "input" },
        {
          prop: "status",
          label: "状态",
          type: "select",
          placeholder: "协议状态",
          options: [
            { label: "未生效", value: "false" },
            { label: "生效中", value: "true" },
          ],
        },
      ],
      // 表格列配置
      columns: [
        { label: "协议名称", prop: "name", minWidth: 200 },
        { label: "版本号", prop: "version", width: 189 },
        { label: "状态", prop: "status", width: 100, slot: "status" },
        { label: "协议内容", prop: "content", width: 100, slot: "content" },
        { label: "创建人", prop: "createBy", width: 120 },
        {
          label: "创建时间",
          prop: "createTime",
          width: 200,
          slot: "createTime",
        },
        {
          label: "操作",
          prop: "action",
          width: 300,
          slot: "action",
          showOverflowTooltip: false,
          attrs: { "class-name": "small-padding fixed-width" },
        },
      ],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 抽屉相关
      drawerVisible: false,
      drawerTitle: "",
      drawerContent: "",
      // 提交按钮 loading
      submitLoading: false,
      // 表单参数
      form: {
        content: "",
      },
      // 表单校验
      rules: {
        name: [
          { required: true, message: "协议名称不能为空", trigger: "blur" },
        ],
        version: [
          { required: true, message: "版本号不能为空", trigger: "blur" },
        ],
        content: [
          { required: true, message: "协议内容不能为空", trigger: "blur" },
        ],
      },
    };
  },
  methods: {
    // 取消按钮
    cancel() {
      this.open = false;
      this.submitLoading = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        version: undefined,
        content: undefined,
      };
      this.resetForm("form");
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.form.name = "全心宝保障服务协议";
      this.form.version = "v1.0.0";
      this.form.content = defaultContractContent;
      this.open = true;
      this.title = "添加协议";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.form = {
        id: row.id,
        name: row.name,
        version: row.version,
        content: row.content || "",
      };
      this.open = true;
      this.title = "修改协议";
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (!valid) return;

        this.submitLoading = true;

        const contractDto = {
          id: this.form.id,
          name: this.form.name,
          version: this.form.version,
          content: this.form.content,
        };
        const formData = new FormData();
        formData.append(
          "contractDto",
          new Blob([JSON.stringify(contractDto)], {
            type: "application/json",
          })
        );

        if (this.form.id) {
          updateContract(formData)
            .then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.$refs.proTable.refresh();
            })
            .finally(() => {
              this.submitLoading = false;
            });
        } else {
          addContract(formData)
            .then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.$refs.proTable.refresh();
            })
            .finally(() => {
              this.submitLoading = false;
            });
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal
        .confirm('是否确认删除协议 "' + row.name + '" ？')
        .then(() => {
          return deleteContract(row.id);
        })
        .then(() => {
          this.$refs.proTable.refresh();
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
    /** 设为生效 */
    handleEnable(row) {
      this.$modal
        .confirm(
          '是否确认将协议 "' +
            row.name +
            '" 设为生效状态？\n注意：设置后其他协议将自动设为未生效'
        )
        .then(() => {
          return updateContractStatus({
            id: row.id.toString(),
            status: "true",
          });
        })
        .then(() => {
          this.$refs.proTable.refresh();
          this.$modal.msgSuccess("设置成功");
        })
        .catch(() => {});
    },
    /** 查看协议内容 */
    handleViewContent(row) {
      this.drawerTitle = row.name || "协议内容";
      this.drawerContent = row.content || "";
      this.drawerVisible = true;
    },
  },
};
</script>

<style scoped>
.contract-content-wrapper {
  padding: 20px;
  line-height: 1.8;
  font-size: 14px;
  color: #333;
  overflow-y: auto;
  height: calc(100% - 20px);
}
.contract-content-wrapper h2,
.contract-content-wrapper h3 {
  margin: 16px 0 8px;
  color: #1a1a1a;
}
.contract-content-wrapper p {
  margin: 8px 0;
}
</style>
