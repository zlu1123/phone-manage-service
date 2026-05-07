package com.ruoyi.web.service;

import com.ruoyi.web.domain.Contract;

import java.util.List;

public interface ContractService {

    int insert(Contract contract);

    int update(Contract contract);

    int delete(Contract contract);

    Contract queryContractById(Contract contract);

    List<Contract> queryContractsByCondition(Contract contract);

    int enableContract(Integer id);

    int updateStatus(Integer id, Boolean status, String updateBy);

    int batchUpdateStatus(Integer id, String updateBy);
}
