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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            throw new RuntimeException("留资信息不存在，ID：" + leaveInformation.getId());
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
    public List<LeaveInformation> queryLeaveInformationByCondition(LeaveInformation leaveInformation) {
        return leaveInformationMapper.selectByCondition(leaveInformation);
    }

    @Override
    public List<LeaveInformation> queryInfoByNameOrNum(String text) {
        return leaveInformationMapper.selectByNameOrNum(text);
    }

    @Override
    public LeaveInformation selectByPhoneNum(String phoneNum) {
        if (!StringUtils.hasText(phoneNum)) {
            return null;
        }
        return leaveInformationMapper.selectByPhoneNum(phoneNum.trim());
    }

    /**
     * 导入留资信息
     * <p>
     * 以电话号码作为唯一匹配键：
     * <ul>
     *     <li>updateSupport=true 且电话已存在：更新对应记录的姓名</li>
     *     <li>updateSupport=false 且电话已存在：跳过并记为失败</li>
     *     <li>电话不存在：新增</li>
     * </ul>
     *
     * @param list          导入数据
     * @param updateSupport 是否更新已存在的数据
     * @param operName      操作人
     * @return 导入结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importLeaveInfo(List<LeaveInformation> list, boolean updateSupport, String operName) {
        if (list == null || list.isEmpty()) {
            return "导入数据为空";
        }

        int successCount = 0;
        int updateCount = 0;
        int failCount = 0;
        StringBuilder failMsg = new StringBuilder();
        Set<String> processedPhones = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {
            LeaveInformation info = list.get(i);
            int rowNum = i + 1;
            try {
                // 防御：解析结果不应为null，出现时给出明确提示而不是抛空指针
                if (info == null) {
                    failCount++;
                    failMsg.append("<br/>第").append(rowNum).append("条：未解析到有效数据");
                    continue;
                }
                String name = info.getName() == null ? null : info.getName().trim();
                String phoneNum = info.getPhoneNum() == null ? null : info.getPhoneNum().trim();

                // 1. 字段逻辑校验：姓名、电话必填
                if (!StringUtils.hasText(name) || !StringUtils.hasText(phoneNum)) {
                    failCount++;
                    failMsg.append("<br/>第").append(rowNum).append("条：姓名和电话不能为空");
                    continue;
                }
                // 2. 文件内电话去重
                if (processedPhones.contains(phoneNum)) {
                    failCount++;
                    failMsg.append("<br/>第").append(rowNum).append("条：电话号码【").append(phoneNum).append("】与文件内其他行重复");
                    continue;
                }
                processedPhones.add(phoneNum);
                info.setName(name);
                info.setPhoneNum(phoneNum);

                // 3. 按电话号码精确匹配已有记录
                LeaveInformation exist = leaveInformationMapper.selectByPhoneNum(phoneNum);
                if (exist != null) {
                    if (updateSupport) {
                        info.setId(exist.getId());
                        info.setUpdateBy(operName);
                        info.setUpdateTime(new Date());
                        leaveInformationMapper.updateSelective(info);
                        updateCount++;
                    } else {
                        failCount++;
                        failMsg.append("<br/>第").append(rowNum).append("条：电话号码【").append(phoneNum).append("】已存在");
                    }
                } else {
                    info.setCreateBy(operName);
                    info.setUpdateBy(operName);
                    info.setCreateTime(new Date());
                    info.setUpdateTime(new Date());
                    leaveInformationMapper.insert(info);
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                failMsg.append("<br/>第").append(rowNum).append("条导入失败：")
                        .append(StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName());
            }
        }

        StringBuilder resultMsg = new StringBuilder();
        resultMsg.append("共 ").append(list.size()).append(" 条数据，成功导入 ").append(successCount).append(" 条");
        if (updateCount > 0) {
            resultMsg.append("，更新 ").append(updateCount).append(" 条");
        }
        if (failCount > 0) {
            resultMsg.append("，失败 ").append(failCount).append(" 条");
            resultMsg.append(failMsg);
        }
        return resultMsg.toString();
    }

}