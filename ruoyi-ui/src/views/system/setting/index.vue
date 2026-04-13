<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>系统时间设置</span>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="系统时间" prop="configValue">
          <!-- 只读展示模式 -->
          <span v-if="!isEditing" class="sys-time-display">{{
            form.configValue || "暂未设置"
          }}</span>
          <!-- 编辑模式 -->
          <el-date-picker
            v-else
            v-model="form.configValue"
            type="date"
            placeholder="请选择系统时间"
            value-format="yyyy-MM-dd"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <template v-if="!isEditing">
            <el-button type="primary" icon="el-icon-edit" @click="handleEdit"
              >修改</el-button
            >
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

<script>
import { getConfigKey, updateSysTime } from "@/api/system/config";

export default {
  name: "SysTime",
  data() {
    return {
      // 是否处于编辑模式
      isEditing: false,
      // 编辑前的原始值，用于取消时恢复
      originalValue: "",
      // 表单参数
      form: {
        configKey: "sys.time",
        configValue: "",
      },
      // 表单校验
      rules: {
        configValue: [
          { required: true, message: "请选择系统时间", trigger: "change" },
        ],
      },
    };
  },
  created() {
    this.getSysTime();
  },
  methods: {
    /** 获取当前系统时间 */
    getSysTime() {
      getConfigKey("sys.time").then((response) => {
        this.form.configValue = response.msg;
        this.originalValue = response.msg;
      });
    },
    /** 点击修改按钮，进入编辑模式 */
    handleEdit() {
      this.isEditing = true;
    },
    /** 确认提交 */
    handleSubmit() {
      this.$refs["form"].validate((valid) => {
        if (!valid) return;

        const data = {
          configKey: this.form.configKey,
          configValue: this.form.configValue,
        };

        this.$modal
          .confirm('是否确认修改系统时间为 "' + this.form.configValue + '" ？')
          .then(() => {
            return updateSysTime(data);
          })
          .then(() => {
            this.$modal.msgSuccess("修改成功");
            this.originalValue = this.form.configValue;
            this.isEditing = false;
          })
          .catch(() => {});
      });
    },
    /** 取消编辑，恢复原始值 */
    handleCancel() {
      this.form.configValue = this.originalValue;
      this.isEditing = false;
    },
  },
};
</script>

<style scoped>
.sys-time-display {
  font-size: 16px;
  font-weight: bold;
  color: #606266;
  line-height: 40px;
}
</style>
