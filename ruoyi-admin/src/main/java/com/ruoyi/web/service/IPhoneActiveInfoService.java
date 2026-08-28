package com.ruoyi.web.service;

import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.domain.PhoneActiveInfoImport;
import com.ruoyi.web.domain.PhoneOrderContract;

import java.util.List;
import java.util.Map;

public interface IPhoneActiveInfoService {
    Long saveOrUpdateActiveInfo(PhoneActiveInfo info);

    /**
     * 保存或更新签约信息
     *
     * @param contract 签约信息（必须包含 orderId）
     */
    void saveOrUpdateContract(PhoneOrderContract contract);

    List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info);

    /**
     * 查询订单详情（全字段，包含详情表和签约表）
     *
     * @param id 订单ID
     * @return 订单详情，可能为null
     */
    PhoneActiveInfo getActiveDetail(Long id);

    /**
     * 导出查询（全字段，不分页）
     */
    List<PhoneActiveInfo> queryExportList(PhoneActiveInfo info);

    /**
     * 批量导入订单数据
     * <p>
     * 两阶段处理：
     * <ol>
     *     <li>先对全部行做字段校验（含门店匹配、文件内序列号去重），
     *         任何一行校验不通过则整个文件不导入，不会写入任何数据；</li>
     *     <li>全部校验通过后逐行入库，运行期冲突（如序列号已存在）仅影响对应行。</li>
     * </ol>
     * 返回结构化结果：
     * <ul>
     *     <li>total：总条数</li>
     *     <li>successCount / updateCount / failCount：成功、更新、失败条数</li>
     *     <li>errors：失败明细列表</li>
     *     <li>warns：提示信息列表（如自动创建留资记录）</li>
     *     <li>message：结果摘要（展示给用户）</li>
     * </ul>
     *
     * @param list 导入的数据列表
     * @param updateSupport 是否更新已存在的数据（根据sn判断）
     * @param operName 操作人
     * @return 结构化导入结果
     */
    Map<String, Object> importActiveInfo(List<PhoneActiveInfoImport> list, boolean updateSupport, String operName);

    /**
     * 首页统计卡片（管理员，可选门店维度）
     *
     * @param currentDate 当前业务日期
     * @param storeId     门店ID，null表示全部门店
     */
    Map<String, Object> getDashboardStatistics(String currentDate, Long storeId);

    Map<String, Object> getUserDashboardStatistics(String createBy, String currentDate);

    /**
     * 订单趋势（可选门店维度）
     *
     * @param period      week / month
     * @param createBy    创建人，null表示全部
     * @param currentDate 当前业务日期
     * @param storeId     门店ID，null表示全部门店
     */
    Map<String, Object> getOrderTrend(String period, String createBy, String currentDate, Long storeId);

    List<Map<String, Object>> getDeviceModelDistribution();

    List<Map<String, Object>> getSignatureModelDistribution();

    Map<String, Object> getMonthlyOrderStats(String currentDate, Long storeId);

    List<Map<String, Object>> getWarrantyStatus(String currentDate);

    List<PhoneActiveInfo> getRecentOrders(String createBy, int limit, Long storeId);

    /**
     * 门店列表（通信源手机部门的直接子部门）
     */
    List<Map<String, Object>> getStoreList();

    /**
     * 各门店订单对比统计（含无订单门店）
     */
    List<Map<String, Object>> getStoreComparison();

    /**
     * 根据订单ID查询签约时的协议内容（历史快照，不受协议模板表变更影响）
     *
     * @param id 订单ID
     * @return 协议内容富文本，可能为null
     */
    String getContractContent(Long id);

    /**
     * 根据用户名解析其所属门店ID
     * 从用户的部门出发，向上查找通信源手机 (201) 的直接子部门即为门店
     *
     * @param username 用户名
     * @return 门店ID (sys_dept.dept_id)，null表示无法解析
     */
    Long resolveStoreIdByUsername(String username);
}