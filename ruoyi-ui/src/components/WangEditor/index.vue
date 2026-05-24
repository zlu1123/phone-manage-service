<template>
  <div class="wang-editor">
    <Toolbar
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      :mode="mode"
      style="border-bottom: 1px solid #ccc"
    />
    <Editor
      :defaultConfig="editorConfig"
      :mode="mode"
      v-model="valueHtml"
      style="height: 300px; overflow-y: hidden"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<script>
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";
import "@wangeditor/editor/dist/css/style.css";
import { Message } from "element-ui";

export default {
  name: "WangEditor",
  components: { Editor, Toolbar },
  props: {
    /* 编辑器的内容 */
    value: {
      type: String,
      default: "",
    },
    /* 只读 */
    readOnly: {
      type: Boolean,
      default: false,
    },
    /* 高度 */
    height: {
      type: Number,
      default: 300,
    },
    /* 最小高度 */
    minHeight: {
      type: Number,
      default: 300,
    },
  },
  data() {
    return {
      editorRef: null,
      valueHtml: this.value,
      mode: "default", // 或 'simple' 简洁模式
      // 工具栏配置 - 禁用上传文档功能
      toolbarConfig: {
        excludeKeys: [
          // 排除上传相关功能
          "uploadImage",
          "uploadVideo",
          "insertImage",
          "insertVideo",
          "codeBlock",
        ],
      },
      // 编辑器配置
      editorConfig: {
        placeholder: "请输入协议内容...",
        readOnly: this.readOnly,
        // 禁用图片上传，只允许网络图片
        MENU_CONF: {
          insertImage: {
            // 只支持网络图片
            checkImage(src) {
              return true;
            },
          },
        },
        // 自定义提示
        customAlert(s, t) {
          switch (t) {
            case "success":
              Message.success(s);
              break;
            case "info":
              Message.info(s);
              break;
            case "warning":
              Message.warning(s);
              break;
            case "error":
              Message.error(s);
              break;
            default:
              Message.info(s);
              break;
          }
        },
      },
    };
  },
  watch: {
    value(val) {
      this.valueHtml = val;
    },
  },
  methods: {
    handleCreated(editor) {
      this.editorRef = Object.seal(editor); // 一定要用 Object.seal()
    },
    handleChange(editor) {
      this.$emit("input", this.valueHtml);
      this.$emit("change", this.valueHtml);
    },
  },
  beforeDestroy() {
    const editor = this.editorRef;
    if (editor == null) return;
    editor.destroy(); // 组件销毁时，及时销毁 editor
  },
};
</script>

<style scoped>
.wang-editor {
  border: 1px solid #ccc;
  border-radius: 4px;
}
</style>
