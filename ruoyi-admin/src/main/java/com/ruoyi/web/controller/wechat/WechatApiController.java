package com.ruoyi.web.controller.wechat;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.web.core.config.PhoneInfoConverterContext;
import com.ruoyi.web.domain.CompensationOrder;
import com.ruoyi.web.domain.Contract;
import com.ruoyi.web.domain.LeaveInformation;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.model.CompensationOrderDto;
import com.ruoyi.web.model.CompensationOrderReturnDto;
import com.ruoyi.web.model.LeaveInformationDto;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.*;
import com.ruoyi.web.service.impl.ExternalApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.ruoyi.common.utils.SecurityUtils.getNickName;


@Api("小程序接口")
@Slf4j
@RestController
@RequestMapping("/wechat/api/")
public class WechatApiController extends BaseController {

    @Autowired
    private ExternalApiService externalApiService;
    @Autowired
    private PhoneInfoConverterContext converterContext;
    @Autowired
    private IPhoneActiveInfoService phoneActiveInfoService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ServerConfig serverConfig;

    /**
     * 查询激活信息
     * 查询策略：先用SN码查询，如果失败再用IMEI查询，两次都失败返回匹配失败
     *
     * @param typeCode 手机类型编号
     * @param code     SN码
     * @param imei     IMEI码（可选，用于SN查询失败时的重试，同时作为 imei1 返回）
     * @param imei2    IMEI2 码（可选，双卡设备的第二个 IMEI）
     * @return 激活信息
     */
    @ApiOperation("查询激活信息")
    @PostMapping("/queryActiveInfo")
    public R queryActiveInfo(@RequestParam("typeCode") String typeCode,
                             @RequestParam("code") String code,
                             @RequestParam("infoId") Long infoId,
                             @RequestParam(value = "imei", required = false) String imei,
                             @RequestParam(value = "imei2", required = false) String imei2,
                             @RequestParam(value = "imagePath", required = false) String imagePath,
                             @RequestParam(value = "imageUrl", required = false) String imageUrl,
                             @RequestParam(value = "img", required = false) MultipartFile img,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             @RequestParam(value = "skipApiCall", required = false, defaultValue = "false") Boolean skipApiCall) {
        // 跳过06 API调用（无旧手机场景：新增用户 / 旧手机损坏丢失等）
        if (Boolean.TRUE.equals(skipApiCall)) {
            log.info("skipApiCall=true，跳过06 API查询，直接进入下一步。typeCode={}, code={}, imei={}, imei2={}", typeCode, code, imei, imei2);
            return handleSkipApiCall(typeCode, code, imei, imei2, imagePath, imageUrl, img, file, infoId);
        }

        String type = PhoneType.getValueByCode(typeCode);
        if (type == null) {
            return R.fail("查询失败，无效的手机类型！");
        }

        String resolvedImagePath;
        try {
            resolvedImagePath = resolveImagePath(imagePath, imageUrl, img, file);
        } catch (Exception e) {
            log.error("处理上传图片失败", e);
            return R.fail("图片处理失败：" + e.getMessage());
        }

        // 第一次：用SN码查询
        log.info("第一次查询：使用SN码={}, type={}", code, type);
        ExternalApiService.ApiResult apiResult = externalApiService.fetchDataFromExternalApi(type, code);

        // 第二次：SN查询失败且有IMEI时，用IMEI重试
        if (!apiResult.getSuccess() && imei != null && !imei.trim().isEmpty()) {
            log.info("SN查询失败，第二次查询：使用IMEI={}, type={}", imei, type);
            apiResult = externalApiService.fetchDataFromExternalApi(type, imei.trim());
        }

        // 两次都失败，返回匹配失败
        if (!apiResult.getSuccess()) {
            return R.fail("查询失败，请检查输入是否正确！" + apiResult.getData());
        }

        // 存库，封装（需要的信息返回，其他原始数据以json形式存库）
        PhoneInfoDto phoneInfoDto = handleObject(typeCode, apiResult.getData());
        if (phoneInfoDto == null) {
            return R.fail("数据解析失败");
        }
        // 前端传入的 sn/imei/imei2 优先级高于第三方 API 返回（因为第三方 API 对部分机型不返回 imei 或字段不可靠）
        overrideIdentifiersFromRequest(phoneInfoDto, code, imei, imei2);
        try {
            phoneInfoDto.setSysTime((String) redisTemplate.opsForValue().get(CacheConstants.SYS_CONFIG_KEY + Constants.SYSTEM_TIME_CACHE_KEY));
            phoneInfoDto.setImagePath(normalizeImagePath(resolvedImagePath));
            phoneInfoDto.setImageUrl(buildImageUrl(phoneInfoDto.getImagePath()));
            Long id = saveActiveInfo(phoneInfoDto, apiResult.getRawJson(), typeCode, phoneInfoDto.getImagePath(), infoId);
            phoneInfoDto.setId(id);
        } catch (Exception e) {
            log.error("保存数据失败", e);
            return R.fail("保存数据失败：" + e.getMessage());
        }
        return R.ok(phoneInfoDto);
    }

