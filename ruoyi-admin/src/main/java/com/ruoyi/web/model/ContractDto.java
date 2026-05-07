package com.ruoyi.web.model;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.*;
import java.util.Date;

/**
 * 协议管理 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDto extends BaseEntity {

    /**
     * id
     * 更新操作时必填，新增操作时为null
     */
    private Integer id;

    /**
     * 协议名称
     */
    @NotBlank(message = "协议名称不能为空")
    @Size(min = 1, max = 50, message = "协议名称长度必须在1-50个字符之间")
    private String name;

    /**
     * 协议版本号
     */
    @NotBlank(message = "协议版本号不能为空")
    @Size(min = 1, max = 32, message = "协议版本号长度必须在1-32个字符之间")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "协议版本号只能包含字母、数字、点、下划线和中划线")
    private String version;

    /**
     * 协议文件地址
     */
    private String filePath;

    /**
     * 是否生效（0：未生效，1：生效中）
     * 新增时默认为false，可为null
     */
    private Boolean status;

    /**
     * 创建时间
     * 系统自动生成，无需校验
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     * 系统自动生成，无需校验
     */
    private Date updateTime;

    /**
     * 修改人
     */
    private String updateBy;
}