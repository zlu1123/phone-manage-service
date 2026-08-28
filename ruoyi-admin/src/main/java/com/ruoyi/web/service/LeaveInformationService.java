package com.ruoyi.web.service;

import com.ruoyi.web.domain.LeaveInformation;

import java.util.List;

public interface LeaveInformationService {

    Long insert(LeaveInformation leaveInformation);

    int update(LeaveInformation leaveInformation);

    int delete(LeaveInformation leaveInformation);

    LeaveInformation queryLeaveInformationById(LeaveInformation leaveInformation);

    List<LeaveInformation> queryLeaveInformationByCondition(LeaveInformation leaveInformation);

    List<LeaveInformation> queryInfoByNameOrNum(String text);

    /**
     * 按电话号码精确查询留资信息（电话唯一）
     *
     * @param phoneNum 电话号码
     * @return 留资信息，不存在返回 null
     */
    LeaveInformation selectByPhoneNum(String phoneNum);

    /**
     * 导入留资信息
     *
     * @param list          导入数据
     * @param updateSupport 是否更新已存在的数据（按电话号码匹配）
     * @param operName      操作人
     * @return 导入结果消息
     */
    String importLeaveInfo(List<LeaveInformation> list, boolean updateSupport, String operName);
}
