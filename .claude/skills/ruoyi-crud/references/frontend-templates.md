# 前端模板（Vue3 + Element Plus + pro-table，以 info/list/index.vue 与 system/contract/index.vue 为范本）

占位符同后端：`<Entity>`/`<entity>`/`<module>`（URL 前缀，如 system）/`<表注释>`。

## 1. API 封装（ruoyi-ui/src/api/<module>/<entity>.js）

```js
import request from '@/utils/request'

// 查询<表注释>列表（分页）
export function query<Entity>List(query) {
  return request({
    url: '/<module>/<entity>/getList',
    method: 'get',
    params: query
  })
}

// 查询<表注释>详情
export function get<Entity>Detail(id) {
  return request({
    url: '/<module>/<entity>/getDetail',
    method: 'get',
    params: { id }
  })
}

// 新增<表注释>
export function insert<Entity>(data) {
  return request({
    url: '/<module>/<entity>/insert',
    method: 'post',
    data: data
  })
}

// 更新<表注释>
export function update<Entity>(data) {
  return request({
    url: '/<module>/<entity>/update',
    method: 'post',
    data: data
  })
}

// 删除<表注释>
export function delete<Entity>(id) {
  return request({
    url: '/<module>/<entity>/delete/' + id,
    method: 'delete'
  })
}

// 导出<表注释>数据
export function export<Entity>List(query) {
  return request({
    url: '/<module>/<entity>/export',
    method: 'post',
    params: query
  })
}
```

## 2. 列表页（ruoyi-ui/src/views/<module>/<entity>/index.vue）

项目列表页统一用 `pro-table` 组件（fetch-api / search-fields / columns），新增编辑用 `el-dialog + el-form`，导入用 `ExcelImportDialog`：

```vue
<template>
  <div class="app-container">
    <pro-table
      ref="proTableRef"
      :fetch-api="fetchList"
      :search-fields="searchFields"
      :columns="columns"
      row-key="id"
    >
      <!-- 工具栏 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            :icon="Plus"
            size="small"
            @click="handleAdd"
            v-hasPermi="['<module>:<entity>:add']"
            >新增</el-button
          >
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="warning"
            plain
            :icon="Download"
            size="small"
            @click="handleExport"
            v-hasPermi="['<module>:<entity>:export']"
            >导出</el-button
          >
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="info"
            plain
            :icon="Upload"
            size="small"
            @click="importOpen = true"
            v-hasPermi="['<module>:<entity>:import']"
            >导入</el-button
          >
        </el-col>
      </template>

      <!-- 操作列 -->
      <template #operation="{ row }">
        <el-button size="small" type="primary" link :icon="Edit" @click="handleEdit(row)" v-hasPermi="['<module>:<entity>:edit']">编辑</el-button>
        <el-button size="small" type="danger" link :icon="Delete" @click="handleDelete(row)" v-hasPermi="['<module>:<entity>:remove']">删除</el-button>
      </template>
    </pro-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="<字段中文名>" prop="xxx">
          <el-input v-model="form.xxx" placeholder="请输入<字段中文名>" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入弹窗（action 必传） -->
    <ExcelImportDialog
      v-model:open="importOpen"
      title="<表注释>导入"
      :action="'/<module>/<entity>/importData'"
      :template-action="'/<module>/<entity>/importTemplate'"
      template-file-name="<表注释>导入模板"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete, Download, Upload } from '@element-plus/icons-vue';
import {
  query<Entity>List, get<Entity>Detail, insert<Entity>, update<Entity>, delete<Entity>, export<Entity>List
} from '@/api/<module>/<entity>';
import ExcelImportDialog from '@/components/ExcelImportDialog';

const { proxy } = getCurrentInstance();

// 搜索条件（pro-table 支持 type: input | select | date | daterange | slot）
const searchFields = [
  { prop: 'xxx', label: '<字段中文名>', type: 'input' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '全部', value: null },
      { label: '正常', value: 0 },
      { label: '停用', value: 1 },
    ],
  },
];

// 列定义（slot 为自定义列渲染，名称对应 template 里的 #xxx）
const columns = [
  { type: 'selection', width: '50' },
  { label: 'ID', prop: 'id', width: '60' },
  { label: '<字段中文名>', prop: 'xxx' },
  { label: '状态', prop: 'status', slot: 'status', width: '100' },
  { label: '创建时间', prop: 'createTime', width: '180' },
  { label: '操作', slot: 'operation', width: '140', fixed: 'right' },
];

const proTableRef = ref(null);

// 查询接口（参数自动带上分页 pageNum/pageSize）
function fetchList(params) {
  return query<Entity>List(params);
}

// 导出
function handleExport() {
  proxy.download('/<module>/<entity>/export', { ...proTableRef.value?.getQueryParams() }, `<表注释>_${new Date().getTime()}.xlsx`);
}

// 新增/编辑弹窗
const open = ref(false);
const title = ref('');
const formRef = ref(null);
const form = ref({ status: 0 });

const rules = {
  xxx: [{ required: true, message: '<字段中文名>不能为空', trigger: 'blur' }],
};

function reset() {
  form.value = { status: 0 };
  formRef.value?.resetFields();
}

function handleAdd() {
  reset();
  title.value = '新增<表注释>';
  open.value = true;
}

function handleEdit(row) {
  reset();
  title.value = '编辑<表注释>';
  form.value = { ...row };
  open.value = true;
}

function submitForm() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return;
    const api = form.value.id ? update<Entity> : insert<Entity>;
    api(form.value).then((res) => {
      if (res.code === 200) {
        ElMessage.success(res.msg || '操作成功');
        open.value = false;
        proTableRef.value?.reload();
      }
    });
  });
}

function handleDelete(row) {
  ElMessageBox.confirm(`确认删除「${row.id}」这条<表注释>吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    return delete<Entity>(row.id);
  }).then((res) => {
    if (res.code === 200) {
      ElMessage.success('删除成功');
      proTableRef.value?.reload();
    }
  }).catch(() => {});
}

// 导入
const importOpen = ref(false);
function handleImportSuccess() {
  importOpen.value = false;
  ElMessage.success('导入成功');
  proTableRef.value?.reload();
}
</script>
```

## 注意事项

- 列表接口返回格式为 `{code, rows, total, msg}`，`res.code === 200` 即成功（项目封装的 request 已统一处理 code !== 200 的报错弹窗，页面里成功分支可简化）
- 下载文件统一用 `proxy.download(url, params, fileName)`（全局混入的方法）
- 字典字段优先用 `useDict` 或 dict 组件（`@/utils/dict`），生成时问用户是否有对应字典类型
- 页面路径与菜单 SQL 的 `component` 字段必须一致：`<module>/<entity>/index`
- 若需要状态列自定义渲染，用 `<template #status="{ row }">` + el-tag，参考 info/list/index.vue
