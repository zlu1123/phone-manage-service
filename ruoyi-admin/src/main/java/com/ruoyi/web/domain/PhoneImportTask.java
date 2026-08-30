package com.ruoyi.web.domain;

import lombok.Data;

import java.util.Date;

/**
 * 订单导入任务
 * <p>
 * 导入采用「异步任务 + 全量原子」模式：上传后立即返回 taskId，
 * 后台线程执行导入并实时更新进度，前端轮询本对象查询任务状态与结果；
 * 任意一行失败则整体回滚，任务标记为失败并记录错误明细。
 */
@Data
public class PhoneImportTask {

    /** 任务状态：进行中 */
    public static final String STATUS_RUNNING = "0";
    /** 任务状态：成功 */
    public static final String STATUS_SUCCESS = "1";
    /** 任务状态：失败（已整体回滚） */
    public static final String STATUS_FAILED = "2";

    /** 主键 */
    private Long id;

    /** 任务ID（UUID，接口查询用） */
    private String taskId;

    /** 导入文件名 */
    private String fileName;

    /** 任务状态：0-进行中 1-成功 2-失败 */
    private String status;

    /** 导入总行数 */
    private Integer totalCount;

    /** 已处理行数（实时进度） */
    private Integer processedCount;

    /** 新增成功数 */
    private Integer successCount;

    /** 更新数 */
    private Integer updateCount;

    /** 失败数（失败时整体回滚） */
    private Integer failCount;

    /** 结果信息（成功统计/错误与提示明细） */
    private String message;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间（含进度更新） */
    private Date updateTime;
}
