package com.ruoyi.web.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "PhoneInfoDto", description = "手机信息实体")
@Data
public class PhoneInfoDto {

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
}
