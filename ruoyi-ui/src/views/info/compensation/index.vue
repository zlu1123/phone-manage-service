<template>
  <div class="app-container">
    <pro-table ref="proTableRef" :fetch-api="getListApi" :search-fields="searchFields" :columns="columns">
      <!-- 工具栏：新增按钮 -->
      <template #toolbar>
        <el-col :span="1.5">
          <el-button type="primary" plain :icon="Plus" size="small" @click="handleAdd">新增</el-button>
        </el-col>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <el-tag :type="getStatusTagType(row.status)" size="small">
          {{ getStatusText(row.status) }}
        </el-tag>
      </template>

      <!-- 赔付金额列 -->
      <template #amount="{ row }">
        <span>{{ Number(row.status) === 2 ? '-' : row.amount != null ? '¥' + row.amount : '-' }}</span>
      </template>

      <!-- 创建时间列 -->
      <template #createTime="{ row }">
        <span>{{ parseTime(row.createTime) }}</span>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <el-button
          v-if="Number(row.status) === 0"
          v-hasRole="['admin', 'user']"
          size="small"
          type="primary"
          link
          :icon="Edit"
          @click="handleReview(row)"
          >审核</el-button
        >
        <el-button
          v-if="Number(row.status) === 1 && Number(row.amount) === 0"
          v-hasRole="['admin', 'user']"
          size="small"
          type="warning"
          link
          :icon="Edit"
          @click="handleEditAmount(row)"
          >修改金额</el-button
        >
        <el-button v-hasRole="['admin', 'user']" size="small" type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
      </template>
    </pro-table>

    <!-- 新增对话框 -->
    <el-dialog
      title="新增赔付订单"
      v-model="addDialogVisible"
      width="450px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px" size="small">
        <el-form-item label="订单" prop="orderId">
          <el-select
            v-model="addForm.orderId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="请输入签约IMEI搜索订单"
            :remote-method="searchOrders"
            :loading="orderLoading"
            style="width: 100%"
            @visible-change="onOrderVisibleChange"
            @change="onOrderSelect"
          >
            <el-option
              v-for="item in orderOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="留资人">
          <span v-if="addFormLeaveInfo" style="color: #303133">
            {{ addFormLeaveInfo }}
          </span>
          <span v-else style="color: #909399">选择订单后自动填充</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" @click="addDialogVisible = false">取 消</el-button>
          <el-button type="primary" size="small" :loading="submitLoading" @click="handleAddSubmit">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      title="审核赔付订单"
      v-model="reviewDialogVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px" size="small">
        <el-form-item label="审核状态" prop="status">
          <el-select v-model="reviewForm.status" placeholder="请选择审核状态" style="width: 100%">
            <el-option label="审核通过" :value="1" />
            <el-option label="审核不通过" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="reviewForm.status === 1" label="赔付金额" prop="amount">
          <el-input-number
            v-model="reviewForm.amount"
            :precision="2"
            :min="0"
            :max="999999.99"
            placeholder="请输入赔付金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          v-if="reviewForm.status === 2"
          label="拒绝原因"
          prop="rejectionReason"
          :required="reviewForm.status === 2"
        >
          <el-input v-model="reviewForm.rejectionReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="reviewForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" @click="reviewDialogVisible = false">取 消</el-button>
          <el-button type="primary" size="small" :loading="submitLoading" @click="handleReviewSubmit">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改金额对话框 -->
    <el-dialog
      title="修改赔付金额"
      v-model="editAmountDialogVisible"
      width="450px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="editAmountFormRef"
        :model="editAmountForm"
        :rules="editAmountRules"
        label-width="100px"
        size="small"
        @submit.prevent
      >
        <el-form-item label="赔付金额" prop="amount">
          <el-input-number
            v-model="editAmountForm.amount"
            :precision="2"
            :min="0.01"
            :max="999999.99"
            placeholder="请输入赔付金额"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" @click="editAmountDialogVisible = false">取 消</el-button>
          <el-button type="primary" size="small" :loading="submitLoading" @click="handleEditAmountSubmit"
            >确 定</el-button
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import { getCompensationList, addCompensation, updateCompensation, deleteCompensation } from '@/api/order/compensation';
import { queryOrderList } from '@/api/order/list';
import { getLeaveInfoList } from '@/api/order/user';
import { parseTime } from '@/utils/ruoyi';

// 列表请求 API
const getListApi = getCompensationList;

