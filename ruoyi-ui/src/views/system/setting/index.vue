<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <span>系统时间设置</span>
        </div>
      </template>
      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="100px">
        <el-form-item label="系统时间" prop="configValue">
          <!-- 只读展示模式：只取日期部分 -->
          <span v-if="!isEditing" class="sys-time-display">{{ displayDate || '暂未设置' }}</span>
          <!-- 编辑模式：点击修改后创建，默认值使用接口返回的系统时间 -->
          <el-date-picker
            v-if="isEditing"
            v-model="pickerDate"
            type="date"
            placeholder="请选择系统时间"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <template v-if="!isEditing">
            <el-button type="primary" :icon="Edit" @click="handleEdit">修改</el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="handleSubmit">确认</el-button>
            <el-button @click="handleCancel">取消</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="Setting">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { getConfigKey, updateSysTime } from '@/api/system/config'

const formRef = ref(null)
const isEditing = ref(false)
const configValue = ref('')
const originalValue = ref('')
const pickerDate = ref(null)

const formModel = computed(() => ({
  configKey: 'sys.time',
  configValue: configValue.value,
}))

/** 提取日期部分（yyyy-MM-dd），兼容完整时间戳格式 */
function extractDate(val) {
  if (!val) return ''
  return String(val).substring(0, 10)
}

/** 将 Date 对象格式化为 yyyy-MM-dd */
function formatDate(date) {
  if (!date) return ''
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 只读展示的日期字符串 */
const displayDate = computed(() => extractDate(configValue.value))

const rules = {
  configValue: [{ required: true, message: '请选择系统时间', trigger: 'change' }],
}

/** 获取当前系统时间 */
function getSysTime() {
  getConfigKey('sys.time').then((response) => {
    configValue.value = response.msg
    originalValue.value = response.msg
  })
}

/** 进入编辑模式 */
function handleEdit() {
  const dateStr = extractDate(originalValue.value)
  pickerDate.value = dateStr ? new Date(dateStr) : null
  isEditing.value = true
}

/** 确认提交 */
function handleSubmit() {
  if (!formRef.value) return
  // 将 Date 对象格式化为 yyyy-MM-dd 字符串
  configValue.value = formatDate(pickerDate.value)
  formRef.value.validate((valid) => {
    if (!valid) return

    ElMessageBox.confirm(
      '是否确认修改系统时间为 "' + configValue.value + '" ？',
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
      .then(() => updateSysTime({ configKey: 'sys.time', configValue: configValue.value }))
      .then(() => {
        ElMessage.success('修改成功')
        originalValue.value = configValue.value
        isEditing.value = false
      })
      .catch(() => {})
  })
}

/** 取消编辑，恢复原始值 */
function handleCancel() {
  isEditing.value = false
}

getSysTime()
</script>

<style scoped>
</style>
