<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="协议名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入协议名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="版本号" prop="version">
        <el-input
          v-model="queryParams.version"
          placeholder="请输入版本号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建人" prop="createBy">
        <el-input
          v-model="queryParams.createBy"
          placeholder="请输入创建人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="协议状态"
          clearable
        >
          <el-option label="未生效" value="false" />
          <el-option label="生效中" value="true" />
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
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery"
          >重置</el-button
        >
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
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
      <right-toolbar
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <!-- 协议列表 -->
    <el-table v-loading="loading" :data="contractList">
      <el-table-column
        label="协议名称"
        align="center"
        prop="name"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="版本号"
        align="center"
        prop="version"
        width="120"
      />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status ? 'success' : 'info'">
            {{ scope.row.status ? "生效中" : "未生效" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="协议文件"
        align="center"
        prop="filePath"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <el-link
            type="primary"
            :href="baseUrl + scope.row.filePath"
            target="_blank"
          >
            {{ getFileName(scope.row.filePath) }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        label="创建人"
        align="center"
        prop="createBy"
        width="120"
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
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
        width="220"
      >
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            >修改</el-button
          >
          <el-button
            v-if="!scope.row.status"
            size="mini"
            type="text"
            icon="el-icon-circle-check"
            @click="handleEnable(scope.row)"
            >设为生效</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            >删除</el-button
          >
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

    <!-- 添加或修改协议对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
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
        <el-form-item label="协议文件" prop="file">
          <el-upload
            ref="fileUpload"
            action="#"
            :before-upload="handleBeforeUpload"
            :on-exceed="handleExceed"
            :limit="1"
            accept=".pdf"
            :show-file-list="true"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button slot="trigger" size="small" type="primary"
              >选取文件</el-button
            >
            <div slot="tip" class="el-upload__tip">仅支持上传 PDF 格式文件</div>
          </el-upload>
          <div v-if="form.filePath" style="margin-top: 10px">
            <el-link
              type="primary"
              :href="baseUrl + form.filePath"
              target="_blank"
            >
              查看当前文件: {{ getFileName(form.filePath) }}
            </el-link>
          </div>
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

export default {
  name: "Contract",
  data() {
    return {
      // 基础路径
      baseUrl: process.env.VUE_APP_BASE_API,
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 协议表格数据
      contractList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 上传的文件
      uploadFile: null,
      // 提交按钮 loading
      submitLoading: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined,
        version: undefined,
        createBy: undefined,
        status: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "协议名称不能为空", trigger: "blur" },
        ],
        version: [
          { required: true, message: "版本号不能为空", trigger: "blur" },
        ],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询协议列表 */
    getList() {
      this.loading = true;
      getContractList(this.queryParams).then((response) => {
        this.contractList = response.data.rows;
        this.total = response.data.total;
        this.loading = false;
      });
    },
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
        filePath: undefined,
      };
      this.uploadFile = null;
      this.resetForm("form");
      // 清空上传组件
      if (this.$refs.fileUpload) {
        this.$refs.fileUpload.clearFiles();
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
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
        filePath: row.filePath,
      };
      this.open = true;
      this.title = "修改协议";
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (!valid) return;

        // 防止重复提交
        this.submitLoading = true;

        // 构建FormData
        const formData = new FormData();
        const contractDto = {
          name: this.form.name,
          version: this.form.version,
        };

        if (this.form.id) {
          // 修改
          contractDto.id = this.form.id;
          formData.append(
            "contractDto",
            new Blob([JSON.stringify(contractDto)], {
              type: "application/json",
            })
          );
          if (this.uploadFile) {
            formData.append("file", this.uploadFile);
          }
          updateContract(formData)
            .then((response) => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            })
            .finally(() => {
              this.submitLoading = false;
            });
        } else {
          // 新增
          if (!this.uploadFile) {
            this.$modal.msgError("请上传协议文件");
            this.submitLoading = false;
            return;
          }
          formData.append(
            "contractDto",
            new Blob([JSON.stringify(contractDto)], {
              type: "application/json",
            })
          );
          formData.append("file", this.uploadFile);
          addContract(formData)
            .then((response) => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
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
          this.getList();
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
          this.getList();
          this.$modal.msgSuccess("设置成功");
        })
        .catch(() => {});
    },
    /** 文件状态改变时触发 */
    handleFileChange(file, fileList) {
      if (fileList.length > 0) {
        const fileName = file.name.toLowerCase();
        const isTypeOk = fileName.endsWith(".pdf");
        if (!isTypeOk) {
          this.$modal.msgError("仅支持上传 PDF 格式文件!");
          this.$refs.fileUpload.clearFiles();
          return;
        }
        this.uploadFile = file.raw;
      }
    },
    /** 文件移除时触发 */
    handleFileRemove() {
      this.uploadFile = null;
    },
    /** 上传前校检 */
    handleBeforeUpload(file) {
      const fileExt = file.name.split(".").pop().toLowerCase();
      const isTypeOk = fileExt === "pdf";
      if (!isTypeOk) {
        this.$modal.msgError("仅支持上传 PDF 格式文件!");
        return false;
      }
      this.uploadFile = file;
      return false; // 阻止自动上传，手动控制
    },
    /** 文件超出个数限制 */
    handleExceed() {
      this.$modal.msgError("只能上传一个文件!");
    },
    /** 获取文件名 */
    getFileName(filePath) {
      if (!filePath) return "";
      if (filePath.lastIndexOf("/") > -1) {
        return filePath.slice(filePath.lastIndexOf("/") + 1);
      }
      return filePath;
    },
  },
};
</script>

<style scoped>
.el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