// 表格引用
const proTableRef = ref(null);

// 提交按钮loading
const submitLoading = ref(false);

// 新增对话框
const addDialogVisible = ref(false);
const addFormRef = ref(null);
const addForm = reactive({
  orderId: undefined,
  infoId: undefined,
});
const addRules = {
  orderId: [
    { required: true, message: '请选择订单', trigger: 'change' },
  ],
};

// 审核对话框
const reviewDialogVisible = ref(false);
const reviewFormRef = ref(null);
const reviewForm = reactive({
  id: undefined,
  amount: undefined,
  status: undefined,
  rejectionReason: '',
  remark: '',
});
const reviewRules = {
  status: [{ required: true, message: '请选择审核状态', trigger: 'change' }],
  rejectionReason: [
    {
      validator: (rule, value, callback) => {
        if (reviewForm.status === 2 && !value) {
          callback(new Error('请输入拒绝原因'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

// 修改金额对话框
const editAmountDialogVisible = ref(false);
const editAmountFormRef = ref(null);
const editAmountForm = reactive({
  id: undefined,
  amount: undefined,
});
const editAmountRules = {
  amount: [{ required: true, message: '请输入赔付金额', trigger: 'blur' }],
};

// 搜索字段配置
const searchFields = [
  { prop: 'name', label: '留资人姓名', type: 'input' },
  { prop: 'phoneNum', label: '留资人电话', type: 'input' },
  { prop: 'signatureImei', label: '签约IMEI', type: 'input' },
  { prop: 'createBy', label: '创建人', type: 'input' },
];

// 表格列配置
const columns = [
  { label: 'ID', prop: 'id', width: '60' },
  { label: '订单ID', prop: 'orderId', width: '80' },
  { label: '留资人ID', prop: 'infoId', width: '90' },
  { label: '留资人姓名', prop: 'name', minWidth: '100' },
  { label: '留资人电话', prop: 'phoneNum', minWidth: '120' },
  { label: '签约IMEI', prop: 'signatureImei', minWidth: '140' },
  { label: '赔付金额', prop: 'amount', slot: 'amount', width: '110' },
  { label: '状态', prop: 'status', slot: 'status', width: '100' },
  {
    label: '拒绝原因',
    prop: 'rejectionReason',
    minWidth: '150',
    showOverflowTooltip: true,
  },
  {
    label: '备注',
    prop: 'remark',
    minWidth: '120',
    showOverflowTooltip: true,
  },
  { label: '创建人', prop: 'createByName', width: '100' },
  {
    label: '创建时间',
    prop: 'createTime',
    slot: 'createTime',
    width: '160',
  },
  {
    label: '操作',
    slot: 'action',
    width: '120',
    fixed: 'right',
    showOverflowTooltip: false,
  },
];

/** 获取状态文本 */
const getStatusText = (status) => {
  const map = { 0: '待审核', 1: '审核通过', 2: '审核不通过' };
  return map[Number(status)] || '未知';
};

/** 获取状态标签类型 */
const getStatusTagType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger' };
  return map[Number(status)] || 'info';
};

// 订单下拉选项
const orderOptions = ref([]);
const orderLoading = ref(false);

// 留资人下拉选项（保留用于远程搜索，后续可移除）
const leaveInfoOptions = ref([]);
const leaveInfoLoading = ref(false);

// 选中订单后自动填充的留资人显示文本
const addFormLeaveInfo = ref('');

/** 下拉展开时加载默认列表 */
const onOrderVisibleChange = (visible) => {
  if (visible && orderOptions.value.length === 0) {
    searchOrders('');
  }
};

/** 远程搜索订单列表 */
const searchOrders = (query) => {
  if (orderLoading.value) return;
  orderLoading.value = true;
  const params = { pageNum: 1, pageSize: 20 };
  if (query) {
    params.signatureImei = query;
  }
  queryOrderList(params)
    .then((res) => {
      const rows = res.data?.rows || res.rows || [];
      orderOptions.value = rows.map((item) => ({
        value: item.id,
        label: `${item.signatureModel || '-'} / ${item.signatureImei || '-'}`,
        infoId: item.infoId,
      }));
    })
    .finally(() => {
      orderLoading.value = false;
    });
};

/** 选择订单后自动回填留资人 */
const onOrderSelect = (orderId) => {
  if (!orderId) {
    addForm.infoId = undefined;
    addFormLeaveInfo.value = '';
    return;
  }
  const selected = orderOptions.value.find((item) => item.value === orderId);
  if (selected && selected.infoId) {
    addForm.infoId = selected.infoId;
    // 通过留资人ID精确查询
    getLeaveInfoList({ id: selected.infoId, pageNum: 1, pageSize: 1 })
      .then((res) => {
        const rows = res.data?.rows || res.rows || [];
        if (rows.length > 0) {
          const match = rows[0];
          addFormLeaveInfo.value = `${match.name || '-'} / ${match.phoneNum || '-'}`;
        } else {
          addFormLeaveInfo.value = `ID: ${selected.infoId}`;
        }
      })
      .catch(() => {
        addFormLeaveInfo.value = `ID: ${selected.infoId}`;
      });
  } else {
    addForm.infoId = undefined;
    addFormLeaveInfo.value = '未关联留资人';
  }
};

/** 远程搜索留资人列表 */
const searchLeaveInfo = (query) => {
  if (leaveInfoLoading.value) return;
  leaveInfoLoading.value = true;
  const params = { pageNum: 1, pageSize: 20 };
  if (query) {
    params.name = query;
  }
  getLeaveInfoList(params)
    .then((res) => {
      const rows = res.data?.rows || res.rows || [];
      leaveInfoOptions.value = rows.map((item) => ({
        value: item.id,
        label: `${item.name || '-'} / ${item.phoneNum || '-'}`,
      }));
    })
    .finally(() => {
      leaveInfoLoading.value = false;
    });
};

/** 重置新增表单 */
const resetAddForm = () => {
  addForm.orderId = undefined;
  addForm.infoId = undefined;
  addFormLeaveInfo.value = '';
  orderOptions.value = [];
  nextTick(() => {
    addFormRef.value?.clearValidate();
  });
};

/** 重置审核表单 */
const resetReviewForm = () => {
  reviewForm.id = undefined;
  reviewForm.amount = undefined;
  reviewForm.status = undefined;
  reviewForm.rejectionReason = '';
  reviewForm.remark = '';
  nextTick(() => {
    reviewFormRef.value?.clearValidate();
  });
};

/** 新增按钮 */
const handleAdd = () => {
  resetAddForm();
  addDialogVisible.value = true;
};

/** 提交新增 */
const handleAddSubmit = () => {
  addFormRef.value.validate((valid) => {
    if (!valid) return;
    submitLoading.value = true;
    addCompensation(addForm)
      .then(() => {
        ElMessage.success('新增成功');
        addDialogVisible.value = false;
        proTableRef.value.refresh();
      })
      .finally(() => {
        submitLoading.value = false;
      });
  });
};

/** 审核按钮 */
const handleReview = (row) => {
  resetReviewForm();
  reviewForm.id = row.id;
  reviewForm.amount = row.amount;
  reviewForm.rejectionReason = row.rejectionReason || '';
  reviewForm.remark = row.remark || '';
  reviewDialogVisible.value = true;
};

/** 提交审核 */
const handleReviewSubmit = () => {
  reviewFormRef.value.validate((valid) => {
    if (!valid) return;
    submitLoading.value = true;
    updateCompensation(reviewForm)
      .then(() => {
        ElMessage.success('审核提交成功');
        reviewDialogVisible.value = false;
        proTableRef.value.refresh();
      })
      .finally(() => {
        submitLoading.value = false;
      });
  });
};

/** 修改金额按钮 */
const handleEditAmount = (row) => {
  editAmountForm.id = row.id;
  editAmountForm.amount = undefined;
  editAmountDialogVisible.value = true;
  nextTick(() => {
    editAmountFormRef.value?.clearValidate();
  });
};

/** 提交修改金额 */
const handleEditAmountSubmit = () => {
  editAmountFormRef.value.validate((valid) => {
    if (!valid) return;
    submitLoading.value = true;
    updateCompensation(editAmountForm)
      .then(() => {
        ElMessage.success('金额修改成功');
        editAmountDialogVisible.value = false;
        proTableRef.value.refresh();
      })
      .finally(() => {
        submitLoading.value = false;
      });
  });
};

/** 删除按钮 */
const handleDelete = (row) => {
  ElMessageBox.confirm(`是否确认删除该赔付订单（ID：${row.id}）？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      return deleteCompensation(row.id);
    })
    .then(() => {
      ElMessage.success('删除成功');
      proTableRef.value.refresh();
    })
    .catch(() => {});
};
</script>
