# 后端模板（以 LeaveInformation 实际代码为范本）

以下模板里的 `<Entity>`/`<entity>`/`<TABLE>` 为占位符：Entity=大驼峰类名（如 LeaveInformation），entity=小驼峰（如 leaveInformation），TABLE=表名（如 leave_information）。业务控制器统一放 `controller/system` 包。

## 1. Domain（com.ruoyi.web.domain）

```java
package com.ruoyi.web.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class <Entity> extends BaseEntity {

    @Excel(name = "ID")
    private Long id;

    /** 字段注释 */
    @Excel(name = "字段中文名")
    private String xxx;

    /** 字典字段示例 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private Integer status;

    /** 时间字段示例（createTime/updateTime 已在 BaseEntity，不要重复声明） */
    @Excel(name = "业务时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date bizTime;

}
```

## 2. Mapper 接口（com.ruoyi.web.mapper）

```java
package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.<Entity>;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface <Entity>Mapper {

    /**
     * 按条件查询列表
     *
     * @param <entity> 查询条件
     * @return 结果列表
     */
    List<<Entity>> selectListByExample(<Entity> <entity>);

    /**
     * 按ID查询
     */
    <Entity> selectById(@Param("id") Long id);

    /**
     * 新增，返回主键回填到 id 属性
     */
    int insert(<Entity> <entity>);

    /**
     * 按ID更新（null 字段不更新）
     */
    int updateById(<Entity> <entity>);

    /**
     * 按ID删除
     */
    int deleteById(@Param("id") Long id);
}
```