    /**
     * 跳过06 API查询，直接构造返回信息（无旧手机场景）
     * 用于以下场景：
     * - 新增用户（无旧手机）
     * - 旧手机损坏/丢失（后续由前端根据旧手机使用时长判断跳转签协议或亚丁）
     */
    private R handleSkipApiCall(String typeCode, String code, String imei, String imei2,
                                 String imagePath, String imageUrl, MultipartFile img,
                                 MultipartFile file, Long infoId) {
        String resolvedImagePath;
        try {
            resolvedImagePath = resolveImagePath(imagePath, imageUrl, img, file);
        } catch (Exception e) {
            log.error("处理上传图片失败", e);
            return R.fail("图片处理失败：" + e.getMessage());
        }

        // 构造 PhoneInfoDto，直接使用前端传入的信息，无需调用06 API
        PhoneInfoDto phoneInfoDto = new PhoneInfoDto();
        String trimmedCode = code == null ? "" : code.trim();
        String trimmedImei = imei == null ? "" : imei.trim();
        String trimmedImei2 = imei2 == null ? "" : imei2.trim();

        // 识别 code 是 SN 还是 IMEI：如果 code 是纯数字且长度>=15，则可能是 IMEI
        if (!trimmedCode.isEmpty() && trimmedCode.matches("\\d{15,17}")) {
            phoneInfoDto.setImei1(trimmedCode);
            if (!trimmedImei.isEmpty()) {
                phoneInfoDto.setImei2(trimmedImei);
            }
        } else {
            phoneInfoDto.setSn(trimmedCode);
            phoneInfoDto.setImei1(trimmedImei);
            phoneInfoDto.setImei2(trimmedImei2);
        }

        // 无旧手机场景，不设置 model/activated 等06 API返回的字段
        phoneInfoDto.setSysTime((String) redisTemplate.opsForValue().get(CacheConstants.SYS_CONFIG_KEY + Constants.SYSTEM_TIME_CACHE_KEY));
        phoneInfoDto.setImagePath(normalizeImagePath(resolvedImagePath));
        phoneInfoDto.setImageUrl(buildImageUrl(phoneInfoDto.getImagePath()));

        try {
            Long id = saveActiveInfo(phoneInfoDto, null, typeCode, phoneInfoDto.getImagePath(), infoId, 1);
            phoneInfoDto.setId(id);
        } catch (Exception e) {
            log.error("保存数据失败（skipApiCall）", e);
            return R.fail("保存数据失败：" + e.getMessage());
        }
        return R.ok(phoneInfoDto);
    }

