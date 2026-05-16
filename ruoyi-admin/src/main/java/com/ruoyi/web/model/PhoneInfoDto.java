package com.ruoyi.web.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

@ApiModel(value = "PhoneInfoDto", description = "手机信息实体")
@Data
@ToString
public class PhoneInfoDto {

    @ApiModelProperty("订单id")
    private Long id;
    @ApiModelProperty("sn号")
    private String sn;
    @ApiModelProperty("imei1号")
    private String imei1;
    @ApiModelProperty("imei2号")
    private String imei2;
    @ApiModelProperty("型号")
    private String model;
    @ApiModelProperty("是否激活")
    private Boolean activated;
    @ApiModelProperty("激活时间")
    private String activateDate;
    @ApiModelProperty("保修结束日期")
    private String coverage;

    @ApiModelProperty("上传图片相对路径")
    private String imagePath;
    @ApiModelProperty("上传图片完整访问链接")
    private String imageUrl;
    @ApiModelProperty("系统时间")
    private String sysTime;
    @ApiModelProperty("协议id")
    private Long contractId;
    @ApiModelProperty("协议文件相对路径")
    private String contractPath;
}
