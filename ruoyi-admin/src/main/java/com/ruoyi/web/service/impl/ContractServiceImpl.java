package com.ruoyi.web.service.impl;

import com.ruoyi.web.domain.Contract;
import com.ruoyi.web.mapper.ContractMapper;
import com.ruoyi.web.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 协议管理 Service 实现类
 */
@Service
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractMapper contractMapper;

    /**
     * 新增协议
     * @param contract 协议信息
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(Contract contract) {

        // 检查协议名称是否已存在
        int nameCount = contractMapper.checkNameExists(contract.getName());
        if (nameCount > 0) {
            throw new RuntimeException("协议名称【" + contract.getName() + "】已存在");
        }

        // 检查协议版本是否已存在
        int versionCount = contractMapper.checkVersionExists(contract.getVersion());
        if (versionCount > 0) {
            throw new RuntimeException("协议版本号【" + contract.getVersion() + "】已存在");
        }

        // 设置创建时间和更新时间
        Date now = new Date();
        contract.setCreateTime(now);
        contract.setUpdateTime(now);

        // 设置默认状态（未生效）
        if (contract.getStatus() == null) {
            contract.setStatus(false);
        }

        // 执行插入操作
        return contractMapper.insertSelective(contract);
    }

    /**
     * 更新协议
     * @param contract 协议信息
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(Contract contract) {
        // 参数校验
        if (contract == null) {
            throw new IllegalArgumentException("协议信息不能为空");
        }
        if (contract.getId() == null) {
            throw new IllegalArgumentException("协议ID不能为空");
        }

        // 查询原协议信息
        Contract oldContract = contractMapper.selectById(contract.getId());
        if (oldContract == null) {
            throw new RuntimeException("协议不存在，ID：" + contract.getId());
        }

        // 如果修改了协议名称，检查新名称是否与其他协议冲突
        if (StringUtils.hasText(contract.getName()) && !contract.getName().equals(oldContract.getName())) {
            int nameCount = contractMapper.checkNameExists(contract.getName());
            if (nameCount > 0) {
                throw new RuntimeException("协议名称【" + contract.getName() + "】已存在");
            }
        }

        // 如果修改了协议版本号，检查新版本号是否与其他协议冲突
        if (StringUtils.hasText(contract.getVersion()) && !contract.getVersion().equals(oldContract.getVersion())) {
            int versionCount = contractMapper.checkVersionExists(contract.getVersion());
            if (versionCount > 0) {
                throw new RuntimeException("协议版本号【" + contract.getVersion() + "】已存在");
            }
        }

        // 设置更新时间
        contract.setUpdateTime(new Date());

        // 执行更新操作（只更新非空字段）
        return contractMapper.updateSelective(contract);
    }

    /**
     * 删除协议
     * @param contract 协议信息（包含ID）
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Contract contract) {
        // 参数校验
        if (contract == null) {
            throw new IllegalArgumentException("协议信息不能为空");
        }
        if (contract.getId() == null) {
            throw new IllegalArgumentException("协议ID不能为空");
        }

        // 查询协议是否存在
        Contract existingContract = contractMapper.selectById(contract.getId());
        if (existingContract == null) {
            throw new RuntimeException("协议不存在，ID：" + contract.getId());
        }

        // 执行删除操作
        return contractMapper.deleteById(contract.getId());
    }

    /**
     * 根据条件查询协议
     * @param contract 查询条件
     * @return 协议信息
     */
    @Override
    public Contract queryContractById(Contract contract) {
        // 参数校验
        if (contract == null) {
            throw new IllegalArgumentException("协议信息不能为空");
        }
        if (contract.getId() == null) {
            throw new IllegalArgumentException("协议ID不能为空");
        }

        // 根据ID查询协议
        return contractMapper.selectById(contract.getId());
    }

    /**
     * 根据ID查询协议（重载方法）
     * @param id 协议ID
     * @return 协议信息
     */
    public Contract queryContractById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("协议ID不能为空");
        }
        return contractMapper.selectById(id);
    }

    /**
     * 根据协议名称查询
     * @param name 协议名称
     * @return 协议信息
     */
    public Contract queryContractByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("协议名称不能为空");
        }
        return contractMapper.selectByName(name);
    }

    /**
     * 根据版本号查询
     * @param version 协议版本号
     * @return 协议信息
     */
    public Contract queryContractByVersion(String version) {
        if (!StringUtils.hasText(version)) {
            throw new IllegalArgumentException("协议版本号不能为空");
        }
        return contractMapper.selectByVersion(version);
    }

    /**
     * 查询所有协议列表
     * @return 协议列表
     */
    public List<Contract> queryAllContracts() {
        return contractMapper.selectAll();
    }

    /**
     * 查询生效中的协议列表
     * @return 生效中的协议列表
     */
    public List<Contract> queryActiveContracts() {
        return contractMapper.selectActiveContracts();
    }

    /**
     * 分页查询协议列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 协议列表
     */
    public List<Contract> queryContracts(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        int offset = (pageNum - 1) * pageSize;
        return contractMapper.selectByPage(offset, pageSize);
    }

    /**
     * 条件查询协议列表
     * @param contract 查询条件
     * @return 协议列表
     */
    public List<Contract> queryContractsByCondition(Contract contract) {
        return contractMapper.selectByCondition(contract);
    }

    /**
     * 更新协议状态
     * @param id 协议ID
     * @param status 状态值
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateStatus(Integer id, Boolean status, String updateBy) {
        if (id == null) {
            throw new IllegalArgumentException("协议ID不能为空");
        }
        if (status == null) {
            throw new IllegalArgumentException("协议状态不能为空");
        }

        // 检查协议是否存在
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new RuntimeException("协议不存在，ID：" + id);
        }

        // 更新状态
        return contractMapper.updateStatus(id, status, updateBy);
    }

    /**
     * 批量更新协议状态为失效
     * @param id 排除的协议ID
     * @param status 状态值
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(Integer id, String updateBy) {
        if (id == null) {
            throw new IllegalArgumentException("协议ID不能为空");
        }

        // 更新状态
        return contractMapper.batchUpdateStatus(id, updateBy);
    }
    /**
     * 批量删除协议
     * @param ids 协议ID数组
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            throw new IllegalArgumentException("请选择要删除的协议");
        }

        // 检查所有ID是否存在
        for (Integer id : ids) {
            Contract contract = contractMapper.selectById(id);
            if (contract == null) {
                throw new RuntimeException("协议不存在，ID：" + id);
            }
        }

        // 批量删除
        return contractMapper.deleteBatch(ids);
    }

    /**
     * 统计协议总数
     * @return 协议总数
     */
    public int countTotal() {
        return contractMapper.countTotal();
    }

    /**
     * 启用协议（将状态设置为生效中）
     * @param id 协议ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int enableContract(Integer id) {
        return updateStatus(id, true, null);
    }

    /**
     * 禁用协议（将状态设置为未生效）
     * @param id 协议ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int disableContract(Integer id) {
        return updateStatus(id, false, null);
    }
}