package com.ruoyi.web.service.impl;

import com.ruoyi.web.domain.LeaveInformation;
import com.ruoyi.web.mapper.LeaveInformationMapper;
import com.ruoyi.web.service.LeaveInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 留资信息管理 Service 实现类
 */
@Service
public class LeaveInforationServiceImpl implements LeaveInformationService {

    @Autowired
    private LeaveInformationMapper leaveInformationMapper;

    /**
     * 新增
     * @param leaveInformation 信息
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(LeaveInformation leaveInformation) {

        // 检查电话是否已存在
        int phoneNumExists = leaveInformationMapper.checkPhoneNumExists(leaveInformation.getPhoneNum());
        if (phoneNumExists > 0) {
            throw new RuntimeException("电话号码【" + leaveInformation.getPhoneNum() + "】已存在");
        }

        // 设置创建时间和更新时间
        Date now = new Date();
        leaveInformation.setCreateTime(now);
        leaveInformation.setUpdateTime(now);

        // 执行插入操作
        leaveInformationMapper.insertSelective(leaveInformation);

        System.out.println("生成的主键ID: " + leaveInformation.getId());
        return leaveInformation.getId();
    }

    /**
     * 更新留资信息
     * @param leaveInformation 留资信息信息
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(LeaveInformation leaveInformation) {
        // 参数校验
        if (leaveInformation == null) {
            throw new IllegalArgumentException("留资信息不能为空");
        }
        if (leaveInformation.getId() == null) {
            throw new IllegalArgumentException("留资信息ID不能为空");
        }

        // 查询原留资信息信息
        LeaveInformation oldLeaveInformation = leaveInformationMapper.selectById(leaveInformation.getId());
        if (oldLeaveInformation == null) {
            throw new RuntimeException("留资信息不存在，ID：" + leaveInformation.getId());
        }

        // 如果修改了电话号码，检查新号码是否与其他留资信息冲突
        if (StringUtils.hasText(leaveInformation.getPhoneNum()) && !leaveInformation.getPhoneNum().equals(oldLeaveInformation.getPhoneNum())) {
            int nameCount = leaveInformationMapper.checkPhoneNumExists(leaveInformation.getPhoneNum());
            if (nameCount > 0) {
                throw new RuntimeException("电话号码【" + leaveInformation.getName() + "】已存在");
            }
        }

        // 设置更新时间
        leaveInformation.setUpdateTime(new Date());

        // 执行更新操作（只更新非空字段）
        return leaveInformationMapper.updateSelective(leaveInformation);
    }

    /**
     * 删除留资信息
     * @param leaveInformation 留资信息信息（包含ID）
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(LeaveInformation leaveInformation) {
        // 参数校验
        if (leaveInformation == null) {
            throw new IllegalArgumentException("留资信息不能为空");
        }
        if (leaveInformation.getId() == null) {
            throw new IllegalArgumentException("留资ID不能为空");
        }

        // 查询留资是否存在
        LeaveInformation leaveInformation1 = leaveInformationMapper.selectById(leaveInformation.getId());
        if (leaveInformation1 == null) {
            throw new RuntimeException("袖子信息不存在，ID：" + leaveInformation.getId());
        }

        // 执行删除操作
        return leaveInformationMapper.deleteById(leaveInformation.getId());
    }

    /**
     * 根据条件查询留资信息
     * @param leaveInformation 查询条件
     * @return 留资信息信息
     */
    @Override
    public LeaveInformation queryLeaveInformationById(LeaveInformation leaveInformation) {
        // 参数校验
        if (leaveInformation == null) {
            throw new IllegalArgumentException("留资信息信息不能为空");
        }
        if (leaveInformation.getId() == null) {
            throw new IllegalArgumentException("留资信息ID不能为空");
        }

        // 根据ID查询留资信息
        return leaveInformationMapper.selectById(leaveInformation.getId());
    }

    /**
     * 条件查询留资信息列表
     * @param LeaveInformation 查询条件
     * @return 留资信息列表
     */
    public List<LeaveInformation> queryLeaveInformationsByCondition(LeaveInformation leaveInformation) {
        return leaveInformationMapper.selectByCondition(leaveInformation);
    }

    @Override
    public List<LeaveInformation> queryInfoByNameOrNum(String text) {
        return leaveInformationMapper.selectByNameOrNum(text);
    }

}