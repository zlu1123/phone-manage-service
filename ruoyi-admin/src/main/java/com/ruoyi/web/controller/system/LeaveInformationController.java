package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.poi.ExcelUtil;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.List;

@Api("后台留资信息管理")
@Slf4j
@RestController
@RequestMapping("/system/leaveInfo")
public class LeaveInformationController extends BaseController {

    @Autowired
    private LeaveInformationService leaveInformationService;

    /**
     * @param LeaveInformation 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询留资信息信息")
    @GetMapping("/getList")
    public R getContract(LeaveInformation leaveInformation) {
        startPage();
        List<LeaveInformation> list = leaveInformationService.queryLeaveInformationByCondition(leaveInformation);
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

    /**
     * 导出留资用户数据（按当前筛选条件，不分页）
     *
     * @param response           响应
     * @param leaveInformation   筛选条件
     */
    @ApiOperation("导出留资用户数据")
    @PostMapping("/export")
    public void export(HttpServletResponse response, LeaveInformation leaveInformation) {
        List<LeaveInformation> list = leaveInformationService.queryLeaveInformationByCondition(leaveInformation);
        ExcelUtil<LeaveInformation> util = new ExcelUtil<>(LeaveInformation.class);
        util.exportExcel(response, list, "留资用户");
    }

    /**
     * 下载留资用户导入模板
     */
    @ApiOperation("下载留资用户导入模板")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<LeaveInformation> util = new ExcelUtil<>(LeaveInformation.class);
        util.importTemplateExcel(response, "留资用户", "填写说明：姓名、电话为必填项，示例：张三 13800138000；请勿修改表头，从表头下一行开始填写，创建时间由系统自动生成");
    }

    /**
     * 导入留资用户数据
     *
     * @param file          上传的Excel文件
     * @param updateSupport 是否更新已存在的数据
     * @return 导入结果
     */
    @ApiOperation("导入留资用户数据")
    @PostMapping("/importData")
    public R importData(MultipartFile file, @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception {
        ExcelUtil<LeaveInformation> util = new ExcelUtil<>(LeaveInformation.class);
        // 模板首行为填写说明，表头在第2行，故标题占用行数为1；创建时间由后端生成，不参与导入
        List<LeaveInformation> list = util.importExcel(file.getInputStream(), 1);
        String message = leaveInformationService.importLeaveInfo(list, updateSupport, getUsername());
        // 导入结果（含成功/失败明细）放入msg返回，前端弹窗展示的是msg，避免失败时仍提示"操作成功"
        return R.ok(null, message);
    }

}
