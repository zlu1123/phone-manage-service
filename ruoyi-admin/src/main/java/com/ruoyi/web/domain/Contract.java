package com.ruoyi.web.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 协议管理 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    /**
     * id
     */
    private Integer id;

    /**
     * 协议名称
     */
    private String name;

    /**
     * 协议版本号
     */
    private String version;

    /**
     * 协议文件地址
     */
    private String filePath;

    /**
     * 协议内容（富文本）
     */
    private String content;

    /**
     * 是否生效（0：未生效，1：生效中）
     */
    private Boolean status;

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