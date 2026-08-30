package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.PhoneImportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PhoneImportTaskMapper {

    int insert(PhoneImportTask task);

    PhoneImportTask selectByTaskId(@Param("taskId") String taskId);

    /**
     * 更新处理进度（独立事务内调用，导入执行期间轮询实时可见）
     *
     * @param taskId         任务ID
     * @param processedCount 已处理行数
     */
    int updateProgress(@Param("taskId") String taskId, @Param("processedCount") int processedCount);

    /**
     * 任务结束：写入最终状态、统计数与结果信息
     */
    int updateFinish(@Param("taskId") String taskId, @Param("status") String status,
                     @Param("processedCount") int processedCount,
                     @Param("successCount") int successCount, @Param("updateCount") int updateCount,
                     @Param("failCount") int failCount, @Param("message") String message);

    /** 进行中的任务数（并发保护：同一时间只允许一个导入任务） */
    int countRunning();

    /**
     * 将超时未完成的进行中任务标记为失败（服务重启/任务中断兜底）
     *
     * @param minutes 超过该分钟数未更新视为中断
     */
    int failStaleRunning(@Param("minutes") int minutes);
}
