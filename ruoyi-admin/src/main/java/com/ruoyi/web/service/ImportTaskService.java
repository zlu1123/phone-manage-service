package com.ruoyi.web.service;

import com.ruoyi.web.domain.PhoneActiveInfoImport;
import com.ruoyi.web.domain.PhoneImportTask;
import com.ruoyi.web.mapper.PhoneImportTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * 订单导入异步任务：在后台线程执行导入并维护任务表状态。
 * <p>
 * 全量原子保证：{@link IPhoneActiveInfoService#importActiveInfo} 自带事务，
 * 任意一行失败即抛异常整体回滚（含留资自动创建）；本类捕获异常后
 * 将任务标记为失败并记录错误明细，前端轮询可见。
 * <p>
 * 进度更新使用 REQUIRES_NEW 独立事务：主导入事务未提交（或已回滚）时，
 * 轮询也能看到实时进度。
 */
@Slf4j
@Service
public class ImportTaskService {

    @Autowired
    private IPhoneActiveInfoService phoneActiveInfoService;

    @Autowired
    private PhoneImportTaskMapper importTaskMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 执行导入任务（由 Controller 提交到导入线程池，非请求线程）
     *
     * @param task          任务记录（含 taskId/totalCount，调用前已落库）
     * @param rows          解析后的导入行（Excel 解析在请求线程完成）
     * @param updateSupport 是否更新已存在的数据
     * @param operName      操作人
     */
    public void runImport(PhoneImportTask task, List<PhoneActiveInfoImport> rows,
                          boolean updateSupport, String operName) {
        String taskId = task.getTaskId();
        int total = task.getTotalCount() == null ? 0 : task.getTotalCount();
        TransactionTemplate progressTx = new TransactionTemplate(transactionManager);
        progressTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // 进度更新走独立事务，回滚后仍保留最后处理位置，供失败任务展示
        final int[] lastProcessed = {0};
        try {
            Map<String, Object> result = phoneActiveInfoService.importActiveInfo(rows, updateSupport, operName,
                    processedCount -> {
                        lastProcessed[0] = processedCount;
                        progressTx.execute(status -> importTaskMapper.updateProgress(taskId, processedCount));
                    });
            int successCount = ((Number) result.getOrDefault("successCount", 0)).intValue();
            int updateCount = ((Number) result.getOrDefault("updateCount", 0)).intValue();
            int failCount = ((Number) result.getOrDefault("failCount", 0)).intValue();
            String message = (String) result.get("message");
            if (failCount > 0) {
                // 校验阶段不通过：未写入任何数据，任务直接标记失败
                importTaskMapper.updateFinish(taskId, PhoneImportTask.STATUS_FAILED, 0, 0, 0, failCount, message);
            } else {
                importTaskMapper.updateFinish(taskId, PhoneImportTask.STATUS_SUCCESS, total,
                        successCount, updateCount, 0, message);
            }
        } catch (Exception e) {
            // 运行期失败：导入事务已整体回滚，记录错误明细供页面提醒用户
            log.error("订单导入任务失败 taskId={}", taskId, e);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            importTaskMapper.updateFinish(taskId, PhoneImportTask.STATUS_FAILED, lastProcessed[0], 0, 0, total,
                    "导入失败，已回滚全部数据，未写入任何记录：" + reason);
        }
    }
}
