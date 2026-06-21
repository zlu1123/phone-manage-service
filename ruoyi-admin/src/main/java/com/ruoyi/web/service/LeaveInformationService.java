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
}
