<template>
  <div class="wang-editor">
    <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" :mode="mode" style="border-bottom: 1px solid #ccc" />
    <Editor
      :defaultConfig="editorConfig"
      :mode="mode"
      v-model="valueHtml"
:style="{ height: height + 'px', overflowY: 'hidden' }"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount, markRaw } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage } from 'element-plus'

const props = defineProps({
  value: { type: String, default: '' },
  readOnly: { type: Boolean, default: false },
  height: { type: Number, default: 300 },
  minHeight: { type: Number, default: 300 },
})

const emit = defineEmits(['input', 'change'])

const editorRef = ref(null)
const valueHtml = ref(props.value)
const mode = 'default'

const toolbarConfig = {
  excludeKeys: ['uploadImage', 'uploadVideo', 'insertImage', 'insertVideo', 'codeBlock'],
}

const editorConfig = {
  placeholder: '请输入协议内容...',
  readOnly: props.readOnly,
  MENU_CONF: {
    insertImage: {
      checkImage(src) {
        return true
      },
    },
  },
  customAlert(s, t) {
    switch (t) {
      case 'success':
        ElMessage.success(s)
        break
      case 'info':
        ElMessage.info(s)
        break
      case 'warning':
        ElMessage.warning(s)
        break
      case 'error':
        ElMessage.error(s)
        break
      default:
        ElMessage.info(s)
        break
    }
  },
}

watch(
  () => props.value,
  (val) => {
    valueHtml.value = val
  }
)

function handleCreated(editor) {
  editorRef.value = markRaw(editor)
}

function handleChange() {
  emit('input', valueHtml.value)
  emit('change', valueHtml.value)
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})
</script>

<style scoped>
.wang-editor {
  border: 1px solid #ccc;
  border-radius: 4px;
}
</style>