    /**
     * 协议签订
     */
    @ApiOperation("协议签订")
    @PostMapping("/signContract")
    public R signContract(@RequestParam(value = "id", required = false) Long id,
                          @RequestParam(value = "contractId", required = false) Long contractId,
                          @RequestParam(value = "contractPath", required = false) String contractPath,
                          @RequestParam(value = "signatureFile", required = false) MultipartFile signatureFile,
                          @RequestParam(value = "signaturePath", required = false) String signaturePath,
                          @RequestParam(value = "signatureModel", required = false) String signatureModel,
                          @RequestParam(value = "signatureImei", required = false) String signatureImei,
                          @RequestParam(value = "signatureDate", required = false) String signatureDate) {
        PhoneActiveInfo info = new PhoneActiveInfo();
        // 根据协议id查询协议
        Contract contract = new Contract();
        contract.setId(contractId.intValue());
        Contract contract1 = contractService.queryContractById(contract);
        if (contract1 != null && contract1.getStatus()) {
            info.setContractContent(contract1.getContent());
        } else {
            return R.fail("协议不存在或不是生效中协议，请检查后重试");
        }
        info.setId(id);
        info.setContractId(contractId);
        info.setContractPath(contractPath);
        info.setSignatureModel(signatureModel);
        info.setSignatureImei(signatureImei);
        info.setSignatureDate(signatureDate);

        // 处理手写签名图片
        String resolvedSignaturePath = null;
        try {
            resolvedSignaturePath = resolveSignaturePath(signatureFile, signaturePath);
        } catch (Exception e) {
            log.error("处理手写签名图片失败", e);
            return R.fail("签名图片处理失败：" + e.getMessage());
        }
        info.setSignaturePath(resolvedSignaturePath);

        Long l = phoneActiveInfoService.saveOrUpdateActiveInfo(info);
        if (l == null) {
            return R.fail("协议签订失败，请稍后重试");
        }
        return R.ok();
    }
    /**
     * 处理不同厂商手机类型数据
     * @param type 手机类型
     * @param object 激活信息
     * @return
     */
    private PhoneInfoDto handleObject(String type, Object object) {
        PhoneInfoDto dto = new PhoneInfoDto();
        if (object == null) {
            return dto;
        }
        // 1. 将 Map 转换为强类型对象
        MapToObjectConverter objectConverter = converterContext.getObjectConverter(type);
        if (objectConverter == null) {
            // 未找到对应策略，可记录日志或抛出异常
            log.warn("No object converter found for type: {}", type);
            return dto;
        }
        Object typedObject = objectConverter.convert((Map<String, Object>) object);

        // 2. 使用原有策略填充 DTO
        PhoneInfoConverter converter = converterContext.getConverter(type);
        if (converter != null) {
            converter.convert(typedObject, dto);
        } else {
            // 未找到对应策略，可记录日志或抛出异常
            log.warn("No converter found for type: {}", type);
        }
        return dto;
    }

    /**
     * 使用前端传入的 sn/imei/imei2 覆盖 DTO 中的值。
     * 规则：
     * 1. imei 参数不为空则覆盖 imei1
     * 2. imei2 参数不为空则覆盖 imei2
     * 3. code 参数若与 imei/imei2 都不相同，则认为 code 是 SN，用其覆盖 sn
     * 4. 前端未传的字段保留 Converter 原填充值作兜底
     */
    private void overrideIdentifiersFromRequest(PhoneInfoDto dto, String code, String imei, String imei2) {
        if (dto == null) {
            return;
        }
        String trimmedCode = code == null ? null : code.trim();
        String trimmedImei = imei == null ? null : imei.trim();
        String trimmedImei2 = imei2 == null ? null : imei2.trim();

        if (!StringUtils.isEmpty(trimmedImei)) {
            dto.setImei1(trimmedImei);
        }
        if (!StringUtils.isEmpty(trimmedImei2)) {
            dto.setImei2(trimmedImei2);
        }
        if (!StringUtils.isEmpty(trimmedCode)
                && !trimmedCode.equals(trimmedImei)
                && !trimmedCode.equals(trimmedImei2)) {
            dto.setSn(trimmedCode);
        }
    }

    private Long saveActiveInfo(PhoneInfoDto dto, String rawJson, String typeCode, String imagePath, Long infoId) {
        return saveActiveInfo(dto, rawJson, typeCode, imagePath, infoId, 0);
    }

