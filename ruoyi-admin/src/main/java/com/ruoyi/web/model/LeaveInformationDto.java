package com.ruoyi.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 留资信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveInformationDto {

    /**
     * id
     */
    private Long id;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 电话号码
     */
    @NotBlank(message = "电话号码不能为空")
    @Size(min = 1, max = 16, message = "电话号码长度必须在1-16个字符之间")
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