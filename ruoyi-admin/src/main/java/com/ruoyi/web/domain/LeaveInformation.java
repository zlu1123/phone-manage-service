package com.ruoyi.web.domain;

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
    private String name;

    /**
     * 电话号码
     */
    private String phoneNum;

    /**
     * 创建时间
     */
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