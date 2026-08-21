package com.ruoyi.web.domain;

import com.ruoyi.common.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 留资信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveInformation {

    /**
     * id
     */
    private Long id;

    /**
     * 姓名
     */
    @Excel(name = "姓名", required = true)
    private String name;

    /**
     * 电话号码
     */
    @Excel(name = "电话", required = true)
    private String phoneNum;

    /**
     * 创建时间（仅导出，导入时由后端生成，不允许从Excel填写）
     */
    @Excel(name = "创建时间", type = Excel.Type.EXPORT, width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 修改人
     */
    private String updateBy;
}