    private Long saveActiveInfo(PhoneInfoDto dto, String rawJson, String typeCode, String imagePath, Long infoId, Integer skipApiCall) {
        log.info("开始保存信息，skipApiCall={}, {}", skipApiCall, dto);
        PhoneActiveInfo info = new PhoneActiveInfo();
        info.setSn(dto.getSn());
        info.setPhoneType(typeCode);
        info.setImei1(dto.getImei1());
        info.setImei2(dto.getImei2());
        info.setModel(dto.getModel());
        info.setActivated(dto.getActivated());
        info.setActivateDate(dto.getActivateDate());
        info.setCoverage(dto.getCoverage());
        info.setActiveInfo(rawJson); // 原始完整 JSON
        info.setSysTime(dto.getSysTime());
        info.setImagePath(imagePath);
        info.setInfoId(infoId);
        info.setSkipApiCall(skipApiCall);
        // 设置创建人/更新人
        info.setCreateBy(getUsername());
        info.setUpdateBy(getUsername());
        info.setNickName(getNickName());
        return phoneActiveInfoService.saveOrUpdateActiveInfo(info);
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

    private String resolveSignaturePath(MultipartFile signatureFile, String signaturePath) throws Exception {
        // 优先使用上传的签名文件
        if (signatureFile != null && !signatureFile.isEmpty()) {
            return FileUploadUtils.upload(RuoYiConfig.getUploadPath(), signatureFile);
        }
        // 其次使用传入的签名路径
        if (!StringUtils.isEmpty(signaturePath)) {
            return signaturePath.trim();
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

    @ApiOperation("查询订单列表-个人")
    @GetMapping("/queryOrderList")
    public R queryOrderList() {
        startPage();
        String userName = getUsername();
        PhoneActiveInfo phoneActiveInfo = new PhoneActiveInfo();
        phoneActiveInfo.setCreateBy(userName);
        List<PhoneActiveInfo> list = phoneActiveInfoService.queryActiveList(phoneActiveInfo);
        return R.ok(getDataTable(list));
    }

    /**
     * @param ContractDto 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询协议信息")
    @GetMapping("/getContract")
    public R getContract() {
        Contract contract = new Contract();
        contract.setStatus(true);
        List<Contract> list = contractService.queryContractsByCondition(contract);
        if (list != null && list.size() > 0) {
            return R.ok(list.get(0));
        }
        return R.ok();
    }

    @Autowired
    private ISysConfigService configService;

    @ApiOperation("查询订单列表-个人")
    @GetMapping("/test")
    public R test(){
        System.out.println("定时任务--------系统时间更新开始");
        String systemDate = (String)redisTemplate.opsForValue().get(CacheConstants.SYS_CONFIG_KEY + Constants.SYSTEM_TIME_CACHE_KEY);
        SysConfig config = new SysConfig();
        config.setConfigKey(Constants.SYSTEM_TIME_CACHE_KEY);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate date = LocalDate.parse(systemDate, formatter);
        LocalDate nextDay = date.plusDays(1);

        System.out.println("原日期：" + date);
        System.out.println("加1天：" + nextDay);

        config.setConfigValue(nextDay.format(formatter));
        config.setConfigId(100L);
        configService.updateConfig(config);
        System.out.println("定时任务--------系统时间更新完成");
        return R.ok();
    }


    @Autowired
    private LeaveInformationService leaveInformationService;

    /**
     * 新增留资信息
     *
     * @param leaveInformationDto 留资信息信息
     * @return 操作结果
     */
    @PostMapping("/insertLeaveInformation")
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
     * @param context 手机号或姓名
     * @return 订单分页列表
     */
    @ApiOperation("查询留资信息列表")
    @GetMapping("/getLeaveInformationList")
    public TableDataInfo getLeaveInformationList(@RequestParam(value = "context") String context) {
        startPage();
        List<LeaveInformation> list = leaveInformationService.queryInfoByNameOrNum(context);
        return getDataTable(list);
    }


    @Autowired
    private CompensationOrderService compensationOrderService;

    /**
     * 新增赔付订单
     *
     * @param compensationOrderDto 赔付订单信息
     * @return 操作结果
     */
    @PostMapping("/insertCompensationOrder")
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
     * @param compensationOrderDto 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询赔付订单信息")
    @GetMapping("/getCompensationOrderList")
    public TableDataInfo getCompensationOrderList(CompensationOrderDto compensationOrderDto) {
        startPage();
        List<CompensationOrderReturnDto> list = compensationOrderService.queryCompensationOrderByCondition(compensationOrderDto);
        return getDataTable(list);
    }

    /**
     * 更新赔付订单
     *
     * @param compensationOrder 赔付订单（必须包含ID）
     * @return 操作结果
     */
    @PostMapping("/updateCompensationOrder")
    public R updateCompensationOrder(@RequestBody @Validated CompensationOrder compensationOrder) {
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
     * @param phoneActiveInfo 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询已签约订单列表")
    @GetMapping("/querySignContractOrderList")
    public R querySignContractOrderList(PhoneActiveInfo phoneActiveInfo) {
        startPage();
        phoneActiveInfo.setIsSignature(1);
        List<PhoneActiveInfo> list = phoneActiveInfoService.queryActiveList(phoneActiveInfo);
        return R.ok(getDataTable(list));
    }
}