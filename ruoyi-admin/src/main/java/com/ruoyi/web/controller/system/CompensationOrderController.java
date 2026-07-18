package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.web.domain.CompensationOrder;
import com.ruoyi.web.model.CompensationOrderDto;
import com.ruoyi.web.model.CompensationOrderReturnDto;
import com.ruoyi.web.service.CompensationOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Api("后台赔付订单管理")
@Slf4j
@RestController
@RequestMapping("/system/CompensationOrder")
public class CompensationOrderController extends BaseController {

    @Autowired
    private CompensationOrderService compensationOrderService;

    /**
     * @param compensationOrderDto 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询赔付订单信息")
    @GetMapping("/getList")
    public TableDataInfo getCompensationOrder(CompensationOrderDto compensationOrderDto) {
        startPage();
        List<CompensationOrderReturnDto> list = compensationOrderService.queryCompensationOrderByCondition(compensationOrderDto);
        return getDataTable(list);
    }

    /**
     * 新增赔付订单
     *
     * @param compensationOrderDto 赔付订单信息
     * @return 操作结果
     */
    @PostMapping("/insert")
    public R insert(@RequestBody @Validated CompensationOrderDto compensationOrderDto) {
        try {
            CompensationOrder compensationOrder = new CompensationOrder();
            BeanUtils.copyProperties(compensationOrderDto, compensationOrder);
            compensationOrder.setStatus(0);
            compensationOrder.setCreateBy(getUsername());
            compensationOrder.setUpdateBy(getUsername());
            Long id = compensationOrderService.insert(compensationOrder);
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
     * 更新赔付订单
     *
     * @param compensationOrder 赔付订单（必须包含ID）
     * @return 操作结果
     */
    @PostMapping("/update")
    public R update(@RequestBody @Validated CompensationOrder compensationOrder) {
        try {
            // 校验ID
            if (compensationOrder.getId() == null) {
                return R.fail("赔付订单ID不能为空");
            }
            compensationOrder.setUpdateBy(getUsername());
            int rows = compensationOrderService.update(compensationOrder);
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
     * 删除赔付订单（根据ID）
     *
     * @param id ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable("id") @NotNull(message = "赔付订单ID不能为空") Long id) {
        try {
            CompensationOrder compensationOrder = new CompensationOrder();
            compensationOrder.setId(id);
            int rows = compensationOrderService.delete(compensationOrder);
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
