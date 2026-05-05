package com.ruoyi.web.controller.wechat;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.web.core.config.PhoneInfoConverterContext;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import com.ruoyi.web.service.MapToObjectConverter;
import com.ruoyi.web.service.PhoneInfoConverter;
import com.ruoyi.web.service.impl.ExternalApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
                             @RequestParam(value = "imei", required = false) String imei,
                             @RequestParam(value = "imei2", required = false) String imei2,
                             @RequestParam(value = "imagePath", required = false) String imagePath,
                             @RequestParam(value = "imageUrl", required = false) String imageUrl,
                             @RequestParam(value = "img", required = false) MultipartFile img,
                             @RequestParam(value = "file", required = false) MultipartFile file) {
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
            saveActiveInfo(phoneInfoDto, apiResult.getRawJson(), typeCode, phoneInfoDto.getImagePath());
        } catch (Exception e) {
            log.error("保存数据失败", e);
            return R.fail("保存数据失败：" + e.getMessage());
        }
        return R.ok(phoneInfoDto);
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

    private void saveActiveInfo(PhoneInfoDto dto, String rawJson, String typeCode, String imagePath) {
        log.info("开始保存信息，{}", dto);
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
        // 设置创建人/更新人
        info.setCreateBy(getUsername());
        info.setUpdateBy(getUsername());
        info.setNickName(getNickName());
        phoneActiveInfoService.saveOrUpdateActiveInfo(info);
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
}