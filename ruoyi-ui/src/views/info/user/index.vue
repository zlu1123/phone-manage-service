<template>
  <div class="app-container">
    <pro-table ref="proTableRef" :fetch-api="getListApi" :search-fields="searchFields" :columns="columns">
      <!-- 工具栏 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            :icon="Plus"
            size="small"
            @click="handleAdd"
            v-hasRole="['admin', 'user']"
            >新增</el-button
          >
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="info"
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
            type="warning"
            plain
            :icon="Download"
            size="small"
            @click="handleExport"
            v-hasRole="['admin', 'user']"
            >导出</el-button
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
          size="small"
          type="primary"
          link
          :icon="Edit"
          @click="handleUpdate(row)"
          v-hasRole="['admin', 'user']"
          >修改</el-button
        >
        <el-button
          size="small"
          type="danger"
          link
          :icon="Delete"
          @click="handleDelete(row)"
          v-hasRole="['admin', 'user']"
          >删除</el-button
        >
      </template>
    </pro-table>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="留资人姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入留资人姓名" clearable />
        </el-form-item>
        <el-form-item label="留资人电话" prop="phoneNum">
          <el-input v-model="form.phoneNum" placeholder="请输入留资人电话" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" size="small" :loading="submitLoading" @click="handleSubmit">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 留资用户导入对话框 -->
    <excel-import-dialog
      ref="importUserRef"
      title="留资用户导入"
      action="/system/leaveInfo/importData"
      template-action="/system/leaveInfo/importTemplate"
      template-file-name="leave_user_template"
      update-support-label="是否更新已经存在的留资用户"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, getCurrentInstance } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete, Upload, Download } from '@element-plus/icons-vue';
import ExcelImportDialog from '@/components/ExcelImportDialog';
import { getLeaveInfoList, addLeaveInfo, updateLeaveInfo, deleteLeaveInfo } from '@/api/order/user';

const { proxy } = getCurrentInstance();

// 对话框相关
const dialogVisible = ref(false);
const dialogTitle = ref('');
const submitLoading = ref(false);
const importUserRef = ref(null);

// 表单数据
const form = reactive({
  id: undefined,
  name: '',
  phoneNum: '',
});

// 是否编辑模式
const isEdit = ref(false);

// 列表请求 API
const getListApi = getLeaveInfoList;

// 表单引用
const formRef = ref(null);
const proTableRef = ref(null);

// 表单校验规则
const rules = {
  name: [{ required: true, message: '请输入留资人姓名', trigger: 'blur' }],
  phoneNum: [{ required: true, message: '请输入留资人电话', trigger: 'blur' }],
};

// 搜索字段配置
const searchFields = [
  { prop: 'name', label: '留资人姓名', type: 'input' },
  { prop: 'phoneNum', label: '留资人电话', type: 'input' },
];

// 表格列配置
const columns = [
  { label: 'ID', prop: 'id', width: '80' },
  { label: '留资人姓名', prop: 'name', minWidth: '120' },
  { label: '留资人电话', prop: 'phoneNum', minWidth: '130' },
  {
    label: '创建时间',
    prop: 'createTime',
    slot: 'createTime',
    width: '180',
  },
  {
    label: '操作',
    slot: 'action',
    width: '150',
    fixed: 'right',
    showOverflowTooltip: false,
  },
];

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.name = '';
  form.phoneNum = '';
  isEdit.value = false;
  nextTick(() => {
    if (formRef.value) {
      formRef.value.clearValidate();
    }
  });
};

/** 新增按钮操作 */
const handleAdd = () => {
  resetForm();
  dialogTitle.value = '新增留资用户';
  dialogVisible.value = true;
};

/** 修改按钮操作 */
const handleUpdate = (row) => {
  resetForm();
  dialogTitle.value = '修改留资用户';
  isEdit.value = true;
  form.id = row.id;
  form.name = row.name;
  form.phoneNum = row.phoneNum;
  dialogVisible.value = true;
};

/** 提交表单 */
const handleSubmit = () => {
  formRef.value.validate((valid) => {
    if (!valid) return;
    submitLoading.value = true;
    const api = isEdit.value ? updateLeaveInfo : addLeaveInfo;
    api(form)
      .then(() => {
        ElMessage.success(isEdit.value ? '修改成功' : '新增成功');
        dialogVisible.value = false;
        proTableRef.value.refresh();
      })
      .finally(() => {
        submitLoading.value = false;
      });
  });
};

/** 删除按钮操作 */
const handleDelete = (row) => {
  ElMessageBox.confirm(`是否确认删除留资用户"${row.name}"？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      return deleteLeaveInfo(row.id);
    })
    .then(() => {
      ElMessage.success('删除成功');
      proTableRef.value.refresh();
    })
    .catch(() => {});
};

/** 导入按钮操作 */
const handleImport = () => {
  importUserRef.value?.open();
};

/** 导入成功后刷新列表 */
const handleImportSuccess = () => {
  proTableRef.value.refresh();
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy.download(
    'system/leaveInfo/export',
    {
      ...proTableRef.value?.getQueryParams(),
    },
    `leave_user_${new Date().getTime()}.xlsx`,
  );
};
</script>
