import * as XLSX from "xlsx";
import { saveAs } from "file-saver";

/**
 * 通用前端导出 Excel 工具函数
 * 通过轮询列表接口分页获取所有数据，最终导出为 Excel 文件
 *
 * @param {Object} options 导出配置
 * @param {Function} options.fetchApi - 列表请求接口函数，接收查询参数对象，返回 Promise
 * @param {Object} options.queryParams - 当前筛选条件（查询参数）
 * @param {Array<Object>} options.columns - 列配置数组，每项支持以下格式：
 *   简写格式（推荐，与 el-table-column 风格一致）：
 *   - {string} label: Excel 表头名称（即列名）
 *   - {string} prop: 字段名，直接从数据行中取值
 *   - {Function} [formatter]: 自定义取值函数 (row) => value，有特殊处理时使用
 *   - {number} [width]: 列宽，默认 14
 *
 *   兼容旧格式：
 *   - {string} header: 等同于 label
 *   - {string|Function} field: 等同于 prop（字符串时）或 formatter（函数时）
 *
 * @param {string} [options.fileName] - 导出文件名（不含扩展名），默认 "导出数据"
 * @param {string} [options.sheetName] - Sheet 名称，默认 "Sheet1"
 * @param {number} [options.pageSize] - 每页获取条数，默认 100
 * @param {Function} [options.parseResponse] - 自定义解析接口返回数据的函数，
 *   接收 response，返回 { rows: [], total: 0 }，默认兼容 response.data.rows 和 response.rows
 * @returns {Promise<number>} 导出的数据总条数
 *
 * @example
 * // 在页面中使用（简写格式，与 el-table-column 的 label/prop 一致）
 * import { exportExcel } from "@/utils/export";
 *
 * handleExport() {
 *   exportExcel({
 *     fetchApi: queryOrderList,
 *     queryParams: this.queryParams,
 *     columns: [
 *       { label: "ID", prop: "id", width: 6 },
 *       { label: "名称", prop: "name", width: 20 },
 *       { label: "状态", formatter: (row) => row.active ? "启用" : "禁用", width: 10 },
 *     ],
 *     fileName: "订单数据",
 *     sheetName: "订单数据",
 *   });
 * }
 */
export function exportExcel({
  fetchApi,
  queryParams = {},
  columns = [],
  fileName = "导出数据",
  sheetName = "Sheet1",
  pageSize = 100,
  parseResponse,
}) {
  if (!fetchApi || typeof fetchApi !== "function") {
    return Promise.reject(new Error("fetchApi 必须是一个函数"));
  }
  if (!columns || columns.length === 0) {
    return Promise.reject(new Error("columns 列配置不能为空"));
  }

  // 标准化列配置，兼容新旧两种格式
  const normalizedColumns = columns.map((col) => {
    // 获取表头名称：优先 label，其次 header
    const header = col.label || col.header;
    // 获取取值方式：优先 formatter，其次 field（函数类型），最后 prop 或 field（字符串类型）
    let getValue;
    if (typeof col.formatter === "function") {
      getValue = col.formatter;
    } else if (typeof col.field === "function") {
      getValue = col.field;
    } else {
      const prop = col.prop || col.field;
      getValue = (row) => {
        const value = row[prop];
        return value !== undefined && value !== null ? value : "-";
      };
    }
    return {
      header,
      getValue,
      width: col.width || 14,
    };
  });

  // 默认的接口返回解析函数，兼容两种常见格式
  const defaultParseResponse = (response) => {
    const data = response.data || response;
    return {
      rows: data.rows || [],
      total: data.total || 0,
    };
  };

  const parseFn = parseResponse || defaultParseResponse;

  // 构建轮询查询参数
  const params = {
    ...queryParams,
    pageNum: 1,
    pageSize: pageSize,
  };

  // 递归轮询获取所有分页数据
  const allData = [];
  const fetchPage = (pageNum) => {
    return fetchApi({ ...params, pageNum }).then((response) => {
      const { rows, total } = parseFn(response);
      allData.push(...rows);
      // 如果还有下一页，继续获取
      if (pageNum * pageSize < total) {
        return fetchPage(pageNum + 1);
      }
    });
  };

  return fetchPage(1).then(() => {
    if (allData.length === 0) {
      return Promise.reject(new Error("EMPTY_DATA"));
    }

    // 根据列配置映射导出数据
    const exportData = allData.map((row) => {
      const mappedRow = {};
      normalizedColumns.forEach((col) => {
        mappedRow[col.header] = col.getValue(row);
      });
      return mappedRow;
    });

    // 生成 Excel
    const ws = XLSX.utils.json_to_sheet(exportData);

    // 设置列宽
    ws["!cols"] = normalizedColumns.map((col) => ({
      wch: col.width,
    }));

    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, sheetName);

    const wbout = XLSX.write(wb, {
      bookType: "xlsx",
      type: "array",
    });

    saveAs(
      new Blob([wbout], { type: "application/octet-stream" }),
      `${fileName}_${new Date().getTime()}.xlsx`
    );

    return allData.length;
  });
}
