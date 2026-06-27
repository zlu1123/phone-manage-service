<template>
  <div class="app-container">
    <ProTable
      ref="proTableRef"
      :fetch-api="getContractList"
      :search-fields="searchFields"
      :columns="columns"
      search-label-width="68px"
    >
      <!-- 工具栏：新增按钮 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-button type="primary" plain :icon="Plus" size="small" @click="handleAdd">新增</el-button>
        </el-col>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <el-tag :type="row.status === 'true' || row.status === true ? 'success' : 'info'">
          {{ row.status === 'true' || row.status === true ? '生效中' : '未生效' }}
        </el-tag>
      </template>

      <!-- 协议内容列 -->
      <template #content="{ row }">
        <el-button v-if="row.content" size="small" link :icon="View" @click="handleViewContent(row)">查看</el-button>
        <el-tag v-else type="info" size="small">暂无内容</el-tag>
      </template>

      <!-- 创建时间列 -->
      <template #createTime="{ row }">
        <span>{{ parseTime(row.createTime) }}</span>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <el-button size="small" link type="primary" :icon="Edit" @click="handleUpdate(row)">修改</el-button>
        <el-button v-if="row.status !== 'true' && row.status !== true" size="small" link type="success" :icon="CircleCheck" @click="handleEnable(row)">设为生效</el-button>
        <el-button size="small" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
      </template>
    </ProTable>

    <!-- 协议内容预览抽屉 -->
    <el-drawer :title="drawerTitle" v-model="drawerVisible" direction="rtl" size="50%" append-to-body>
      <div class="contract-content-wrapper" v-html="drawerContent"></div>
    </el-drawer>

    <!-- 添加或修改协议对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="协议名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入协议名称" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="请输入版本号，如：v1.0.0" />
        </el-form-item>
        <el-form-item label="协议内容" prop="content">
          <WangEditor :value="form.content" @input="form.content = $event" :height="300" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
          <el-button @click="cancel" :disabled="submitLoading">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Contract">
import { ref, reactive, getCurrentInstance, toRefs } from 'vue'
import { Plus, View, Edit, CircleCheck, Delete } from '@element-plus/icons-vue'
import {
  getContractList,
  addContract,
  updateContract,
  deleteContract,
  updateContractStatus
} from '@/api/system/contract'
import WangEditor from '@/components/WangEditor/index.vue'

const { proxy } = getCurrentInstance()

// 模板引用
const proTableRef = ref(null)
const formRef = ref(null)

const data = reactive({
  title: '',
  open: false,
  drawerVisible: false,
  drawerTitle: '',
  drawerContent: '',
  submitLoading: false,
  form: { content: '' },
  rules: {
    name: [{ required: true, message: '协议名称不能为空', trigger: 'blur' }],
    version: [{ required: true, message: '版本号不能为空', trigger: 'blur' }],
    content: [{ required: true, message: '协议内容不能为空', trigger: 'blur' }]
  },
  searchFields: [
    { prop: 'name', label: '协议名称', type: 'input' },
    { prop: 'version', label: '版本号', type: 'input' },
    { prop: 'createBy', label: '创建人', type: 'input' },
    {
      prop: 'status',
      label: '状态',
      type: 'select',
      placeholder: '协议状态',
      options: [
        { label: '未生效', value: 'false' },
        { label: '生效中', value: 'true' }
      ]
    }
  ],
  columns: [
    { label: '协议名称', prop: 'name', minWidth: 200 },
    { label: '版本号', prop: 'version', width: 189 },
    { label: '状态', prop: 'status', width: 100, slot: 'status' },
    { label: '协议内容', prop: 'content', width: 100, slot: 'content' },
    { label: '创建人', prop: 'createBy', width: 120 },
    { label: '创建时间', prop: 'createTime', width: 200, slot: 'createTime' },
    {
      label: '操作',
      prop: 'action',
      width: 300,
      slot: 'action',
      showOverflowTooltip: false,
      attrs: { 'class-name': 'small-padding fixed-width' }
    }
  ]
})

const { title, open, drawerVisible, drawerTitle, drawerContent, submitLoading, form, rules, searchFields, columns } = toRefs(data)

function cancel() {
  open.value = false
  submitLoading.value = false
  reset()
}

function reset() {
  form.value = { id: undefined, name: undefined, version: undefined, content: undefined }
  formRef.value?.resetFields()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加协议'
}

function handleUpdate(row) {
  reset()
  form.value = { id: row.id, name: row.name, version: row.version, content: row.content || '' }
  open.value = true
  title.value = '修改协议'
}

function submitForm() {
  if (!formRef.value) return
  formRef.value.validate((valid) => {
    if (!valid) return

    submitLoading.value = true

    const contractDto = {
      id: form.value.id,
      name: form.value.name,
      version: form.value.version,
      content: form.value.content
    }
    const submitData = new FormData()
    submitData.append('contractDto', new Blob([JSON.stringify(contractDto)], { type: 'application/json' }))

    const apiCall = form.value.id ? updateContract : addContract
    const successMsg = form.value.id ? '修改成功' : '新增成功'

    apiCall(submitData)
      .then(() => {
        proxy.$modal.msgSuccess(successMsg)
        open.value = false
        proTableRef.value?.refresh()
      })
      .finally(() => {
        submitLoading.value = false
      })
  })
}

function handleDelete(row) {
  proxy.$modal
    .confirm('是否确认删除协议 "' + row.name + '" ？')
    .then(() => deleteContract(row.id))
    .then(() => {
      proTableRef.value?.refresh()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

function handleEnable(row) {
  proxy.$modal
    .confirm('是否确认将协议 "' + row.name + '" 设为生效状态？\n注意：设置后其他协议将自动设为未生效')
    .then(() => updateContractStatus({ id: row.id, status: 'true' }))
    .then(() => {
      proTableRef.value?.refresh()
      proxy.$modal.msgSuccess('设置成功')
    })
    .catch(() => {})
}

function handleViewContent(row) {
  drawerTitle.value = row.name || '协议内容'
  drawerContent.value = row.content || ''
  drawerVisible.value = true
}
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
.editor-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>

<style scoped>
.contract-content-wrapper :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}
.contract-content-wrapper :deep(table) th,
.contract-content-wrapper :deep(table) td {
  border: 1px solid #ccc;
  padding: 8px 12px;
  text-align: left;
}
.contract-content-wrapper :deep(table) th {
  background-color: #f5f5f5;
  font-weight: bold;
}
</style>
