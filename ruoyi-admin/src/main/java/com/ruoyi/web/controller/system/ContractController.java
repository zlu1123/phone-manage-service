package com.ruoyi.web.controller.system;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.web.domain.Contract;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.model.ContractDto;
import com.ruoyi.web.service.ContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Api("后台协议管理")
@Slf4j
@RestController
@RequestMapping("/system/contract")
public class ContractController extends BaseController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ServerConfig serverConfig;

    /**
     * 根据ID获取协议内容（供前端点击查看协议时按需加载）
     *
     * @param id 协议ID
     * @return 协议内容
     */
    @ApiOperation("根据ID获取协议内容")
    @GetMapping("/getById")
    public R getById(@RequestParam("id") Integer id) {
        Contract contract = new Contract();
        contract.setId(id);
        Contract result = contractService.queryContractById(contract);
        if (result == null) {
            return R.fail("协议不存在");
        }
        return R.ok(result);
    }

    /**
     * @param ContractDto 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询协议信息")
    @GetMapping("/getList")
    public R getContract(ContractDto contractDto) {
        startPage();
        Contract contract = new Contract();
        BeanUtils.copyProperties(contractDto, contract);
        List<Contract> list = contractService.queryContractsByCondition(contract);
        return R.ok(getDataTable(list));
    }


    /**
     * 新增协议
     *
     * @param contractDto 协议信息
     * @param file 协议文件
     * @return 操作结果
     */
    @PostMapping("/insert")
    public R insert(@RequestPart("contractDto") @Validated ContractDto contractDto,
                    @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            String resolvedImagePath = "";
            if (file != null && !file.isEmpty()) {
                try {
                    resolvedImagePath = resolveImagePath(null, null, null, file);
                } catch (Exception e) {
                    log.error("处理上传协议文件失败", e);
                    return R.fail("上传协议文件失败：" + e.getMessage());
                }
            }
            Contract contract = new Contract();
            BeanUtils.copyProperties(contractDto, contract);
            contract.setCreateBy(getUsername());
            contract.setUpdateBy(getUsername());
            contract.setFilePath(normalizeImagePath(resolvedImagePath));
            int rows = contractService.insert(contract);
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
     * 更新协议
     *
     * @param contractDto 协议信息（必须包含ID）
     * @return 操作结果
     */
    @PostMapping("/update")
    public R update(@RequestPart("contractDto") @Validated ContractDto contractDto,
                    @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            String resolvedImagePath = "";
            if (file != null && !file.isEmpty()) {
                try {
                    resolvedImagePath = resolveImagePath(null, null, null, file);
                } catch (Exception e) {
                    log.error("处理上传协议文件失败", e);
                    return R.fail("上传协议文件失败：" + e.getMessage());
                }
            }
            // 校验ID
            if (contractDto.getId() == null) {
                return R.fail("协议ID不能为空");
            }
            Contract contract = new Contract();
            BeanUtils.copyProperties(contractDto, contract);
            contract.setUpdateBy(getUsername());
            contract.setFilePath(normalizeImagePath(resolvedImagePath));
            int rows = contractService.update(contract);
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
     * 删除协议（根据ID）
     *
     * @param id 协议ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable("id") @NotNull(message = "协议ID不能为空") Integer id) {
        try {
            Contract contract = new Contract();
            contract.setId(id);
            int rows = contractService.delete(contract);
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
     * 生效/失效（根据ID）
     *
     * @param id 协议ID
     * @return 操作结果
     */
    @PostMapping("/updateStatus")
    public R updateStatus(@RequestBody(required = false) Map<String, String> map) {
        try {

            Integer id = Integer.parseInt(map.get("id"));
            Boolean status = Boolean.parseBoolean(map.get("status"));
            if (status) {
                // 如果是修改当前协议为生效，则修改其他的协议未失效
                int batchUpdateStatus = contractService.batchUpdateStatus(id, getUsername());
            }

            int rows = contractService.updateStatus(id,status,getUsername());
            if (rows > 0) {
                return R.ok();
            } else {
                return R.fail();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    private String resolveImagePath(String imagePath, String imageUrl, MultipartFile img, MultipartFile file) throws Exception {
        MultipartFile targetFile = chooseImageFile(img, file);
        if (targetFile != null) {
            return uploadImage(targetFile);
        }
        if (!StringUtils.isEmpty(imagePath)) {
            return imagePath.trim();
        }
        if (!StringUtils.isEmpty(imageUrl)) {
            return imageUrl.trim();
        }
        return null;
    }

    private MultipartFile chooseImageFile(MultipartFile img, MultipartFile file) {
        if (img != null && !img.isEmpty()) {
            return img;
        }
        if (file != null && !file.isEmpty()) {
            return file;
        }
        return null;
    }

    private String uploadImage(MultipartFile file) throws Exception {
        return FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file);
    }

    private String normalizeImagePath(String imagePath) {
        if (StringUtils.isEmpty(imagePath)) {
            return null;
        }
        String trimmedPath = imagePath.trim();
        int profileIndex = trimmedPath.indexOf("/profile/");
        if (profileIndex >= 0) {
            return trimmedPath.substring(profileIndex);
        }
        return trimmedPath;
    }

    private String buildImageUrl(String imagePath) {
        if (StringUtils.isEmpty(imagePath)) {
            return null;
        }
        if (isExternalUrl(imagePath)) {
            return imagePath;
        }
        return serverConfig.getUrl() + imagePath;
    }

    private boolean isExternalUrl(String value) {
        String lowerValue = value.toLowerCase();
        return lowerValue.startsWith("http://") || lowerValue.startsWith("https://");
    }
}
