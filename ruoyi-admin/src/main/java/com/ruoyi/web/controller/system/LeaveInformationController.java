package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.web.domain.LeaveInformation;
import com.ruoyi.web.model.LeaveInformationDto;
import com.ruoyi.web.service.LeaveInformationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Api("后台留资信息管理")
@Slf4j
@RestController
@RequestMapping("/system/leaveInfo")
public class LeaveInformationController extends BaseController {

    @Autowired
    private LeaveInformationService leaveInformationService;

    @Autowired
    private ServerConfig serverConfig;

    /**
     * @param LeaveInformation 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询留资信息信息")
    @GetMapping("/getList")
    public R getContract(LeaveInformation leaveInformation) {
        startPage();
        List<LeaveInformation> list = leaveInformationService.queryLeaveInformationsByCondition(leaveInformation);
        return R.ok(getDataTable(list));
    }

    /**
     * 新增留资信息
     *
     * @param leaveInformationDto 留资信息信息
     * @return 操作结果
     */
    @PostMapping("/insert")
    public R insert(@RequestBody @Validated LeaveInformationDto leaveInformationDto) {
        try {
            LeaveInformation leaveInformation = new LeaveInformation();
            BeanUtils.copyProperties(leaveInformationDto, leaveInformation);
            leaveInformation.setCreateBy(getUsername());
            leaveInformation.setUpdateBy(getUsername());
            Long id = leaveInformationService.insert(leaveInformation);
            if (id > 0) {
                return R.ok(id);
            } else {
                return R.fail();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新留资
     *
     * @param LeaveInformationDto 留资信息（必须包含ID）
     * @return 操作结果
     */
    @PostMapping("/update")
    public R update(@RequestBody @Validated LeaveInformationDto leaveInformationDto) {
        try {
            // 校验ID
            if (leaveInformationDto.getId() == null) {
                return R.fail("留资信息ID不能为空");
            }
            LeaveInformation leaveInformation = new LeaveInformation();
            BeanUtils.copyProperties(leaveInformationDto, leaveInformation);
            leaveInformation.setUpdateBy(getUsername());
            int rows = leaveInformationService.update(leaveInformation);
            if (rows > 0) {
                return R.ok();
            } else {
                return R.fail();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除留资信息（根据ID）
     *
     * @param id ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable("id") @NotNull(message = "留资信息ID不能为空") Long id) {
        try {
            LeaveInformation leaveInformation = new LeaveInformation();
            leaveInformation.setId(id);
            int rows = leaveInformationService.delete(leaveInformation);
            if (rows > 0) {
                return R.ok();
            } else {
                return R.fail();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
