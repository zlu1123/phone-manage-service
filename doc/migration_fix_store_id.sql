-- ============================================================
-- 批量修正 phone_active_info.store_id
-- 根据 create_by 向上查找门店（parent_id = 201 的直接子部门）
-- 数据库: 124.222.38.87 phone-manage
-- ============================================================

-- Step 1: 预览 — 查看每个 create_by 会被分配到哪个门店
SELECT
    pai.create_by,
    pai.store_id   AS old_store_id,
    CASE
        WHEN sd1.parent_id = 201 THEN sd1.dept_id
        WHEN sd2.parent_id = 201 THEN sd2.dept_id
        WHEN sd3.parent_id = 201 THEN sd3.dept_id
        WHEN sd4.parent_id = 201 THEN sd4.dept_id
        WHEN sd5.parent_id = 201 THEN sd5.dept_id
    END AS new_store_id,
    CASE
        WHEN sd1.parent_id = 201 THEN sd1.dept_name
        WHEN sd2.parent_id = 201 THEN sd2.dept_name
        WHEN sd3.parent_id = 201 THEN sd3.dept_name
        WHEN sd4.parent_id = 201 THEN sd4.dept_name
        WHEN sd5.parent_id = 201 THEN sd5.dept_name
    END AS store_name,
    COUNT(*)       AS order_count
FROM phone_active_info pai
JOIN sys_user su ON pai.create_by = su.user_name
LEFT JOIN sys_dept sd1 ON su.dept_id    = sd1.dept_id   -- 用户直属部门
LEFT JOIN sys_dept sd2 ON sd1.parent_id = sd2.dept_id   -- 上级
LEFT JOIN sys_dept sd3 ON sd2.parent_id = sd3.dept_id   -- 再上级
LEFT JOIN sys_dept sd4 ON sd3.parent_id = sd4.dept_id   -- 再上级
LEFT JOIN sys_dept sd5 ON sd4.parent_id = sd5.dept_id   -- 再上级
WHERE pai.create_by IS NOT NULL AND pai.create_by != ''
GROUP BY pai.create_by, pai.store_id, new_store_id, store_name
ORDER BY store_name, pai.create_by;


-- Step 2: 检查无法匹配门店的账号（需要手动处理）
SELECT DISTINCT pai.create_by
FROM phone_active_info pai
LEFT JOIN sys_user su ON pai.create_by = su.user_name
WHERE su.user_name IS NULL
   OR su.dept_id IS NULL
   OR pai.create_by IS NULL
   OR pai.create_by = '';


-- Step 3: 确认无误后，执行更新（仅更新 store_id 为空或不一致的记录）
UPDATE phone_active_info pai
JOIN sys_user su ON pai.create_by = su.user_name
LEFT JOIN sys_dept sd1 ON su.dept_id    = sd1.dept_id
LEFT JOIN sys_dept sd2 ON sd1.parent_id = sd2.dept_id
LEFT JOIN sys_dept sd3 ON sd2.parent_id = sd3.dept_id
LEFT JOIN sys_dept sd4 ON sd3.parent_id = sd4.dept_id
LEFT JOIN sys_dept sd5 ON sd4.parent_id = sd5.dept_id
SET pai.store_id = (
    CASE
        WHEN sd1.parent_id = 201 THEN sd1.dept_id
        WHEN sd2.parent_id = 201 THEN sd2.dept_id
        WHEN sd3.parent_id = 201 THEN sd3.dept_id
        WHEN sd4.parent_id = 201 THEN sd4.dept_id
        WHEN sd5.parent_id = 201 THEN sd5.dept_id
        ELSE pai.store_id
    END
)
WHERE pai.create_by IS NOT NULL AND pai.create_by != '';