## 3. Mapper XML（ruoyi-admin/src/main/resources/mapper/<module>/<Entity>Mapper.xml）

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.web.mapper.<Entity>Mapper">

    <resultMap id="BaseResultMap" type="com.ruoyi.web.domain.<Entity>">
        <id column="id" property="id"/>
        <result column="snake_col" property="camelCol"/>
        <!-- 其余列逐行映射 -->
    </resultMap>

    <sql id="Base_Column_List">
        id, snake_col, status, create_time, create_by, update_time, update_by
    </sql>

    <select id="selectListByExample" parameterType="com.ruoyi.web.domain.<Entity>" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM <TABLE>
        <where>
            <if test="xxx != null and xxx != ''">AND xxx = #{xxx}</if>
            <if test="status != null">AND status = #{status}</if>
        </where>
        ORDER BY id DESC
    </select>

    <select id="selectById" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM <TABLE>
        WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO <TABLE> (snake_col, status, create_time, create_by)
        VALUES (#{camelCol}, #{status}, #{createTime}, #{createBy})
    </insert>

    <update id="updateById">
        UPDATE <TABLE>
        <set>
            <if test="xxx != null">xxx = #{xxx},</if>
            <if test="status != null">status = #{status},</if>
            update_time = now()
            <if test="updateBy != null">, update_by = #{updateBy}</if>
        </set>
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM <TABLE> WHERE id = #{id}
    </delete>

</mapper>
```

## 4. Service 接口（com.ruoyi.web.service）

```java
package com.ruoyi.web.service;

import com.ruoyi.web.domain.<Entity>;

import java.util.List;

public interface <Entity>Service {

    List<<Entity>> query<Entity>ByCondition(<Entity> <entity>);

    <Entity> get<Entity>ById(Long id);

    Long insert(<Entity> <entity>);

    int update(<Entity> <entity>);

    int delete(<Entity> <entity>);
}
```

## 5. ServiceImpl（com.ruoyi.web.service.impl）

```java
package com.ruoyi.web.service.impl;

import com.ruoyi.web.domain.<Entity>;
import com.ruoyi.web.mapper.<Entity>Mapper;
import com.ruoyi.web.service.<Entity>Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class <Entity>ServiceImpl implements <Entity>Service {

    @Autowired
    private <Entity>Mapper <entity>Mapper;

    @Override
    public List<<Entity>> query<Entity>ByCondition(<Entity> <entity>) {
        return <entity>Mapper.selectListByExample(<entity>);
    }

    @Override
    public <Entity> get<Entity>ById(Long id) {
        return <entity>Mapper.selectById(id);
    }

    @Override
    public Long insert(<Entity> <entity>) {
        <entity>Mapper.insert(<entity>);
        return <entity>.getId();
    }

    @Override
    public int update(<Entity> <entity>) {
        return <entity>Mapper.updateById(<entity>);
    }

    @Override
    public int delete(<Entity> <entity>) {
        return <entity>Mapper.deleteById(<entity>.getId());
    }
}
```

## 6. Controller（com.ruoyi.web.controller.system）

以 LeaveInformationController 为范本（R 返回 + try/catch + ExcelUtil 导入导出）：

```java
package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.web.domain.<Entity>;
import com.ruoyi.web.service.<Entity>Service;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.List;

@Api("<表注释>管理")
@Slf4j
@RestController
@RequestMapping("/<module>/<entity>")
public class <Entity>Controller extends BaseController {

    @Autowired
    private <Entity>Service <entity>Service;

    /**
     * 查询<表注释>分页列表
     */
    @ApiOperation("查询<表注释>列表")
    @GetMapping("/getList")
    public R getList(<Entity> <entity>) {
        startPage();
        List<<Entity>> list = <entity>Service.query<Entity>ByCondition(<entity>);
        return R.ok(getDataTable(list));
    }

    /**
     * 按ID查询详情
     */
    @GetMapping("/getDetail")
    public R getDetail(@RequestParam @NotNull(message = "ID不能为空") Long id) {
        return R.ok(<entity>Service.get<Entity>ById(id));
    }

    /**
     * 新增<表注释>
     */
    @ApiOperation("新增<表注释>")
    @PostMapping("/insert")
    public R insert(@RequestBody <Entity> <entity>) {
        try {
            <entity>.setCreateBy(getUsername());
            <entity>.setUpdateBy(getUsername());
            Long id = <entity>Service.insert(<entity>);
            return id > 0 ? R.ok(id) : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新<表注释>（必须包含ID）
     */
    @ApiOperation("更新<表注释>")
    @PostMapping("/update")
    public R update(@RequestBody <Entity> <entity>) {
        try {
            if (<entity>.getId() == null) {
                return R.fail("ID不能为空");
            }
            <entity>.setUpdateBy(getUsername());
            int rows = <entity>Service.update(<entity>);
            return rows > 0 ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除<表注释>
     */
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable("id") @NotNull(message = "ID不能为空") Long id) {
        try {
            <Entity> <entity> = new <Entity>();
            <entity>.setId(id);
            int rows = <entity>Service.delete(<entity>);
            return rows > 0 ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    // ---- 以下为 Excel 导入导出（importExport=true 时生成） ----

    /**
     * 导出<表注释>数据（按当前筛选条件，不分页）
     */
    @ApiOperation("导出<表注释>数据")
    @PostMapping("/export")
    public void export(HttpServletResponse response, <Entity> <entity>) {
        List<<Entity>> list = <entity>Service.query<Entity>ByCondition(<entity>);
        ExcelUtil<<Entity>> util = new ExcelUtil<>(<Entity>.class);
        util.exportExcel(response, list, "<表注释>");
    }

    /**
     * 下载导入模板
     */
    @ApiOperation("下载<表注释>导入模板")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<<Entity>> util = new ExcelUtil<>(<Entity>.class);
        util.importTemplateExcel(response, "<表注释>", "填写说明：xx 为必填项；请勿修改表头，从表头下一行开始填写");
    }

    /**
     * 导入<表注释>数据
     */
    @ApiOperation("导入<表注释>数据")
    @PostMapping("/importData")
    public R importData(MultipartFile file, @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception {
        ExcelUtil<<Entity>> util = new ExcelUtil<>(<Entity>.class);
        List<<Entity>> list = util.importExcel(file.getInputStream(), 0);
        // 按业务写导入处理（校验/去重/更新），返回成功失败明细到 msg，前端弹窗展示
        return R.ok(null, "导入成功 N 条");
    }
}
```

## 7. DTO（可选，com.ruoyi.web.model）

需要接收前端表单参数 + `@Validated` 校验时生成（如 LeaveInformationDto），Controller 里 `BeanUtils.copyProperties(dto, entity)` 转换。校验注解用 `javax.validation.constraints.*`（NotBlank/NotNull/Size）。

## 8. 菜单 SQL（doc/<entity>_menu.sql）

```sql
-- <表注释>管理菜单（parent_id 请按实际调整：目录=0，业务菜单挂载的目录按现有菜单结构）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
('业务目录', 0, 5, 'biz', NULL, NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'system', sysdate(), '<表注释>目录');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '<表注释>', menu_id, 1, '<entity>', '<module>/<entity>/index', NULL, '', 1, 0, 'C', '0', '0', '<module>:<entity>:list', 'table', 'admin', sysdate(), '<表注释>菜单'
FROM sys_menu WHERE menu_name = '业务目录';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', menu_id, 1, '', '', NULL, '', 1, 0, 'F', '0', '0', '<module>:<entity>:query', '#', 'admin', sysdate(), ''
FROM sys_menu WHERE menu_name = '<表注释>';

-- 按钮权限 <module>:<entity>:add / :edit / :remove 同理生成，菜单类型 F
```

说明：若依前端是动态路由，`views/<module>/<entity>/index.vue` 文件就位 + 菜单 component 路径正确即可访问，无需改 router。菜单 SQL 的 parent_id 生成前先 `SELECT menu_id FROM sys_menu WHERE menu_name='xx'` 核对现有目录结构。
