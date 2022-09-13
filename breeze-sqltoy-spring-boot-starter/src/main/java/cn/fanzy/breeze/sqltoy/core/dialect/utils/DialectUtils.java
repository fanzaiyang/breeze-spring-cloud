/**
 * 
 */
package cn.fanzy.breeze.sqltoy.core.dialect.utils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import cn.fanzy.breeze.sqltoy.core.SqlExecuteStat;
import cn.fanzy.breeze.sqltoy.core.SqlToyConstants;
import cn.fanzy.breeze.sqltoy.core.SqlToyContext;
import cn.fanzy.breeze.sqltoy.core.callback.CallableStatementResultHandler;
import cn.fanzy.breeze.sqltoy.core.callback.DecryptHandler;
import cn.fanzy.breeze.sqltoy.core.callback.GenerateSavePKStrategy;
import cn.fanzy.breeze.sqltoy.core.callback.GenerateSqlHandler;
import cn.fanzy.breeze.sqltoy.core.callback.LockSqlHandler;
import cn.fanzy.breeze.sqltoy.core.callback.PreparedStatementResultHandler;
import cn.fanzy.breeze.sqltoy.core.callback.ReflectPropsHandler;
import cn.fanzy.breeze.sqltoy.core.callback.RowCallbackHandler;
import cn.fanzy.breeze.sqltoy.core.callback.UniqueSqlHandler;
import cn.fanzy.breeze.sqltoy.core.callback.UpdateRowHandler;
import cn.fanzy.breeze.sqltoy.core.config.SqlConfigParseUtils;
import cn.fanzy.breeze.sqltoy.core.config.model.EntityMeta;
import cn.fanzy.breeze.sqltoy.core.config.model.FieldMeta;
import cn.fanzy.breeze.sqltoy.core.config.model.FieldSecureConfig;
import cn.fanzy.breeze.sqltoy.core.config.model.PKStrategy;
import cn.fanzy.breeze.sqltoy.core.config.model.ShardingStrategyConfig;
import cn.fanzy.breeze.sqltoy.core.config.model.SqlParamsModel;
import cn.fanzy.breeze.sqltoy.core.config.model.SqlToyConfig;
import cn.fanzy.breeze.sqltoy.core.config.model.SqlToyResult;
import cn.fanzy.breeze.sqltoy.core.config.model.SqlWithAnalysis;
import cn.fanzy.breeze.sqltoy.core.config.model.TableCascadeModel;
import cn.fanzy.breeze.sqltoy.core.dialect.model.SavePKStrategy;
import cn.fanzy.breeze.sqltoy.core.exception.DataAccessException;
import cn.fanzy.breeze.sqltoy.core.model.IgnoreCaseSet;
import cn.fanzy.breeze.sqltoy.core.model.IgnoreKeyCaseMap;
import cn.fanzy.breeze.sqltoy.core.model.LockMode;
import cn.fanzy.breeze.sqltoy.core.model.QueryExecutor;
import cn.fanzy.breeze.sqltoy.core.model.QueryResult;
import cn.fanzy.breeze.sqltoy.core.model.SecureType;
import cn.fanzy.breeze.sqltoy.core.model.StoreResult;
import cn.fanzy.breeze.sqltoy.core.model.inner.QueryExecutorExtend;
import cn.fanzy.breeze.sqltoy.core.plugins.UnifyFieldsHandler;
import cn.fanzy.breeze.sqltoy.core.plugins.secure.DesensitizeProvider;
import cn.fanzy.breeze.sqltoy.core.plugins.secure.FieldsSecureProvider;
import cn.fanzy.breeze.sqltoy.core.plugins.sharding.ShardingUtils;
import cn.fanzy.breeze.sqltoy.core.utils.BeanUtil;
import cn.fanzy.breeze.sqltoy.core.utils.CollectionUtil;
import cn.fanzy.breeze.sqltoy.core.utils.DataSourceUtils.DBType;
import cn.fanzy.breeze.sqltoy.core.utils.ReservedWordsUtil;
import cn.fanzy.breeze.sqltoy.core.utils.ResultUtils;
import cn.fanzy.breeze.sqltoy.core.utils.SqlUtil;
import cn.fanzy.breeze.sqltoy.core.utils.SqlUtilsExt;
import cn.fanzy.breeze.sqltoy.core.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @project sqltoy-orm
 * @description 提供一些不同数据库都通用的逻辑处理,避免在各个数据库工具类中写重复代码
 * @author zhongxuchen
 * @version v1.0,Date:2014年12月26日
 * @modify {Date:2017-2-24,优化count sql处理逻辑,排除统计型查询导致的问题,本质统计性查询不应该用分页方式查询}
 * @modify {Date:2018-1-6,优化对数据库表字段默认值的处理,提供统一的处理方法}
 * @modify {Date:2018-1-22,增加业务主键生成赋值,同时对saveAll等操作返回生成的主键值映射到VO集合中}
 * @modify {Date:2018-5-3,修复getCountBySql关于剔除order by部分的逻辑错误}
 * @modify {Date:2018-9-25,修复select和from对称判断问题,影响分页查询时剔除from之前语句构建select
 *         count(1) from错误}
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DialectUtils {
	/**
	 * 定义日志
	 */
	protected final static Logger logger = LoggerFactory.getLogger(DialectUtils.class);

	// union 匹配模式
	public static final Pattern UNION_PATTERN = Pattern.compile("(?i)\\W+union\\W+");

	public static final Pattern ORDER_BY_PATTERN = Pattern.compile("(?i)\\Worder\\s+by\\W");

	public static final Pattern GROUP_BY_PATTERN = Pattern.compile("(?i)\\Wgroup\\s+by\\W");

	/**
	 * 存储过程格式
	 */
	public static final Pattern STORE_PATTERN = Pattern.compile("^(\\s*\\{)?\\s*\\?");

	// distinct 匹配模式
	public static final Pattern DISTINCT_PATTERN = Pattern.compile("(?i)^select\\s+distinct\\s+");

	/**
	 * 统计正则表达式
	 */
	public static final Pattern STAT_PATTERN = Pattern
			.compile("\\W(sum|avg|min|max|first|last|first_value|last_value)\\(");

	/**
	 * 查询select 匹配
	 */
	private static final String SELECT_REGEX = "select\\s+";

	/**
	 * 查询from 匹配
	 */
	private static final String FROM_REGEX = "\\s+from[\\(\\s+]";

	private static final String WHERE_REGEX = "\\s+where[\\(\\s+]";

	private static final HashMap<String, String> QuesFilters = new HashMap<String, String>() {
		private static final long serialVersionUID = 7135705054559913831L;
		{
			put("'", "'");
			put("\"", "\"");
		}
	};

	/**
	 * @todo 处理分页sql的参数
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param queryExecutor
	 * @param pageSql
	 * @param startIndex
	 * @param endIndex
	 * @return
	 * @throws Exception
	 */
	public static SqlToyResult wrapPageSqlParams(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, String pageSql, Object startIndex, Object endIndex, String dialect)
			throws Exception {
		QueryExecutorExtend extend = queryExecutor.getInnerModel();
		String[] paramsNamed = extend.getParamsName(sqlToyConfig);
		Object[] paramsValue = extend.getParamsValue(sqlToyContext, sqlToyConfig);
		if (startIndex == null && endIndex == null) {
			return SqlConfigParseUtils.processSql(pageSql, paramsNamed, paramsValue, dialect);
		}
		String[] realParamNamed = null;
		Object[] realParamValue = null;
		int paramLength;
		// 分页2个参数，top一个参数
		int extendSize = (endIndex == null) ? 1 : 2;
		if (sqlToyConfig.isNamedParam()) {
			paramLength = paramsNamed.length;
			// 防止传null
			if (paramsValue == null) {
				paramsValue = new Object[paramLength];
			}
			realParamValue = new Object[paramLength + extendSize];
			// 设置开始记录和截止记录参数名称和对应的值
			realParamNamed = new String[paramLength + extendSize];
			if (paramLength > 0) {
				System.arraycopy(paramsNamed, 0, realParamNamed, 0, paramLength);
				System.arraycopy(paramsValue, 0, realParamValue, 0, paramLength);
			}
			realParamNamed[paramLength] = SqlToyConstants.PAGE_FIRST_PARAM_NAME;
			realParamValue[paramLength] = startIndex;
			if (extendSize == 2) {
				realParamNamed[paramLength + 1] = SqlToyConstants.PAGE_LAST_PARAM_NAME;
				realParamValue[paramLength + 1] = endIndex;
			}
		} else {
			int totalParamCnt = getParamsCount(sqlToyConfig.getSql(null));
			// sql中没有?条件参数
			if (totalParamCnt == 0) {
				realParamValue = new Object[extendSize];
				realParamValue[0] = startIndex;
				if (extendSize == 2) {
					realParamValue[1] = endIndex;
				}
			} else {
				if (paramsValue == null && totalParamCnt > 0) {
					paramsValue = new Object[totalParamCnt];
				}
				paramLength = (paramsValue == null) ? 0 : paramsValue.length;
				realParamValue = new Object[paramLength + extendSize];
				if (sqlToyConfig.isHasFast()) {
					int tailSqlParamCnt = getParamsCount(sqlToyConfig.getFastTailSql(null));
					// @fast() tail 前面部分参数数量
					int tailPreParamCnt = totalParamCnt - tailSqlParamCnt;
					System.arraycopy(paramsValue, 0, realParamValue, 0, tailPreParamCnt);
					realParamValue[tailPreParamCnt] = startIndex;
					if (extendSize == 2) {
						realParamValue[tailPreParamCnt + 1] = endIndex;
					}
					if (tailSqlParamCnt > 0) {
						System.arraycopy(paramsValue, tailPreParamCnt, realParamValue, tailPreParamCnt + extendSize,
								tailSqlParamCnt);
					}
				} else {
					if (paramLength > 0) {
						System.arraycopy(paramsValue, 0, realParamValue, 0, paramLength);
					}
					realParamValue[paramLength] = startIndex;
					if (extendSize == 2) {
						realParamValue[paramLength + 1] = endIndex;
					}
				}
			}
		}
		// 通过参数处理最终的sql和参数值
		return SqlConfigParseUtils.processSql(pageSql, realParamNamed, realParamValue, dialect);
	}

	/**
	 * @todo 实现普通的sql语句查询
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param sql
	 * @param paramsValue
	 * @param rowCallbackHandler
	 * @param decryptHandler     解密
	 * @param conn
	 * @param dbType
	 * @param startIndex
	 * @param fetchSize
	 * @param maxRows
	 * @return
	 * @throws Exception
	 */
	public static QueryResult findBySql(final SqlToyContext sqlToyContext, final SqlToyConfig sqlToyConfig,
			final String sql, final Object[] paramsValue, final RowCallbackHandler rowCallbackHandler,
			final DecryptHandler decryptHandler, final Connection conn, final Integer dbType, final int startIndex,
			final int fetchSize, final int maxRows) throws Exception {
		// 做sql签名
		String lastSql = SqlUtilsExt.signSql(sql, dbType, sqlToyConfig);
		// 打印sql
		SqlExecuteStat.showSql("执行查询", lastSql, paramsValue);
		PreparedStatement pst = conn.prepareStatement(lastSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		if (fetchSize > 0) {
			pst.setFetchSize(fetchSize);
		}
		if (maxRows > 0) {
			pst.setMaxRows(maxRows);
		}
		ResultSet rs = null;
		return (QueryResult) SqlUtil.preparedStatementProcess(null, pst, rs, new PreparedStatementResultHandler() {
			@Override
			public void execute(Object obj, PreparedStatement pst, ResultSet rs) throws Exception {
				SqlUtil.setParamsValue(sqlToyContext.getTypeHandler(), conn, dbType, pst, paramsValue, null, 0);
				rs = pst.executeQuery();
				this.setResult(ResultUtils.processResultSet(sqlToyContext, sqlToyConfig, conn, rs, rowCallbackHandler,
						null, decryptHandler, startIndex));
			}
		});
	}

	/**
	 * @todo 实现普通的sql语句查询
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param sql
	 * @param paramsValue
	 * @param updateRowHandler
	 * @param conn
	 * @param dbType
	 * @param startIndex
	 * @param fetchSize
	 * @param maxRows
	 * @return
	 * @throws Exception
	 */
	public static QueryResult updateFetchBySql(final SqlToyContext sqlToyContext, final SqlToyConfig sqlToyConfig,
			final String sql, final Object[] paramsValue, final UpdateRowHandler updateRowHandler,
			final Connection conn, final Integer dbType, final int startIndex, final int fetchSize, final int maxRows)
			throws Exception {
		// 做sql签名
		String lastSql = SqlUtilsExt.signSql(sql, dbType, sqlToyConfig);
		// 打印sql
		SqlExecuteStat.showSql("执行updateFetch", lastSql, paramsValue);
		PreparedStatement pst = null;
		if (updateRowHandler == null) {
			pst = conn.prepareStatement(lastSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		} else {
			pst = conn.prepareStatement(lastSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}
		if (fetchSize > 0) {
			pst.setFetchSize(fetchSize);
		}
		if (maxRows > 0) {
			pst.setMaxRows(maxRows);
		}
		ResultSet rs = null;
		return (QueryResult) SqlUtil.preparedStatementProcess(null, pst, rs, new PreparedStatementResultHandler() {
			@Override
			public void execute(Object obj, PreparedStatement pst, ResultSet rs) throws Exception {
				SqlUtil.setParamsValue(sqlToyContext.getTypeHandler(), conn, dbType, pst, paramsValue, null, 0);
				rs = pst.executeQuery();
				this.setResult(ResultUtils.processResultSet(sqlToyContext, sqlToyConfig, conn, rs, null,
						updateRowHandler, null, startIndex));
			}
		});
	}

	/**
	 * @todo 通用的查询记录总数(包含剔除order by和智能判断是直接select count from ()
	 *       还是直接剔除from之前的语句补充select count)
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param sql
	 * @param paramsValue
	 * @param isLastSql
	 * @param conn
	 * @param dbType
	 * @return
	 * @throws Exception
	 */
	public static Long getCountBySql(final SqlToyContext sqlToyContext, final SqlToyConfig sqlToyConfig,
			final String sql, final Object[] paramsValue, final boolean isLastSql, final Connection conn,
			final Integer dbType) throws Exception {
		String lastCountSql;
		int paramCnt = 0;
		int withParamCnt = 0;
		// 通过配置直接给定的最优化count 语句
		if (isLastSql) {
			lastCountSql = sql;
		} else {
			String countPart = " count(1) ";
			// es count(1) 不起作用
			if (dbType.equals(DBType.ES)) {
				countPart = " count(*) ";
			}
			String query_tmp = sql;
			String withSql = "";
			// with as分析器(避免每次做with 检测,提升效率)
			if (sqlToyConfig != null && sqlToyConfig.isHasWith()) {
				SqlWithAnalysis sqlWith = new SqlWithAnalysis(sql);
				query_tmp = sqlWith.getRejectWithSql();
				withSql = sqlWith.getWithSql();
			}
			int lastBracketIndex = query_tmp.lastIndexOf(")");
			int sql_from_index = 0;
			// sql不以from开头，截取from 后的部分语句
			if (StringUtil.indexOfIgnoreCase(query_tmp, "from") != 0) {
				sql_from_index = StringUtil.getSymMarkMatchIndex(SELECT_REGEX, FROM_REGEX, query_tmp.toLowerCase(), 0);
			}
			// 剔除order提高运行效率
			int orderByIndex = StringUtil.matchLastIndex(query_tmp, ORDER_BY_PATTERN);
			// order by 在from 之后
			if (orderByIndex > sql_from_index) {
				// 剔除order by 语句
				if (orderByIndex > lastBracketIndex) {
					query_tmp = query_tmp.substring(0, orderByIndex + 1);
				} else {
					// 剔除掉order by 后面语句对称的() 内容
					String orderJudgeSql = clearDisturbSql(query_tmp.substring(orderByIndex + 1));
					// 在order by 不在子查询内,说明可以整体切除掉order by
					if (orderJudgeSql.indexOf(")") == -1) {
						query_tmp = query_tmp.substring(0, orderByIndex + 1);
					}
				}
			}
			int groupIndex = StringUtil.matchLastIndex(query_tmp, GROUP_BY_PATTERN);
			// 判断group by 是否是内层，如select * from (select * from table group by)
			// 外层group by 必须要进行包裹(update by chenrenfei 2016-4-21)
			boolean isInnerGroup = false;
			if (groupIndex != -1) {
				isInnerGroup = clearDisturbSql(query_tmp.substring(groupIndex + 1)).lastIndexOf(")") != -1;
			}
			final StringBuilder countQueryStr = new StringBuilder();
			// 是否包含union,update 2012-11-21
			boolean hasUnion = StringUtil.matches(query_tmp, UNION_PATTERN);
			// 不包含distinct和group by 等,则剔除[select * ] from 变成select count(1) from
			// 性能最优
			if (!StringUtil.matches(query_tmp.trim(), DISTINCT_PATTERN) && !hasUnion
					&& (groupIndex == -1 || (groupIndex < lastBracketIndex && isInnerGroup))) {
				int selectIndex = StringUtil.matchIndex(query_tmp.toLowerCase(), SELECT_REGEX);
				// 截取出select 和from之间的语句
				String selectFields = (sql_from_index < 1) ? ""
						: query_tmp.substring(selectIndex + 6, sql_from_index).toLowerCase();
				// 剔除嵌套的子查询语句中select 和 from 之间的内容,便于判断统计函数的作用位置
				selectFields = clearSymSelectFromSql(selectFields);
				// 存在统计函数 update by chenrenfei ,date: 2017-2-24
				if (StringUtil.matches(selectFields, STAT_PATTERN)) {
					countQueryStr.append("select ").append(countPart).append(" from (").append(query_tmp)
							.append(") sag_count_tmpTable ");
				} else {
					// 截取from后的部分
					countQueryStr.append("select ").append(countPart)
							.append((sql_from_index != -1 ? query_tmp.substring(sql_from_index) : query_tmp));
				}
			} // 包含distinct 或包含union则直接将查询作为子表(普通做法)
			else {
				countQueryStr.append("select ").append(countPart).append(" from (").append(query_tmp)
						.append(") sag_count_tmpTable ");
			}
			paramCnt = getParamsCount(countQueryStr.toString());
			withParamCnt = getParamsCount(withSql);
			countQueryStr.insert(0, withSql + " ");
			lastCountSql = countQueryStr.toString();
		}
		final int paramCntFin = paramCnt;
		final int withParamCntFin = withParamCnt;
		Object[] realParamsTemp = null;
		if (paramsValue != null) {
			// 将from前的参数剔除
			realParamsTemp = isLastSql ? paramsValue
					: CollectionUtil.subtractArray(paramsValue, withParamCntFin,
							paramsValue.length - paramCntFin - withParamCntFin);
		}
		final Object[] realParams = realParamsTemp;
		// 做sql签名
		lastCountSql = SqlUtilsExt.signSql(lastCountSql, dbType, sqlToyConfig);
		// 打印sql
		SqlExecuteStat.showSql("执行count查询", lastCountSql, realParams);
		PreparedStatement pst = conn.prepareStatement(lastCountSql);
		ResultSet rs = null;
		return (Long) SqlUtil.preparedStatementProcess(null, pst, rs, new PreparedStatementResultHandler() {
			@Override
			public void execute(Object obj, PreparedStatement pst, ResultSet rs) throws SQLException, IOException {
				long resultCount = 0;
				if (realParams != null) {
					SqlUtil.setParamsValue(sqlToyContext.getTypeHandler(), conn, dbType, pst, realParams, null, 0);
				}
				rs = pst.executeQuery();
				if (rs.next()) {
					resultCount = rs.getLong(1);
				}
				this.setResult(resultCount);
			}
		});
	}

	/**
	 * @todo 统一将查询的sql参数由?形式变成:named形式(分页和查询随机记录时)
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param queryExecutor
	 * @param dialect
	 * @param wrapNamed     只在分页场景下需要将?模式传参统一成:name模式，便于跟后面分页startIndex和endIndex参数结合，从而利用sql预编译功能
	 * @return
	 * @throws Exception
	 */
	public static SqlToyConfig getUnifyParamsNamedConfig(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, String dialect, boolean wrapNamed) throws Exception {
		QueryExecutorExtend extend = queryExecutor.getInnerModel();
		// 本身就是:named参数形式或sql中没有任何参数
		boolean isNamed = false;
		// 在QueryExecutorBuilder中已经对wrappedParamNames做了判断赋值
		if (!extend.wrappedParamNames) {
			// sql中是否存在? 形式参数
			boolean hasQuestArg = SqlConfigParseUtils.hasQuestMarkArgs(sqlToyConfig.getSql());
			isNamed = ((extend.paramsName != null && extend.paramsName.length > 0) || !hasQuestArg);
		}
		// 以queryExecutor自定义的分表策略覆盖sql xml中定义的
		List<ShardingStrategyConfig> tableShardings = sqlToyConfig.getTableShardings();
		if (!extend.tableShardings.isEmpty()) {
			tableShardings = extend.tableShardings;
		}
		// sql条件以:named形式、无分表、无扩展缓存翻译则不存在对SqlToyConfig 内容的修改，直接返回
		if ((isNamed || !wrapNamed) && tableShardings.isEmpty() && extend.translates.isEmpty()) {
			return sqlToyConfig;
		}
		// clone sqltoyConfig避免直接修改原始的sql配置对后续执行产生影响
		SqlToyConfig result = sqlToyConfig.clone();
		// 存在扩展的缓存翻译
		if (!extend.translates.isEmpty()) {
			result.getTranslateMap().putAll(extend.translates);
		}
		// ?传参且分页模式,原因是分页存在取count场景，在@fast()情况下无法断定paramValues的值跟?的参数对应关系
		if (!isNamed && wrapNamed) {
			SqlParamsModel sqlParams;
			// 将?形式的参数替换成:named形式参数
			// 存在fast查询
			if (result.isHasFast()) {
				// @fast 前部分
				String fastPreSql = SqlConfigParseUtils.clearDblQuestMark(result.getFastPreSql(null));
				sqlParams = convertParamsToNamed(fastPreSql, 0);
				fastPreSql = SqlConfigParseUtils.recoverDblQuestMark(sqlParams.getSql());
				result.setFastPreSql(fastPreSql);
				int index = sqlParams.getParamCnt();
				// @fas() 中间部分
				String fastSql = SqlConfigParseUtils.clearDblQuestMark(result.getFastSql(null));
				sqlParams = convertParamsToNamed(fastSql, index);
				fastSql = SqlConfigParseUtils.recoverDblQuestMark(sqlParams.getSql());
				result.setFastSql(fastSql);
				index = index + sqlParams.getParamCnt();
				// 尾部
				String tailSql = SqlConfigParseUtils.clearDblQuestMark(result.getFastTailSql(null));
				sqlParams = convertParamsToNamed(tailSql, index);
				tailSql = SqlConfigParseUtils.recoverDblQuestMark(sqlParams.getSql());
				result.setFastTailSql(tailSql);
				// 完整sql
				result.setSql(fastPreSql.concat(" (").concat(fastSql).concat(") ").concat(tailSql));
				// 构造对应?参数个数的:named模式参数名数组
				String[] paramsName = new String[index];
				for (int i = 0; i < index; i++) {
					paramsName[i] = SqlToyConstants.DEFAULT_PARAM_NAME + (i + 1);
				}
				result.setParamsName(paramsName);
			} else {
				sqlParams = convertParamsToNamed(SqlConfigParseUtils.clearDblQuestMark(result.getSql(null)), 0);
				result.setSql(SqlConfigParseUtils.recoverDblQuestMark(sqlParams.getSql()));
				result.setParamsName(sqlParams.getParamsName());
			}
			// 自定义分页的count sql，一般无需定义
			sqlParams = convertParamsToNamed(SqlConfigParseUtils.clearDblQuestMark(result.getCountSql(null)), 0);
			result.setCountSql(SqlConfigParseUtils.recoverDblQuestMark(sqlParams.getSql()));
			// 清空方言缓存
			result.clearDialectSql();
			SqlConfigParseUtils.processFastWith(result, dialect);
		}
		// sharding table 替换sql中的表名称
		ShardingUtils.replaceShardingSqlToyConfig(sqlToyContext, result, tableShardings, dialect,
				extend.getTableShardingParamsName(sqlToyConfig), extend.getTableShardingParamsValue(sqlToyConfig));
		return result;
	}

	/**
	 * update 2020-08-15 增强对非条件参数?的判断处理
	 * 
	 * @todo sql中替换?为:sagParamName+i形式,便于查询处理(主要针对分页和取随机记录的查询)
	 * @param sql
	 * @param startIndex
	 * @return
	 */
	public static SqlParamsModel convertParamsToNamed(String sql, int startIndex) {
		SqlParamsModel sqlParam = new SqlParamsModel();
		if (sql == null || "".equals(sql.trim())) {
			return sqlParam;
		}
		// 以?号对字符串进行切割，并忽视'' 和"" 之间的
		String[] strs = StringUtil.splitExcludeSymMark(sql, SqlConfigParseUtils.ARG_NAME, QuesFilters);
		int size = strs.length;
		if (size == 1) {
			sqlParam.setSql(sql);
			return sqlParam;
		}
		String preName = SqlToyConstants.DEFAULT_PARAM_NAME;
		StringBuilder result = new StringBuilder();
		String[] paramsName = new String[size - 1];
		int index;
		for (int i = 0; i < size - 1; i++) {
			index = i + startIndex + 1;
			result.append(strs[i]).append(":" + preName + index);
			paramsName[i] = preName + index;
		}
		result.append(strs[size - 1]);
		sqlParam.setSql(result.toString());
		sqlParam.setParamsName(paramsName);
		sqlParam.setParamCnt(size - 1);
		return sqlParam;
	}

	/**
	 * @todo 执行批量保存或修改操作
	 * @param sqlToyContext
	 * @param entities
	 * @param batchSize
	 * @param entityMeta
	 * @param forceUpdateFields
	 * @param generateSqlHandler
	 * @param reflectPropsHandler
	 * @param conn
	 * @param dbType
	 * @param autoCommit
	 * @return
	 * @throws Exception
	 */
	public static Long saveOrUpdateAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			EntityMeta entityMeta, String[] forceUpdateFields, GenerateSqlHandler generateSqlHandler,
			ReflectPropsHandler reflectPropsHandler, Connection conn, final Integer dbType, Boolean autoCommit)
			throws Exception {
		// 重新构造修改或保存的属性赋值反调
		ReflectPropsHandler handler = getSaveOrUpdateReflectHandler(entityMeta.getIdArray(), reflectPropsHandler,
				forceUpdateFields, sqlToyContext.getUnifyFieldsHandler());
		handler = getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		List<Object[]> paramValues = BeanUtil.reflectBeansToInnerAry(entities, entityMeta.getFieldsArray(), null,
				handler);
		int pkIndex = entityMeta.getIdIndex();
		// 是否存在业务ID
		boolean hasBizId = (entityMeta.getBusinessIdGenerator() == null) ? false : true;
		int bizIdColIndex = hasBizId ? entityMeta.getFieldIndex(entityMeta.getBusinessIdField()) : 0;
		// 标识符
		String signature = entityMeta.getBizIdSignature();
		Integer[] relatedColumn = entityMeta.getBizIdRelatedColIndex();
		int relatedColumnSize = (relatedColumn == null) ? 0 : relatedColumn.length;
		// 无主键以及多主键以及assign或通过generator方式产生主键策略
		if (null != entityMeta.getIdStrategy() && null != entityMeta.getIdGenerator()) {
			int bizIdLength = entityMeta.getBizIdLength();
			int idLength = entityMeta.getIdLength();
			Object[] rowData;
			Object[] relatedColValue = null;
			String idJdbcType = entityMeta.getIdType();
			String businessIdType = hasBizId ? entityMeta.getColumnJavaType(entityMeta.getBusinessIdField()) : "";
			for (int i = 0; i < paramValues.size(); i++) {
				rowData = (Object[]) paramValues.get(i);
				// 获取主键策略关联字段的值
				if (relatedColumn != null) {
					relatedColValue = new Object[relatedColumnSize];
					for (int meter = 0; meter < relatedColumnSize; meter++) {
						relatedColValue[meter] = rowData[relatedColumn[meter]];
					}
				}
				if (StringUtil.isBlank(rowData[pkIndex])) {
					rowData[pkIndex] = entityMeta.getIdGenerator().getId(entityMeta.getTableName(), signature,
							entityMeta.getBizIdRelatedColumns(), relatedColValue, null, idJdbcType, idLength,
							entityMeta.getBizIdSequenceSize());
					// 回写主键值
					BeanUtil.setProperty(entities.get(i), entityMeta.getIdArray()[0], rowData[pkIndex]);
				}
				if (hasBizId && StringUtil.isBlank(rowData[bizIdColIndex])) {
					rowData[bizIdColIndex] = entityMeta.getBusinessIdGenerator().getId(entityMeta.getTableName(),
							signature, entityMeta.getBizIdRelatedColumns(), relatedColValue, null, businessIdType,
							bizIdLength, entityMeta.getBizIdSequenceSize());
					// 回写主键值
					BeanUtil.setProperty(entities.get(i), entityMeta.getBusinessIdField(), rowData[bizIdColIndex]);
				}
			}
		}

		String saveOrUpdateSql = generateSqlHandler.generateSql(entityMeta, forceUpdateFields);
		SqlExecuteStat.showSql("执行saveOrUpdate语句", saveOrUpdateSql, null);
		return SqlUtil.batchUpdateByJdbc(sqlToyContext.getTypeHandler(), saveOrUpdateSql, paramValues, batchSize, null,
				entityMeta.getFieldsTypeArray(), autoCommit, conn, dbType);
	}

	/**
	 * @todo 处理加工对象基于db2、oracle数据库的saveOrUpdateSql
	 * @param unifyFieldsHandler
	 * @param dbType
	 * @param entityMeta
	 * @param pkStrategy
	 * @param forceUpdateFields
	 * @param fromTable
	 * @param isNullFunction
	 * @param sequence
	 * @param isAssignPK
	 * @param tableName
	 * @return
	 */
	public static String getSaveOrUpdateSql(UnifyFieldsHandler unifyFieldsHandler, Integer dbType,
                                            EntityMeta entityMeta, PKStrategy pkStrategy, String[] forceUpdateFields, String fromTable,
                                            String isNullFunction, String sequence, boolean isAssignPK, String tableName) {
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		// 在无主键的情况下产生insert sql语句
		if (entityMeta.getIdArray() == null) {
			return DialectExtUtils.generateInsertSql(dbType, entityMeta, pkStrategy, isNullFunction, sequence,
					isAssignPK, realTable);
		}
		// 将新增记录统一赋值属性模拟成默认值模式
		IgnoreKeyCaseMap<String, Object> createUnifyFields = null;
		if (unifyFieldsHandler != null && unifyFieldsHandler.createUnifyFields() != null
				&& !unifyFieldsHandler.createUnifyFields().isEmpty()) {
			createUnifyFields = new IgnoreKeyCaseMap<String, Object>();
			createUnifyFields.putAll(unifyFieldsHandler.createUnifyFields());
		}
		boolean isSupportNUL = StringUtil.isBlank(isNullFunction) ? false : true;
		int columnSize = entityMeta.getFieldsArray().length;
		StringBuilder sql = new StringBuilder(columnSize * 30 + 100);
		String columnName;
		sql.append("merge into ");
		sql.append(realTable);
		sql.append(" ta ");
		sql.append(" using (select ");
		for (int i = 0; i < columnSize; i++) {
			columnName = entityMeta.getColumnName(entityMeta.getFieldsArray()[i]);
			columnName = ReservedWordsUtil.convertWord(columnName, dbType);
			if (i > 0) {
				sql.append(",");
			}
			sql.append("? as ");
			sql.append(columnName);
		}
		if (StringUtil.isNotBlank(fromTable)) {
			sql.append(" from ").append(fromTable);
		}
		sql.append(") tv on (");
		StringBuilder idColumns = new StringBuilder();
		// 组织on部分的主键条件判断
		for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
			columnName = entityMeta.getColumnName(entityMeta.getIdArray()[i]);
			columnName = ReservedWordsUtil.convertWord(columnName, dbType);
			if (i > 0) {
				sql.append(" and ");
				idColumns.append(",");
			}
			sql.append(" ta.").append(columnName).append("=tv.").append(columnName);
			idColumns.append("ta.").append(columnName);
		}
		sql.append(" ) ");
		// 排除id的其他字段信息
		StringBuilder insertRejIdCols = new StringBuilder();
		StringBuilder insertRejIdColValues = new StringBuilder();
		// 是否全部是ID,匹配上则无需进行更新，只需将未匹配上的插入即可
		boolean allIds = (entityMeta.getRejectIdFieldArray() == null);
		if (!allIds) {
			// update 操作
			sql.append(" when matched then update set ");
			int rejectIdColumnSize = entityMeta.getRejectIdFieldArray().length;
			// 需要被强制修改的字段
			HashSet<String> fupc = new HashSet<String>();
			if (forceUpdateFields != null) {
				for (String field : forceUpdateFields) {
					fupc.add(ReservedWordsUtil.convertWord(entityMeta.getColumnName(field), dbType));
				}
			}
			FieldMeta fieldMeta;
			String defaultValue;
			// update 只针对非主键字段进行修改
			for (int i = 0; i < rejectIdColumnSize; i++) {
				fieldMeta = entityMeta.getFieldMeta(entityMeta.getRejectIdFieldArray()[i]);
				columnName = ReservedWordsUtil.convertWord(fieldMeta.getColumnName(), dbType);
				if (i > 0) {
					sql.append(",");
					insertRejIdCols.append(",");
					insertRejIdColValues.append(",");
				}
				sql.append(" ta.").append(columnName).append("=");
				// 强制修改
				if (fupc.contains(columnName)) {
					sql.append("tv.").append(columnName);
				} else {
					sql.append(isNullFunction);
					sql.append("(tv.").append(columnName);
					sql.append(",ta.").append(columnName);
					sql.append(")");
				}
				insertRejIdCols.append(columnName);
				// 将创建人、创建时间等模拟成默认值
				defaultValue = DialectExtUtils.getInsertDefaultValue(createUnifyFields, dbType, fieldMeta);
				// 存在默认值
				if (isSupportNUL && null != defaultValue) {
					insertRejIdColValues.append(isNullFunction);
					insertRejIdColValues.append("(tv.").append(columnName).append(",");
					DialectExtUtils.processDefaultValue(insertRejIdColValues, dbType, fieldMeta.getType(),
							defaultValue);
					insertRejIdColValues.append(")");
				} else {
					insertRejIdColValues.append("tv.").append(columnName);
				}
			}
		}
		// 主键未匹配上则进行插入操作
		sql.append(" when not matched then insert (");
		String idsColumnStr = idColumns.toString();
		// 不考虑只有一个字段且还是主键的情况
		if (allIds) {
			sql.append(idsColumnStr.replaceAll("ta.", ""));
			sql.append(") values (");
			sql.append(idsColumnStr.replaceAll("ta.", "tv."));
		} else {
			sql.append(insertRejIdCols.toString());
			// sequence方式主键
			if (pkStrategy.equals(PKStrategy.sequence)) {
				columnName = entityMeta.getColumnName(entityMeta.getIdArray()[0]);
				columnName = ReservedWordsUtil.convertWord(columnName, dbType);
				sql.append(",");
				sql.append(columnName);
				sql.append(") values (");
				sql.append(insertRejIdColValues).append(",");
				if (isAssignPK && isSupportNUL) {
					sql.append(isNullFunction);
					sql.append("(tv.").append(columnName).append(",");
					sql.append(sequence).append(") ");
				} else {
					sql.append(sequence);
				}
			} else if (pkStrategy.equals(PKStrategy.identity)) {
				columnName = entityMeta.getColumnName(entityMeta.getIdArray()[0]);
				columnName = ReservedWordsUtil.convertWord(columnName, dbType);
				if (isAssignPK) {
					sql.append(",");
					sql.append(columnName);
				}
				sql.append(") values (");
				// identity 模式insert无需写插入该字段语句
				sql.append(insertRejIdColValues);
				if (isAssignPK) {
					sql.append(",").append("tv.").append(columnName);
				}
			} else {
				sql.append(",");
				sql.append(idsColumnStr.replaceAll("ta.", ""));
				sql.append(") values (");
				sql.append(insertRejIdColValues).append(",");
				sql.append(idsColumnStr.replaceAll("ta.", "tv."));
			}
		}
		sql.append(")");
		return sql.toString();
	}

	/**
	 * @todo 产生对象update的语句
	 * @param dbType
	 * @param entityMeta
	 * @param nullFunction
	 * @param forceUpdateFields
	 * @param tableName
	 * @return
	 */
	private static String generateUpdateSql(Integer dbType, EntityMeta entityMeta, String nullFunction,
			String[] forceUpdateFields, String tableName) {
		if (entityMeta.getIdArray() == null) {
			return null;
		}
		StringBuilder sql = new StringBuilder(entityMeta.getFieldsArray().length * 30 + 30);
		sql.append(" update  ");
		sql.append(tableName);
		sql.append(" set ");
		String columnName;
		// 需要被强制修改的字段
		HashSet<String> fupc = new HashSet<String>();
		if (forceUpdateFields != null) {
			for (String field : forceUpdateFields) {
				fupc.add(ReservedWordsUtil.convertWord(entityMeta.getColumnName(field), dbType));
			}
		}
		FieldMeta fieldMeta;
		boolean convertBlob = (dbType == DBType.POSTGRESQL || dbType == DBType.POSTGRESQL15 || dbType == DBType.GAUSSDB
				|| dbType == DBType.KINGBASE);
		boolean isMSsql = (dbType == DBType.SQLSERVER);
		int meter = 0;
		for (int i = 0, n = entityMeta.getRejectIdFieldArray().length; i < n; i++) {
			fieldMeta = entityMeta.getFieldMeta(entityMeta.getRejectIdFieldArray()[i]);
			// 排除sqlserver timestamp类型
			if (!(isMSsql && fieldMeta.getType() == java.sql.Types.TIMESTAMP)) {
				columnName = ReservedWordsUtil.convertWord(fieldMeta.getColumnName(), dbType);
				if (meter > 0) {
					sql.append(",");
				}
				sql.append(columnName);
				sql.append("=");
				if (fupc.contains(columnName)) {
					sql.append("?");
				} else {
					// 2020-6-13 修复postgresql\kingbase\guassdb bytea类型处理错误
					if (convertBlob && "byte[]".equals(fieldMeta.getFieldType())) {
						sql.append(nullFunction);
						sql.append("(cast(? as bytea),").append(columnName).append(" )");
					} else {
						sql.append(nullFunction);
						sql.append("(?,").append(columnName).append(")");
					}
				}
				meter++;
			}
		}
		sql.append(" where ");
		for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
			columnName = entityMeta.getColumnName(entityMeta.getIdArray()[i]);
			columnName = ReservedWordsUtil.convertWord(columnName, dbType);
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append(columnName);
			sql.append("=?");
		}
		return sql.toString();
	}

	/**
	 * @todo 加载获取单笔数据库记录
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param sql
	 * @param entityMeta
	 * @param entity
	 * @param cascadeTypes
	 * @param conn
	 * @param dbType
	 * @return
	 * @throws Exception
	 */
	public static Serializable load(final SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			EntityMeta entityMeta, Serializable entity, List<Class> cascadeTypes, Connection conn, final Integer dbType)
			throws Exception {
		Object[] pkValues = BeanUtil.reflectBeanToAry(entity, entityMeta.getIdArray());
		// 检查主键值是否合法
		for (int i = 0; i < pkValues.length; i++) {
			if (StringUtil.isBlank(pkValues[i])) {
				throw new IllegalArgumentException(entityMeta.getSchemaTable(null, dbType)
						+ " load method must assign value for pk,null pk field is:" + entityMeta.getIdArray()[i]);
			}
		}
		SqlToyResult sqlToyResult = SqlConfigParseUtils.processSql(sql, entityMeta.getIdArray(), pkValues, null);
		// 加密字段解密
		DecryptHandler decryptHandler = null;
		if (entityMeta.getSecureColumns() != null) {
			decryptHandler = new DecryptHandler(sqlToyContext.getFieldsSecureProvider(), entityMeta.getSecureColumns());
		}
		QueryResult queryResult = findBySql(sqlToyContext, sqlToyConfig, sqlToyResult.getSql(),
				sqlToyResult.getParamsValue(), null, decryptHandler, conn, dbType, 0, -1, -1);
		List rows = queryResult.getRows();
		Serializable result = null;
		Class entityClass;
		if (rows != null && rows.size() > 0) {
			entityClass = BeanUtil.getEntityClass(entity.getClass());
			rows = BeanUtil.reflectListToBean(sqlToyContext.getTypeHandler(), rows,
					ResultUtils.humpFieldNames(queryResult.getLabelNames(), entityMeta.getColumnFieldMap()),
					entityClass);
			result = (Serializable) rows.get(0);
			// 处理类中的@Translate注解，进行缓存翻译
			ResultUtils.wrapResultTranslate(sqlToyContext, result, entityClass);
		}
		if (result == null) {
			return null;
		}

		// 存在主表对应子表
		if (null != cascadeTypes && !cascadeTypes.isEmpty() && !entityMeta.getCascadeModels().isEmpty()) {
			List pkRefDetails;
			EntityMeta mappedMeta;
			Object[] mainFieldValues;
			String loadSubTableSql;
			for (TableCascadeModel cascadeModel : entityMeta.getCascadeModels()) {
				// 判定是否要加载
				if (cascadeTypes.contains(cascadeModel.getMappedType())) {
					mainFieldValues = BeanUtil.reflectBeanToAry(result, cascadeModel.getFields());
					loadSubTableSql = ReservedWordsUtil.convertSql(cascadeModel.getLoadSubTableSql(), dbType);
					sqlToyResult = SqlConfigParseUtils.processSql(loadSubTableSql, cascadeModel.getMappedFields(),
							mainFieldValues, null);
					SqlExecuteStat.showSql("级联子表加载查询", sqlToyResult.getSql(), sqlToyResult.getParamsValue());
					mappedMeta = sqlToyContext.getEntityMeta(cascadeModel.getMappedType());
					// 子表加密字段解密
					DecryptHandler subDecryptHandler = null;
					if (mappedMeta.getSecureColumns() != null) {
						subDecryptHandler = new DecryptHandler(sqlToyContext.getFieldsSecureProvider(),
								mappedMeta.getSecureColumns());
					}
					pkRefDetails = SqlUtil.findByJdbcQuery(sqlToyContext.getTypeHandler(), sqlToyResult.getSql(),
							sqlToyResult.getParamsValue(), cascadeModel.getMappedType(), null, subDecryptHandler, conn,
							dbType, false, mappedMeta.getColumnFieldMap(), SqlToyConstants.FETCH_SIZE, -1);
					// 处理子类中@Translate注解，进行缓存翻译
					ResultUtils.wrapResultTranslate(sqlToyContext, pkRefDetails, cascadeModel.getMappedType());
					if (null != pkRefDetails && !pkRefDetails.isEmpty()) {
						// oneToMany
						if (cascadeModel.getCascadeType() == 1) {
							BeanUtil.setProperty(result, cascadeModel.getProperty(), pkRefDetails);
						} else {
							// update 2022-5-18 增加oneToOne 级联数据校验
							if (pkRefDetails.size() > 1) {
								throw new DataAccessException("请检查对象:" + entityMeta.getEntityClass().getName()
										+ "中的@OneToOne级联配置,级联查出的数据size=" + pkRefDetails.size() + ">1,不符合预期!");
							}
							BeanUtil.setProperty(result, cascadeModel.getProperty(), pkRefDetails.get(0));
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * @todo 提供统一的loadAll处理机制
	 * @param sqlToyContext
	 * @param entities
	 * @param cascadeTypes
	 * @param lockMode
	 * @param conn
	 * @param dbType
	 * @param tableName
	 * @param lockSqlHandler
	 * @param fetchSize
	 * @param maxRows
	 * @return
	 * @throws Exception
	 */
	public static List<?> loadAll(final SqlToyContext sqlToyContext, List<?> entities, List<Class> cascadeTypes,
			LockMode lockMode, Connection conn, final Integer dbType, String tableName, LockSqlHandler lockSqlHandler,
			final int fetchSize, final int maxRows) throws Exception {
		if (entities == null || entities.isEmpty()) {
			return entities;
		}
		Class entityClass = BeanUtil.getEntityClass(entities.get(0).getClass());
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entityClass);
		// 没有主键不能进行load相关的查询
		if (null == entityMeta.getIdArray() || entityMeta.getIdArray().length < 1) {
			throw new IllegalArgumentException(
					entityClass.getName() + " Entity Object hasn't primary key,cann't use loadAll method!");
		}
		DecryptHandler decryptHandler = null;
		if (entityMeta.getSecureColumns() != null) {
			decryptHandler = new DecryptHandler(sqlToyContext.getFieldsSecureProvider(), entityMeta.getSecureColumns());
		}
		int idSize = entityMeta.getIdArray().length;
		SqlToyResult sqlToyResult = null;
		// 单主键
		if (idSize == 1) {
			// 切取id数组
			Object[] idValues = BeanUtil.sliceToArray(entities, entityMeta.getIdArray()[0]);
			if (idValues == null || idValues.length == 0) {
				throw new IllegalArgumentException(
						tableName + " loadAll method must assign value for pk field:" + entityMeta.getIdArray()[0]);
			}
			// 组织loadAll sql语句
			String sql = wrapLoadAll(entityMeta, idValues.length, tableName, lockSqlHandler, lockMode, dbType);
			sqlToyResult = SqlConfigParseUtils.processSql(sql, null, new Object[] { idValues }, null);
		} // 复合主键
		else {
			List<Object[]> idValues = BeanUtil.reflectBeansToInnerAry(entities, entityMeta.getIdArray(), null, null);
			Object[] rowData;
			Object cellValue;
			// 将条件构造成一个数组
			Object[] realValues = new Object[idValues.size() * idSize];
			int index = 0;
			for (int i = 0, n = idValues.size(); i < n; i++) {
				rowData = idValues.get(i);
				for (int j = 0; j < idSize; j++) {
					cellValue = rowData[j];
					// 验证主键值是否合法
					if (StringUtil.isBlank(cellValue)) {
						throw new IllegalArgumentException(tableName + " loadAll method must assign value for pk,row:"
								+ i + " pk field:" + entityMeta.getIdArray()[j]);
					}
					realValues[index] = cellValue;
					index++;
				}
			}
			// 组织loadAll sql语句
			String sql = wrapLoadAll(entityMeta, idValues.size(), tableName, lockSqlHandler, lockMode, dbType);
			sqlToyResult = SqlConfigParseUtils.processSql(sql, null, realValues, null);
		}

		SqlExecuteStat.showSql("执行依据主键批量查询", sqlToyResult.getSql(), sqlToyResult.getParamsValue());
		List<?> entitySet = SqlUtil.findByJdbcQuery(sqlToyContext.getTypeHandler(), sqlToyResult.getSql(),
				sqlToyResult.getParamsValue(), entityClass, null, decryptHandler, conn, dbType, false,
				entityMeta.getColumnFieldMap(), fetchSize, maxRows);
		// 处理类中的@Translate注解，进行缓存翻译
		ResultUtils.wrapResultTranslate(sqlToyContext, entitySet, entityClass);
		if (entitySet == null || entitySet.isEmpty()) {
			return entitySet;
		}
		// 存在主表对应子表
		if (null != cascadeTypes && !cascadeTypes.isEmpty() && !entityMeta.getCascadeModels().isEmpty()) {
			StringBuilder subTableSql = new StringBuilder();
			List items;
			SqlToyResult subToyResult;
			EntityMeta mappedMeta;
			int fieldSize;
			List<Object[]> idValues = null;
			String colName;
			Object[] rowData;
			Object cellValue;
			for (TableCascadeModel cascadeModel : entityMeta.getCascadeModels()) {
				if (cascadeTypes.contains(cascadeModel.getMappedType())) {
					mappedMeta = sqlToyContext.getEntityMeta(cascadeModel.getMappedType());
					// 清空buffer
					subTableSql.delete(0, subTableSql.length());
					// 构造查询语句,update 2019-12-09 使用完整字段
					subTableSql.append(ReservedWordsUtil.convertSimpleSql(mappedMeta.getLoadAllSql(), dbType))
							.append(" where ");
					String orderCols = "";
					boolean hasOrder = StringUtil.isNotBlank(cascadeModel.getOrderBy());
					boolean hasExtCondtion = StringUtil.isNotBlank(cascadeModel.getLoadExtCondition()) ? true : false;
					fieldSize = cascadeModel.getMappedFields().length;
					// 单主键
					if (fieldSize == 1) {
						colName = cascadeModel.getMappedColumns()[0];
						colName = ReservedWordsUtil.convertWord(colName, dbType);
						subTableSql.append(colName);
						subTableSql.append(" in (?) ");
						if (hasOrder) {
							orderCols = orderCols.concat(colName).concat(",");
						}
					} // 复合主键
					else {
						// 构造(field1=? and field2=?)
						String condition = " (";
						for (int i = 0; i < fieldSize; i++) {
							colName = cascadeModel.getMappedColumns()[i];
							colName = ReservedWordsUtil.convertWord(colName, dbType);
							if (i > 0) {
								condition = condition.concat(" and ");
							}
							condition = condition.concat(colName).concat("=?");
							if (hasOrder) {
								orderCols = orderCols.concat(colName).concat(",");
							}
						}
						condition = condition.concat(") ");
						// 构造成 (field1=? and field2=?) or (field1=? and field2=?)
						if (hasExtCondtion) {
							subTableSql.append(" (");
						}
						idValues = BeanUtil.reflectBeansToInnerAry(entitySet, cascadeModel.getFields(), null, null);
						for (int i = 0; i < idValues.size(); i++) {
							if (i > 0) {
								subTableSql.append(" or ");
							}
							subTableSql.append(condition);
						}
						if (hasExtCondtion) {
							subTableSql.append(") ");
						}
					}
					// 自定义扩展条件
					if (hasExtCondtion) {
						subTableSql.append(" and ").append(cascadeModel.getLoadExtCondition());
					}
					if (hasOrder) {
						subTableSql.append(" order by ").append(orderCols).append(cascadeModel.getOrderBy());
					}
					// 单主键
					if (fieldSize == 1) {
						Object[] pkValues = BeanUtil.sliceToArray(entitySet, cascadeModel.getFields()[0]);
						subToyResult = SqlConfigParseUtils.processSql(subTableSql.toString(), null,
								new Object[] { pkValues }, null);
					} else {
						// 复合主键,将条件值构造成一个数组
						Object[] realValues = new Object[idValues.size() * fieldSize];
						int index = 0;
						for (int i = 0, n = idValues.size(); i < n; i++) {
							rowData = idValues.get(i);
							for (int j = 0; j < fieldSize; j++) {
								cellValue = rowData[j];
								realValues[index] = cellValue;
								index++;
							}
						}
						subToyResult = SqlConfigParseUtils.processSql(subTableSql.toString(), null, realValues, null);
					}
					SqlExecuteStat.showSql("执行级联加载子表", subToyResult.getSql(), subToyResult.getParamsValue());
					// 加密字段解密
					DecryptHandler subDecryptHandler = null;
					if (mappedMeta.getSecureColumns() != null) {
						subDecryptHandler = new DecryptHandler(sqlToyContext.getFieldsSecureProvider(),
								mappedMeta.getSecureColumns());
					}
					items = SqlUtil.findByJdbcQuery(sqlToyContext.getTypeHandler(), subToyResult.getSql(),
							subToyResult.getParamsValue(), cascadeModel.getMappedType(), null, subDecryptHandler, conn,
							dbType, false, mappedMeta.getColumnFieldMap(), SqlToyConstants.FETCH_SIZE, maxRows);
					// 处理子类中的@Translate注解，进行缓存翻译
					ResultUtils.wrapResultTranslate(sqlToyContext, items, cascadeModel.getMappedType());
					SqlExecuteStat.debug("子表加载结果", "子记录数:{} 条", items.size());
					// 将item的值分配映射到main主表对象上
					BeanUtil.loadAllMapping(entitySet, items, cascadeModel);
				}
			}
		}
		return entitySet;
	}

	/**
	 * @TODO 组织loadAll sql语句
	 * @param entityMeta
	 * @param dataSize
	 * @param tableName
	 * @param lockSqlHandler
	 * @param lockMode
	 * @param dbType
	 * @return
	 */
	private static String wrapLoadAll(EntityMeta entityMeta, int dataSize, String tableName,
			LockSqlHandler lockSqlHandler, LockMode lockMode, Integer dbType) {
		int idSize = entityMeta.getIdArray().length;
		StringBuilder loadSql = new StringBuilder();
		loadSql.append("select ").append(ReservedWordsUtil.convertSimpleSql(entityMeta.getAllColumnNames(), dbType));
		loadSql.append(" from ");
		loadSql.append(entityMeta.getSchemaTable(tableName, dbType));
		// sqlserver 锁语句不同于其他
		if (dbType == DBType.SQLSERVER && lockMode != null) {
			switch (lockMode) {
			case UPGRADE:
				loadSql.append(" with (rowlock xlock) ");
				break;
			case UPGRADE_NOWAIT:
			case UPGRADE_SKIPLOCK:
				loadSql.append(" with (rowlock readpast) ");
				break;
			}
		}
		loadSql.append(" where ");
		String field;
		String colName;
		// 单主键
		if (idSize == 1) {
			field = entityMeta.getIdArray()[0];
			colName = ReservedWordsUtil.convertWord(entityMeta.getColumnName(field), dbType);
			loadSql.append(colName);
			loadSql.append(" in (?) ");
		} else {
			// 复合主键构造 (field1=? and field2=?)
			String condition = " (";
			for (int i = 0; i < idSize; i++) {
				field = entityMeta.getIdArray()[i];
				colName = ReservedWordsUtil.convertWord(entityMeta.getColumnName(field), dbType);
				if (i > 0) {
					condition = condition.concat(" and ");
				}
				condition = condition.concat(colName).concat("=?");
			}
			condition = condition.concat(")");
			// 构造 (field1=? and field2=?) or (field1=? and field2=?)
			for (int i = 0; i < dataSize; i++) {
				if (i > 0) {
					loadSql.append(" or ");
				}
				loadSql.append(condition);
			}
		}

		if (lockSqlHandler != null) {
			loadSql.append(lockSqlHandler.getLockSql(loadSql.toString(), dbType, lockMode));
		}
		return loadSql.toString();
	}

	/**
	 * @todo 保存对象
	 * @param sqlToyContext
	 * @param entityMeta
	 * @param pkStrategy
	 * @param isAssignPK
	 * @param insertSql
	 * @param entity
	 * @param generateSqlHandler
	 * @param generateSavePKStrategy
	 * @param conn
	 * @param dbType
	 * @return
	 * @throws Exception
	 */
	public static Object save(final SqlToyContext sqlToyContext, final EntityMeta entityMeta,
			final PKStrategy pkStrategy, final boolean isAssignPK, final String insertSql, Serializable entity,
			final GenerateSqlHandler generateSqlHandler, final GenerateSavePKStrategy generateSavePKStrategy,
			final Connection conn, final Integer dbType) throws Exception {
		final boolean isIdentity = (pkStrategy != null && pkStrategy.equals(PKStrategy.identity));
		final boolean isSequence = (pkStrategy != null && pkStrategy.equals(PKStrategy.sequence));
		String[] reflectColumns;
		if ((isIdentity && !isAssignPK) || (isSequence && !isAssignPK)) {
			reflectColumns = entityMeta.getRejectIdFieldArray();
		} else {
			reflectColumns = entityMeta.getFieldsArray();
		}
		// 构造全新的新增记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = getAddReflectHandler(null, sqlToyContext.getUnifyFieldsHandler());
		handler = getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		// update 2022-7-16 增加默认值的代入
		Object[] fullParamValues = BeanUtil.reflectBeanToAry(entity, reflectColumns,
				SqlUtilsExt.getDefaultValues(entityMeta), handler);
		boolean needUpdatePk = false;
		int pkIndex = entityMeta.getIdIndex();
		// 是否存在业务ID
		boolean hasBizId = (entityMeta.getBusinessIdGenerator() == null) ? false : true;
		int bizIdColIndex = hasBizId ? entityMeta.getFieldIndex(entityMeta.getBusinessIdField()) : 0;
		// 标识符
		String signature = entityMeta.getBizIdSignature();
		Integer[] relatedColumn = entityMeta.getBizIdRelatedColIndex();
		String[] relatedColumnNames = entityMeta.getBizIdRelatedColumns();
		int relatedColumnSize = (relatedColumn == null) ? 0 : relatedColumn.length;
		// 主键采用assign方式赋予，则调用generator产生id并赋予其值
		if (entityMeta.getIdStrategy() != null && null != entityMeta.getIdGenerator()) {
			int bizIdLength = entityMeta.getBizIdLength();
			int idLength = entityMeta.getIdLength();
			Object[] relatedColValue = null;
			String businessIdType = hasBizId ? entityMeta.getColumnJavaType(entityMeta.getBusinessIdField()) : "";
			if (relatedColumn != null) {
				relatedColValue = new Object[relatedColumnSize];
				for (int meter = 0; meter < relatedColumnSize; meter++) {
					relatedColValue[meter] = fullParamValues[relatedColumn[meter]];
					if (StringUtil.isBlank(relatedColValue[meter])) {
						throw new IllegalArgumentException("对象:" + entityMeta.getEntityClass().getName()
								+ " 生成业务主键依赖的关联字段:" + relatedColumnNames[meter] + " 值为null!");
					}
				}
			}
			if (StringUtil.isBlank(fullParamValues[pkIndex])) {
				// id通过generator机制产生，设置generator产生的值
				fullParamValues[pkIndex] = entityMeta.getIdGenerator().getId(entityMeta.getTableName(), signature,
						entityMeta.getBizIdRelatedColumns(), relatedColValue, null, entityMeta.getIdType(), idLength,
						entityMeta.getBizIdSequenceSize());
				needUpdatePk = true;
			}
			if (hasBizId && StringUtil.isBlank(fullParamValues[bizIdColIndex])) {
				fullParamValues[bizIdColIndex] = entityMeta.getBusinessIdGenerator().getId(entityMeta.getTableName(),
						signature, entityMeta.getBizIdRelatedColumns(), relatedColValue, null, businessIdType,
						bizIdLength, entityMeta.getBizIdSequenceSize());
				// 回写业务主键值
				BeanUtil.setProperty(entity, entityMeta.getBusinessIdField(), fullParamValues[bizIdColIndex]);
			}
		}
		SqlExecuteStat.showSql("执行单记录插入", insertSql, null);
		final Object[] paramValues = fullParamValues;
		final Integer[] paramsType = entityMeta.getFieldsTypeArray();
		PreparedStatement pst = null;
		Object result = SqlUtil.preparedStatementProcess(null, pst, null, new PreparedStatementResultHandler() {
			@Override
			public void execute(Object obj, PreparedStatement pst, ResultSet rs) throws SQLException, IOException {
				if (isIdentity || isSequence) {
					pst = conn.prepareStatement(insertSql,
							new String[] { entityMeta.getColumnName(entityMeta.getIdArray()[0]) });
				} else {
					pst = conn.prepareStatement(insertSql);
				}
				SqlUtil.setParamsValue(sqlToyContext.getTypeHandler(), conn, dbType, pst, paramValues, paramsType, 0);
				pst.execute();
				if (isIdentity || isSequence) {
					ResultSet keyResult = pst.getGeneratedKeys();
					if (keyResult != null) {
						while (keyResult.next()) {
							this.setResult(keyResult.getObject(1));
						}
					}
				}
			}
		});
		// 无主键直接返回null
		if (entityMeta.getIdArray() == null) {
			return null;
		}
		if (result == null) {
			result = fullParamValues[pkIndex];
		}
		// 回置到entity 主键值
		if (needUpdatePk || isIdentity || isSequence) {
			BeanUtil.setProperty(entity, entityMeta.getIdArray()[0], result);
		}
		// 判定是否有级联子表数据保存
		if (!entityMeta.getCascadeModels().isEmpty()) {
			List subTableData = null;
			EntityMeta subTableEntityMeta;
			String insertSubTableSql;
			SavePKStrategy savePkStrategy;
			for (TableCascadeModel cascadeModel : entityMeta.getCascadeModels()) {
				final String[] mappedFields = cascadeModel.getMappedFields();
				final Object[] mainValues = BeanUtil.reflectBeanToAry(entity, cascadeModel.getFields());
				subTableEntityMeta = sqlToyContext.getEntityMeta(cascadeModel.getMappedType());
				// oneToMany
				if (cascadeModel.getCascadeType() == 1) {
					subTableData = (List) BeanUtil.getProperty(entity, cascadeModel.getProperty());
				} else {
					subTableData = new ArrayList();
					Object item = BeanUtil.getProperty(entity, cascadeModel.getProperty());
					if (item != null) {
						subTableData.add(item);
					}
				}
				if (subTableData != null && !subTableData.isEmpty()) {
					logger.info("执行save操作的级联子表{}批量保存!", subTableEntityMeta.getTableName());
					SqlExecuteStat.debug("执行子表级联保存", null);
					insertSubTableSql = generateSqlHandler.generateSql(subTableEntityMeta, null);
					savePkStrategy = generateSavePKStrategy.generate(subTableEntityMeta);
					saveAll(sqlToyContext, subTableEntityMeta, savePkStrategy.getPkStrategy(),
							savePkStrategy.isAssginValue(), insertSubTableSql, subTableData,
							sqlToyContext.getBatchSize(), new ReflectPropsHandler() {
								@Override
								public void process() {
									for (int i = 0; i < mappedFields.length; i++) {
										this.setValue(mappedFields[i], mainValues[i]);
									}
								}
							}, conn, dbType, null);
				} else {
					logger.info("未执行save操作的级联子表{}批量保存,子表数据为空!", subTableEntityMeta.getTableName());
				}
			}
		}
		return result;
	}

	/**
	 * @todo 保存批量对象数据
	 * @param sqlToyContext
	 * @param entityMeta
	 * @param pkStrategy
	 * @param isAssignPK
	 * @param insertSql
	 * @param entities
	 * @param batchSize
	 * @param reflectPropsHandler
	 * @param conn
	 * @param dbType
	 * @param autoCommit
	 * @return
	 * @throws Exception
	 */
	public static Long saveAll(SqlToyContext sqlToyContext, EntityMeta entityMeta, PKStrategy pkStrategy,
			boolean isAssignPK, String insertSql, List<?> entities, final int batchSize,
			ReflectPropsHandler reflectPropsHandler, Connection conn, final Integer dbType, final Boolean autoCommit)
			throws Exception {
		boolean isIdentity = pkStrategy != null && pkStrategy.equals(PKStrategy.identity);
		boolean isSequence = pkStrategy != null && pkStrategy.equals(PKStrategy.sequence);
		String[] reflectColumns;
		if ((isIdentity && !isAssignPK) || (isSequence && !isAssignPK)) {
			reflectColumns = entityMeta.getRejectIdFieldArray();
		} else {
			reflectColumns = entityMeta.getFieldsArray();
		}
		// 构造全新的新增记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = getAddReflectHandler(reflectPropsHandler, sqlToyContext.getUnifyFieldsHandler());
		handler = getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		// update 2022-7-16 增加了默认值代入
		List paramValues = BeanUtil.reflectBeansToInnerAry(entities, reflectColumns,
				SqlUtilsExt.getDefaultValues(entityMeta), handler);
		int pkIndex = entityMeta.getIdIndex();
		// 是否存在业务ID
		boolean hasBizId = (entityMeta.getBusinessIdGenerator() == null) ? false : true;
		int bizIdColIndex = hasBizId ? entityMeta.getFieldIndex(entityMeta.getBusinessIdField()) : 0;
		// 标识符
		String signature = entityMeta.getBizIdSignature();
		Integer[] relatedColumn = entityMeta.getBizIdRelatedColIndex();
		String[] relatedColumnNames = entityMeta.getBizIdRelatedColumns();
		int relatedColumnSize = (relatedColumn == null) ? 0 : relatedColumn.length;
		// 无主键值以及多主键以及assign或通过generator方式产生主键策略
		if (pkStrategy != null && null != entityMeta.getIdGenerator()) {
			int bizIdLength = entityMeta.getBizIdLength();
			int idLength = entityMeta.getIdLength();
			Object[] rowData;
			boolean isAssigned = true;
			String idJdbcType = entityMeta.getIdType();
			Object[] relatedColValue = null;
			String businessIdType = hasBizId ? entityMeta.getColumnJavaType(entityMeta.getBusinessIdField()) : "";
			List<Object[]> idSet = new ArrayList<Object[]>();
			for (int i = 0, s = paramValues.size(); i < s; i++) {
				rowData = (Object[]) paramValues.get(i);
				// 判断主键策略关联的字段是否有值,合法性验证
				if (relatedColumn != null) {
					relatedColValue = new Object[relatedColumnSize];
					for (int meter = 0; meter < relatedColumnSize; meter++) {
						relatedColValue[meter] = rowData[relatedColumn[meter]];
						if (StringUtil.isBlank(relatedColValue[meter])) {
							throw new IllegalArgumentException("对象:" + entityMeta.getEntityClass().getName()
									+ " 生成业务主键依赖的关联字段:" + relatedColumnNames[meter] + " 值为null!");
						}
					}
				}
				// 主键值为null,调用主键生成策略并赋值
				if (StringUtil.isBlank(rowData[pkIndex])) {
					isAssigned = false;
					rowData[pkIndex] = entityMeta.getIdGenerator().getId(entityMeta.getTableName(), signature,
							entityMeta.getBizIdRelatedColumns(), relatedColValue, null, idJdbcType, idLength,
							entityMeta.getBizIdSequenceSize());
				}
				if (hasBizId && StringUtil.isBlank(rowData[bizIdColIndex])) {
					rowData[bizIdColIndex] = entityMeta.getBusinessIdGenerator().getId(entityMeta.getTableName(),
							signature, entityMeta.getBizIdRelatedColumns(), relatedColValue, null, businessIdType,
							bizIdLength, entityMeta.getBizIdSequenceSize());
					// 回写业务主键值
					BeanUtil.setProperty(entities.get(i), entityMeta.getBusinessIdField(), rowData[bizIdColIndex]);
				}
				idSet.add(new Object[] { rowData[pkIndex] });
			}
			// 批量反向设置最终得到的主键值
			if (!isAssigned) {
				BeanUtil.mappingSetProperties(entities, entityMeta.getIdArray(), idSet, new int[] { 0 }, true);
			}
		}
		SqlExecuteStat.showSql("批量保存[" + paramValues.size() + "]条记录", insertSql, null);
		return SqlUtilsExt.batchUpdateForPOJO(sqlToyContext.getTypeHandler(), insertSql, paramValues,
				entityMeta.getFieldsTypeArray(), entityMeta.getFieldsDefaultValue(), batchSize, autoCommit, conn,
				dbType);
	}

	/**
	 * @todo 执行批量保存或修改操作
	 * @param sqlToyContext
	 * @param entities
	 * @param batchSize
	 * @param entityMeta
	 * @param generateSqlHandler
	 * @param reflectPropsHandler
	 * @param conn
	 * @param dbType
	 * @param autoCommit
	 * @return
	 * @throws Exception
	 */
	public static Long saveAllIgnoreExist(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			EntityMeta entityMeta, GenerateSqlHandler generateSqlHandler, ReflectPropsHandler reflectPropsHandler,
			Connection conn, final Integer dbType, Boolean autoCommit) throws Exception {
		// 构造全新的新增记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = getAddReflectHandler(reflectPropsHandler, sqlToyContext.getUnifyFieldsHandler());
		handler = getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		// update 2022-7-16 增加默认值代入,insert sql上去除了nvl(?,default) 适应一些框架
		List<Object[]> paramValues = BeanUtil.reflectBeansToInnerAry(entities, entityMeta.getFieldsArray(),
				SqlUtilsExt.getDefaultValues(entityMeta), handler);
		int pkIndex = entityMeta.getIdIndex();
		// 是否存在业务ID
		boolean hasBizId = (entityMeta.getBusinessIdGenerator() == null) ? false : true;
		int bizIdColIndex = hasBizId ? entityMeta.getFieldIndex(entityMeta.getBusinessIdField()) : 0;
		// 标识符
		String signature = entityMeta.getBizIdSignature();
		Integer[] relatedColumn = entityMeta.getBizIdRelatedColIndex();
		String[] relatedColumnNames = entityMeta.getBizIdRelatedColumns();
		int relatedColumnSize = (relatedColumn == null) ? 0 : relatedColumn.length;
		// 无主键以及多主键以及assign或通过generator方式产生主键策略
		if (null != entityMeta.getIdStrategy() && null != entityMeta.getIdGenerator()) {
			int bizIdLength = entityMeta.getBizIdLength();
			int idLength = entityMeta.getIdLength();
			Object[] rowData;
			Object[] relatedColValue = null;
			String idJdbcType = entityMeta.getIdType();
			String businessIdType = hasBizId ? entityMeta.getColumnJavaType(entityMeta.getBusinessIdField()) : "";
			for (int i = 0; i < paramValues.size(); i++) {
				rowData = (Object[]) paramValues.get(i);
				// 关联字段赋值
				if (relatedColumn != null) {
					relatedColValue = new Object[relatedColumnSize];
					for (int meter = 0; meter < relatedColumnSize; meter++) {
						relatedColValue[meter] = rowData[relatedColumn[meter]];
						if (relatedColValue[meter] == null) {
							throw new IllegalArgumentException("对象:" + entityMeta.getEntityClass().getName()
									+ " 生成业务主键依赖的关联字段:" + relatedColumnNames[meter] + " 值为null!");
						}
					}
				}
				if (StringUtil.isBlank(rowData[pkIndex])) {
					rowData[pkIndex] = entityMeta.getIdGenerator().getId(entityMeta.getTableName(), signature,
							entityMeta.getBizIdRelatedColumns(), relatedColValue, null, idJdbcType, idLength,
							entityMeta.getBizIdSequenceSize());
					// 回写主键值
					BeanUtil.setProperty(entities.get(i), entityMeta.getIdArray()[0], rowData[pkIndex]);
				}
				if (hasBizId && StringUtil.isBlank(rowData[bizIdColIndex])) {
					rowData[bizIdColIndex] = entityMeta.getBusinessIdGenerator().getId(entityMeta.getTableName(),
							signature, entityMeta.getBizIdRelatedColumns(), relatedColValue, null, businessIdType,
							bizIdLength, entityMeta.getBizIdSequenceSize());
					// 回写业务主键值
					BeanUtil.setProperty(entities.get(i), entityMeta.getBusinessIdField(), rowData[bizIdColIndex]);
				}
			}
		}
		String saveAllNotExistSql = generateSqlHandler.generateSql(entityMeta, null);
		SqlExecuteStat.showSql("批量插入且忽视已存在记录", saveAllNotExistSql, null);
		return SqlUtilsExt.batchUpdateForPOJO(sqlToyContext.getTypeHandler(), saveAllNotExistSql, paramValues,
				entityMeta.getFieldsTypeArray(), entityMeta.getFieldsDefaultValue(), batchSize, autoCommit, conn,
				dbType);
	}

	/**
	 * @todo 单笔记录修改
	 * @param sqlToyContext
	 * @param entity
	 * @param entityMeta
	 * @param nullFunction
	 * @param forceUpdateFields
	 * @param conn
	 * @param dbType
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static Long update(SqlToyContext sqlToyContext, Serializable entity, EntityMeta entityMeta,
			String nullFunction, String[] forceUpdateFields, Connection conn, final Integer dbType, String tableName)
			throws Exception {
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		// 无主键
		if (entityMeta.getIdArray() == null) {
			throw new IllegalArgumentException("表:" + realTable + " 无主键,不符合update/updateAll规则,请检查表设计是否合理!");
		}
		// 全部是主键则无需update
		if (entityMeta.getRejectIdFieldArray() == null) {
			logger.warn("表:" + realTable + " 字段全部是主键不存在更新字段,无需执行更新操作!");
			return 0L;
		}
		// 构造全新的修改记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = getUpdateReflectHandler(null, forceUpdateFields,
				sqlToyContext.getUnifyFieldsHandler());
		handler = getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		Object[] fieldsValues = BeanUtil.reflectBeanToAry(entity, entityMeta.getFieldsArray(), null, handler);
		// 判断主键是否为空
		int pkIndex = entityMeta.getIdIndex();
		for (int i = pkIndex; i < pkIndex + entityMeta.getIdArray().length; i++) {
			if (StringUtil.isBlank(fieldsValues[i])) {
				throw new IllegalArgumentException("通过对象对表:" + realTable + " 进行update操作,主键字段必须要赋值!");
			}
		}
		// 构建update语句
		String updateSql = generateUpdateSql(dbType, entityMeta, nullFunction, forceUpdateFields, realTable);
		if (updateSql == null) {
			throw new IllegalArgumentException("update sql is null,引起问题的原因是没有设置需要修改的字段!");
		}
		return SqlUtil.executeSql(sqlToyContext.getTypeHandler(), updateSql, fieldsValues,
				entityMeta.getFieldsTypeArray(), conn, dbType, null, false);
	}

	/**
	 * @todo 单个对象修改，包含接连修改
	 * @param sqlToyContext
	 * @param entity
	 * @param nullFunction
	 * @param forceUpdateFields
	 * @param cascade
	 * @param generateSqlHandler
	 * @param forceCascadeClasses
	 * @param subTableForceUpdateProps
	 * @param conn
	 * @param tableName
	 * @throws Exception
	 */
	public static Long update(SqlToyContext sqlToyContext, Serializable entity, String nullFunction,
			String[] forceUpdateFields, final boolean cascade, final GenerateSqlHandler generateSqlHandler,
			final Class[] forceCascadeClasses, final HashMap<Class, String[]> subTableForceUpdateProps, Connection conn,
			final Integer dbType, String tableName) throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		// 无主键
		if (entityMeta.getIdArray() == null) {
			throw new IllegalArgumentException("表:" + realTable + " 无主键,不符合update/updateAll规则,请检查表设计是否合理!");
		}
		// 全部是主键则无需update
		if (entityMeta.getRejectIdFieldArray() == null) {
			logger.warn("表:" + realTable + " 字段全部是主键不存在更新字段,无需执行更新操作!");
			return 0L;
		}
		Long updateCnt = update(sqlToyContext, entity, entityMeta, nullFunction, forceUpdateFields, conn, dbType,
				tableName);
		// 不存在级联操作
		if (!cascade || entityMeta.getCascadeModels().isEmpty()) {
			return updateCnt;
		}
		// 级联保存
		HashMap<Type, String> typeMap = new HashMap<Type, String>();
		// 即使子对象数据是null,也强制进行级联修改(null表示删除子表数据)
		if (forceCascadeClasses != null) {
			for (Type type : forceCascadeClasses) {
				typeMap.put(type, "");
			}
		}
		// 级联子表数据
		List subTableData = null;
		String[] forceUpdateProps = null;
		EntityMeta subTableEntityMeta;
		// 对子表进行级联处理
		for (TableCascadeModel cascadeModel : entityMeta.getCascadeModels()) {
			final Object[] mainFieldValues = BeanUtil.reflectBeanToAry(entity, cascadeModel.getFields());
			subTableEntityMeta = sqlToyContext.getEntityMeta(cascadeModel.getMappedType());
			forceUpdateProps = (subTableForceUpdateProps == null) ? null
					: subTableForceUpdateProps.get(cascadeModel.getMappedType());
			// oneToMany
			if (cascadeModel.getCascadeType() == 1) {
				subTableData = (List) BeanUtil.getProperty(entity, cascadeModel.getProperty());
			} else {
				subTableData = new ArrayList();
				Object item = BeanUtil.getProperty(entity, cascadeModel.getProperty());
				if (item != null) {
					subTableData.add(item);
				}
			}
			final String[] mappedFields = cascadeModel.getMappedFields();

			// 针对子表存量数据,调用级联修改的语句，分delete 和update两种操作 1、删除存量数据;2、设置存量数据状态为停用
			if (cascadeModel.getCascadeUpdateSql() != null && ((subTableData != null && !subTableData.isEmpty())
					|| typeMap.containsKey(cascadeModel.getMappedType()))) {
				SqlExecuteStat.debug("执行子表级联更新前的存量数据更新", null);
				// 根据quickvo配置文件针对cascade中update-cascade配置组织具体操作sql
				SqlToyResult sqlToyResult = SqlConfigParseUtils.processSql(cascadeModel.getCascadeUpdateSql(),
						mappedFields, mainFieldValues, null);
				SqlUtil.executeSql(sqlToyContext.getTypeHandler(), sqlToyResult.getSql(), sqlToyResult.getParamsValue(),
						null, conn, dbType, null, true);
			}
			// 子表数据不为空,采取saveOrUpdateAll操作
			if (subTableData != null && !subTableData.isEmpty()) {
				logger.info("执行update主表:{} 对应级联子表: {} 更新操作!", realTable, subTableEntityMeta.getTableName());
				SqlExecuteStat.debug("执行子表级联更新操作", null);
				// 将外键值通过反调赋到相关属性上
				ReflectPropsHandler reflectPropsHandler = new ReflectPropsHandler() {
					@Override
					public void process() {
						for (int i = 0; i < mappedFields.length; i++) {
							this.setValue(mappedFields[i], mainFieldValues[i]);
						}
					}
				};
				// update 2020-07-30,针对mysql和postgresql、sqlite常用数据库做针对性处理
				// 这里需要进行修改,mysql\postgresql\sqlite 等存在缺陷(字段值不为null时会报错)
				if (dbType == DBType.MYSQL || dbType == DBType.MYSQL57 || dbType == DBType.TIDB) {
					mysqlSaveOrUpdateAll(sqlToyContext, subTableEntityMeta, subTableData, reflectPropsHandler,
							forceUpdateProps, conn, dbType);
				} else if (dbType == DBType.POSTGRESQL || dbType == DBType.POSTGRESQL15 || dbType == DBType.GAUSSDB) {
					postgreSaveOrUpdateAll(sqlToyContext, subTableEntityMeta, subTableData, reflectPropsHandler,
							forceUpdateProps, conn, dbType);
				} else if (dbType == DBType.OCEANBASE) {
					oceanBaseSaveOrUpdateAll(sqlToyContext, subTableEntityMeta, subTableData, reflectPropsHandler,
							forceUpdateProps, conn, dbType);
				} else if (dbType == DBType.SQLITE) {
					sqliteSaveOrUpdateAll(sqlToyContext, subTableEntityMeta, subTableData, reflectPropsHandler,
							forceUpdateProps, conn, dbType);
				} // 达梦数据库
				else if (dbType == DBType.DM) {
					dmSaveOrUpdateAll(sqlToyContext, subTableEntityMeta, subTableData, reflectPropsHandler,
							forceUpdateProps, conn, dbType);
				} // kingbase
				else if (dbType == DBType.KINGBASE) {
					kingbaseSaveOrUpdateAll(sqlToyContext, subTableEntityMeta, subTableData, reflectPropsHandler,
							forceUpdateProps, conn, dbType);
				}
				// db2/oracle/mssql 通过merge 方式
				else {
					saveOrUpdateAll(sqlToyContext, subTableData, sqlToyContext.getBatchSize(), subTableEntityMeta,
							forceUpdateProps, generateSqlHandler,
							// 设置关联外键字段的属性值(来自主表的主键)
							reflectPropsHandler, conn, dbType, null);
				}
			} else {
				logger.info("未执行update主表:{} 对应级联子表: {} 更新操作,子表数据为空!", realTable, subTableEntityMeta.getTableName());
			}
		}
		return updateCnt;
	}

	// update 2020-07-30
	// update 级联操作时，子表会涉及saveOrUpdateAll动作,而mysql和postgresql 对应的
	// ON DUPLICATE KEY UPDATE 当字段为非空时报错，因此需特殊处理
	private static void mysqlSaveOrUpdateAll(SqlToyContext sqlToyContext, final EntityMeta entityMeta, List<?> entities,
			ReflectPropsHandler reflectPropsHandler, final String[] forceUpdateFields, Connection conn,
			final Integer dbType) throws Exception {
		int batchSize = sqlToyContext.getBatchSize();
		final String tableName = entityMeta.getSchemaTable(null, dbType);
		Long updateCnt = updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropsHandler, "ifnull",
				conn, dbType, null, tableName, true);
		// 如果修改的记录数量跟总记录数量一致,表示全部是修改
		if (updateCnt >= entities.size()) {
			logger.debug("级联子表{}修改记录数为:{}", tableName, updateCnt);
			return;
		}

		// mysql只支持identity,sequence 值忽略
		boolean isAssignPK = MySqlDialectUtils.isAssignPKValue(entityMeta.getIdStrategy());
		String insertSql = DialectExtUtils
				.generateInsertSql(dbType, entityMeta, entityMeta.getIdStrategy(), "ifnull",
						"NEXTVAL FOR " + entityMeta.getSequence(), isAssignPK, tableName)
				.replaceFirst("(?i)insert ", "insert ignore ");
		Long saveCnt = saveAll(sqlToyContext, entityMeta, entityMeta.getIdStrategy(), isAssignPK, insertSql, entities,
				batchSize, reflectPropsHandler, conn, dbType, null);
		logger.debug("级联子表:{} 变更记录数:{},新建记录数为:{}", tableName, updateCnt, saveCnt);
	}

	// 针对oceanBase
	private static void oceanBaseSaveOrUpdateAll(SqlToyContext sqlToyContext, final EntityMeta entityMeta,
			List<?> entities, ReflectPropsHandler reflectPropsHandler, final String[] forceUpdateFields,
			Connection conn, final Integer dbType) throws Exception {
		int batchSize = sqlToyContext.getBatchSize();
		final String tableName = entityMeta.getSchemaTable(null, dbType);
		Long updateCnt = updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropsHandler, "nvl",
				conn, dbType, null, tableName, true);
		// 如果修改的记录数量跟总记录数量一致,表示全部是修改
		if (updateCnt >= entities.size()) {
			logger.debug("级联子表{}修改记录数为:{}", tableName, updateCnt);
			return;
		}

		Long saveCnt = saveAllIgnoreExist(sqlToyContext, entities, batchSize, entityMeta, new GenerateSqlHandler() {
			@Override
			public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
				PKStrategy pkStrategy = entityMeta.getIdStrategy();
				String sequence = entityMeta.getSequence() + ".nextval";
				if (pkStrategy != null && pkStrategy.equals(PKStrategy.identity)) {
					pkStrategy = PKStrategy.sequence;
					sequence = entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue();
				}
				return DialectExtUtils.mergeIgnore(dbType, entityMeta, pkStrategy, "dual", "nvl", sequence,
						OracleDialectUtils.isAssignPKValue(pkStrategy), tableName);
			}
		}, reflectPropsHandler, conn, dbType, null);
		logger.debug("级联子表:{} 变更记录数:{},新建记录数为:{}", tableName, updateCnt, saveCnt);
	}

	// 针对postgresql 数据库
	private static void postgreSaveOrUpdateAll(SqlToyContext sqlToyContext, final EntityMeta entityMeta,
			List<?> entities, ReflectPropsHandler reflectPropsHandler, final String[] forceUpdateFields,
			Connection conn, final Integer dbType) throws Exception {
		int batchSize = sqlToyContext.getBatchSize();
		final String tableName = entityMeta.getSchemaTable(null, dbType);
		Long updateCnt = updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropsHandler,
				"COALESCE", conn, dbType, null, tableName, true);
		// 如果修改的记录数量跟总记录数量一致,表示全部是修改
		if (updateCnt >= entities.size()) {
			logger.debug("级联子表{}修改记录数为:{}", tableName, updateCnt);
			return;
		}
		Long saveCnt = saveAllIgnoreExist(sqlToyContext, entities, batchSize, entityMeta, new GenerateSqlHandler() {
			@Override
			public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
				PKStrategy pkStrategy = entityMeta.getIdStrategy();
				String sequence = "nextval('" + entityMeta.getSequence() + "')";
				if (pkStrategy != null && pkStrategy.equals(PKStrategy.identity)) {
					// 伪造成sequence模式
					pkStrategy = PKStrategy.sequence;
					sequence = "DEFAULT";
				}
				boolean isAssignPK = PostgreSqlDialectUtils.isAssignPKValue(pkStrategy);
				return DialectExtUtils.insertIgnore(dbType, entityMeta, pkStrategy, "COALESCE", sequence, isAssignPK,
						tableName);
			}
		}, reflectPropsHandler, conn, dbType, null);
		logger.debug("级联子表:{} 变更记录数:{},新建记录数为:{}", tableName, updateCnt, saveCnt);
	}

	// 针对sqlite 数据库
	private static void sqliteSaveOrUpdateAll(SqlToyContext sqlToyContext, final EntityMeta entityMeta,
			List<?> entities, ReflectPropsHandler reflectPropsHandler, final String[] forceUpdateFields,
			Connection conn, final Integer dbType) throws Exception {
		int batchSize = sqlToyContext.getBatchSize();
		final String tableName = entityMeta.getSchemaTable(null, dbType);
		Long updateCnt = updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropsHandler, "ifnull",
				conn, dbType, null, tableName, true);
		// 如果修改的记录数量跟总记录数量一致,表示全部是修改
		if (updateCnt >= entities.size()) {
			logger.debug("级联子表{}修改记录数为:{}", tableName, updateCnt);
			return;
		}
		// sqlite只支持identity,sequence 值忽略
		boolean isAssignPK = SqliteDialectUtils.isAssignPKValue(entityMeta.getIdStrategy());
		String insertSql = DialectExtUtils
				.generateInsertSql(dbType, entityMeta, entityMeta.getIdStrategy(), "ifnull",
						"NEXTVAL FOR " + entityMeta.getSequence(), isAssignPK, tableName)
				.replaceFirst("(?i)insert ", "insert or ignore into ");
		Long saveCnt = saveAll(sqlToyContext, entityMeta, entityMeta.getIdStrategy(), isAssignPK, insertSql, entities,
				batchSize, reflectPropsHandler, conn, dbType, null);
		logger.debug("级联子表:{} 变更记录数:{},新建记录数为:{}", tableName, updateCnt, saveCnt);
	}

	// 针对达梦数据库
	private static void dmSaveOrUpdateAll(SqlToyContext sqlToyContext, final EntityMeta entityMeta, List<?> entities,
			ReflectPropsHandler reflectPropsHandler, final String[] forceUpdateFields, Connection conn,
			final Integer dbType) throws Exception {
		int batchSize = sqlToyContext.getBatchSize();
		final String tableName = entityMeta.getSchemaTable(null, dbType);
		Long updateCnt = updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropsHandler, "nvl",
				conn, dbType, null, tableName, true);
		// 如果修改的记录数量跟总记录数量一致,表示全部是修改
		if (updateCnt >= entities.size()) {
			logger.debug("级联子表{}修改记录数为:{}", tableName, updateCnt);
			return;
		}
		Long saveCnt = saveAllIgnoreExist(sqlToyContext, entities, batchSize, entityMeta, new GenerateSqlHandler() {
			@Override
			public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
				PKStrategy pkStrategy = entityMeta.getIdStrategy();
				String sequence = entityMeta.getSequence() + ".nextval";
				return DialectExtUtils.mergeIgnore(dbType, entityMeta, pkStrategy, "dual", "nvl", sequence,
						DMDialectUtils.isAssignPKValue(pkStrategy), tableName);
			}
		}, reflectPropsHandler, conn, dbType, null);
		logger.debug("级联子表:{} 变更记录数:{},新建记录数为:{}", tableName, updateCnt, saveCnt);
	}

	// 针对人大金仓kingbase数据库
	private static void kingbaseSaveOrUpdateAll(SqlToyContext sqlToyContext, final EntityMeta entityMeta,
			List<?> entities, ReflectPropsHandler reflectPropsHandler, final String[] forceUpdateFields,
			Connection conn, final Integer dbType) throws Exception {
		int batchSize = sqlToyContext.getBatchSize();
		final String tableName = entityMeta.getSchemaTable(null, dbType);
		Long updateCnt = updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropsHandler, "NVL",
				conn, dbType, null, tableName, true);
		// 如果修改的记录数量跟总记录数量一致,表示全部是修改
		if (updateCnt >= entities.size()) {
			logger.debug("级联子表{}修改记录数为:{}", tableName, updateCnt);
			return;
		}
		// mysql只支持identity,sequence 值忽略
		boolean isAssignPK = KingbaseDialectUtils.isAssignPKValue(entityMeta.getIdStrategy());
		String insertSql = DialectExtUtils.insertIgnore(dbType, entityMeta, entityMeta.getIdStrategy(), "NVL",
				"nextval('" + entityMeta.getSequence() + "')", isAssignPK, tableName);
		Long saveCnt = saveAll(sqlToyContext, entityMeta, entityMeta.getIdStrategy(), isAssignPK, insertSql, entities,
				batchSize, reflectPropsHandler, conn, dbType, null);
		logger.debug("级联子表:{} 变更记录数:{},新建记录数为:{}", tableName, updateCnt, saveCnt);
	}

	/**
	 * @todo 批量对象修改
	 * @param sqlToyContext
	 * @param entities
	 * @param batchSize
	 * @param forceUpdateFields
	 * @param reflectPropsHandler
	 * @param nullFunction
	 * @param conn
	 * @param dbType
	 * @param autoCommit
	 * @param tableName
	 * @param skipNull
	 * @return
	 * @throws Exception
	 */
	public static Long updateAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			final String[] forceUpdateFields, ReflectPropsHandler reflectPropsHandler, String nullFunction,
			Connection conn, final Integer dbType, final Boolean autoCommit, String tableName, boolean skipNull)
			throws Exception {
		if (entities == null || entities.isEmpty()) {
			return 0L;
		}
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		// 无主键
		if (entityMeta.getIdArray() == null) {
			throw new IllegalArgumentException("表:" + realTable + " 无主键,不符合update/updateAll规则,请检查表设计是否合理!");
		}
		// 全部是主键则无需update
		if (entityMeta.getRejectIdFieldArray() == null) {
			logger.warn("表:" + realTable + " 字段全部是主键不存在更新字段,无需执行更新操作!");
			return 0L;
		}
		// 构造全新的修改记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = getUpdateReflectHandler(reflectPropsHandler, forceUpdateFields,
				sqlToyContext.getUnifyFieldsHandler());
		handler = getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		List<Object[]> paramsValues = BeanUtil.reflectBeansToInnerAry(entities, entityMeta.getFieldsArray(), null,
				handler);
		// 判断主键是否为空
		int pkIndex = entityMeta.getIdIndex();
		int end = pkIndex + entityMeta.getIdArray().length;
		int index = 0;
		// 累计多少行为空
		int skipCount = 0;
		Iterator<Object[]> iter = paramsValues.iterator();
		Object[] rowValues;
		while (iter.hasNext()) {
			rowValues = iter.next();
			for (int i = pkIndex; i < end; i++) {
				// 判断主键值是否为空
				if (StringUtil.isBlank(rowValues[i])) {
					// 跳过主键值为空的
					if (skipNull) {
						skipCount++;
						iter.remove();
						break;
					} else {
						throw new IllegalArgumentException(
								"通过对象对表" + realTable + " 进行updateAll操作,主键字段必须要赋值!第:" + index + " 条记录主键为null!");
					}
				}
			}
			index++;
		}
		if (skipCount > 0) {
			logger.debug("共有:{}行记录因为主键值为空跳过修改操作!", skipCount);
		}
		// 构建update语句
		String updateSql = generateUpdateSql(dbType, entityMeta, nullFunction, forceUpdateFields, realTable);
		if (updateSql == null) {
			throw new IllegalArgumentException("updateAll sql is null,引起问题的原因是没有设置需要修改的字段!");
		}
		SqlExecuteStat.showSql("批量修改[" + paramsValues.size() + "]条记录", updateSql, null);
		return SqlUtilsExt.batchUpdateForPOJO(sqlToyContext.getTypeHandler(), updateSql, paramsValues,
				entityMeta.getFieldsTypeArray(), null, batchSize, autoCommit, conn, dbType);
	}

	/**
	 * @todo 删除单个对象以及其级联表数据
	 * @param sqlToyContext
	 * @param entity
	 * @param conn
	 * @param dbType
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static Long delete(SqlToyContext sqlToyContext, Serializable entity, Connection conn, final Integer dbType,
			final String tableName) throws Exception {
		if (entity == null) {
			return 0L;
		}
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		if (null == entityMeta.getIdArray()) {
			throw new IllegalArgumentException("delete 操作,表:" + realTable + " 没有主键,请检查表设计!");
		}
		Object[] idValues = BeanUtil.reflectBeanToAry(entity, entityMeta.getIdArray());
		Integer[] parameterTypes = new Integer[idValues.length];
		boolean validator = true;
		// 判断主键值是否为空
		for (int i = 0, n = idValues.length; i < n; i++) {
			parameterTypes[i] = entityMeta.getColumnJdbcType(entityMeta.getIdArray()[i]);
			if (StringUtil.isBlank(idValues[i])) {
				validator = false;
				break;
			}
		}
		if (!validator) {
			throw new IllegalArgumentException(realTable
					+ " delete operate is illegal,table must has primary key and all primaryKey's value must has value!");
		}
		// 级联删除子表数据
		if (!entityMeta.getCascadeModels().isEmpty()) {
			int mapFieldSize;
			for (TableCascadeModel cascadeModel : entityMeta.getCascadeModels()) {
				EntityMeta subMeta;
				// 如果数据库本身通过on delete cascade机制，则sqltoy无需进行删除操作
				if (cascadeModel.isDelete()) {
					subMeta = sqlToyContext.getEntityMeta(cascadeModel.getMappedType());
					Object[] mainFieldValues = BeanUtil.reflectBeanToAry(entity, cascadeModel.getFields());
					mapFieldSize = cascadeModel.getFields().length;
					for (int i = 0; i < mapFieldSize; i++) {
						if (mainFieldValues[i] == null) {
							throw new IllegalArgumentException("表:" + realTable + " 级联删除子表:" + subMeta.getTableName()
									+ " 对应属性:" + cascadeModel.getFields()[i] + " 值为null!");
						}
					}
					Integer[] subTableFieldType = new Integer[mapFieldSize];
					for (int i = 0, n = mapFieldSize; i < n; i++) {
						subTableFieldType[i] = subMeta.getColumnJdbcType(cascadeModel.getMappedFields()[i]);
					}
					SqlExecuteStat.debug("执行级联删除操作", null);
					SqlUtil.executeSql(sqlToyContext.getTypeHandler(), cascadeModel.getDeleteSubTableSql(),
							mainFieldValues, subTableFieldType, conn, dbType, null, true);
				}
			}
		}
		String deleteSql = "delete from ".concat(realTable).concat(" ").concat(entityMeta.getIdArgWhereSql());
		return SqlUtil.executeSql(sqlToyContext.getTypeHandler(), deleteSql, idValues, parameterTypes, conn, dbType,
				null, true);
	}

	/**
	 * @todo 批量删除对象并级联删除掉子表数据
	 * @param sqlToyContext
	 * @param entities
	 * @param batchSize
	 * @param conn
	 * @param dbType
	 * @param autoCommit
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static Long deleteAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize, Connection conn,
			final Integer dbType, final Boolean autoCommit, final String tableName) throws Exception {
		if (null == entities || entities.isEmpty()) {
			return 0L;
		}
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		if (null == entityMeta.getIdArray()) {
			throw new IllegalArgumentException("delete/deleteAll 操作,表:" + realTable + " 没有主键,请检查表设计!");
		}
		List<Object[]> idValues = BeanUtil.reflectBeansToInnerAry(entities, entityMeta.getIdArray(), null, null);
		// 判断主键值是否存在空
		Object[] idsValue;
		for (int i = 0, n = idValues.size(); i < n; i++) {
			idsValue = idValues.get(i);
			for (Object obj : idsValue) {
				if (StringUtil.isBlank(obj)) {
					throw new IllegalArgumentException("第[" + i + "]行数据主键值存在空,批量删除以主键为依据，表:" + realTable + " 主键不能为空!");
				}
			}
		}
		int idsLength = entityMeta.getIdArray().length;
		Integer[] parameterTypes = new Integer[idsLength];
		for (int i = 0, n = idsLength; i < n; i++) {
			parameterTypes[i] = entityMeta.getColumnJdbcType(entityMeta.getIdArray()[i]);
		}
		// 级联批量删除子表数据
		if (!entityMeta.getCascadeModels().isEmpty()) {
			EntityMeta subTableMeta;
			String delSubTableSql;
			int mapFieldSize;
			int meter = 0;
			for (TableCascadeModel cascadeModel : entityMeta.getCascadeModels()) {
				// 如果数据库本身通过on delete cascade机制，则sqltoy无需进行删除操作
				if (cascadeModel.isDelete()) {
					subTableMeta = sqlToyContext.getEntityMeta(cascadeModel.getMappedType());
					List<Object[]> mainFieldValues = BeanUtil.reflectBeansToInnerAry(entities, cascadeModel.getFields(),
							null, null);
					mapFieldSize = cascadeModel.getFields().length;
					meter = 0;
					for (Object[] row : mainFieldValues) {
						for (int i = 0; i < mapFieldSize; i++) {
							if (row[i] == null) {
								throw new IllegalArgumentException(
										"第:" + meter + "行,表:" + realTable + " 级联删除子表:" + subTableMeta.getTableName()
												+ " 对应属性:" + cascadeModel.getFields()[i] + " 值为null!");
							}
						}
						meter++;
					}
					Integer[] subTableFieldType = new Integer[mapFieldSize];
					for (int i = 0, n = mapFieldSize; i < n; i++) {
						subTableFieldType[i] = subTableMeta.getColumnJdbcType(cascadeModel.getMappedFields()[i]);
					}
					delSubTableSql = ReservedWordsUtil.convertSql(cascadeModel.getDeleteSubTableSql(), dbType);
					SqlExecuteStat.showSql("级联删除子表记录", delSubTableSql, null);
					SqlUtilsExt.batchUpdateForPOJO(sqlToyContext.getTypeHandler(), delSubTableSql, mainFieldValues,
							subTableFieldType, null, sqlToyContext.getBatchSize(), null, conn, dbType);
				}
			}
		}
		String deleteSql = ReservedWordsUtil
				.convertSql("delete from ".concat(realTable).concat(" ").concat(entityMeta.getIdArgWhereSql()), dbType);
		SqlExecuteStat.showSql("批量删除[" + idValues.size() + "]条记录", deleteSql, null);
		return SqlUtilsExt.batchUpdateForPOJO(sqlToyContext.getTypeHandler(), deleteSql, idValues, parameterTypes, null,
				batchSize, autoCommit, conn, dbType);
	}

	/**
	 * @todo 进行唯一性查询判定
	 * @param sqlToyContext
	 * @param entity
	 * @param paramsNamed
	 * @param conn
	 * @param dbType
	 * @param tableName
	 * @param uniqueSqlHandler
	 * @return
	 */
	public static boolean isUnique(SqlToyContext sqlToyContext, Serializable entity, final String[] paramsNamed,
			Connection conn, final Integer dbType, final String tableName, final UniqueSqlHandler uniqueSqlHandler) {
		try {
			EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
			String[] realParamNamed;
			Object[] paramValues;
			int rejectIdFieldsSize = (entityMeta.getRejectIdFieldArray() == null) ? 0
					: entityMeta.getRejectIdFieldArray().length;
			// 如果没有特别指定属性，则通过数据是否为null来判断具体的字段
			if (paramsNamed == null || paramsNamed.length == 0) {
				String[] fieldsArray = entityMeta.getFieldsArray();
				Object[] fieldValues = BeanUtil.reflectBeanToAry(entity, fieldsArray);
				List paramValueList = new ArrayList();
				List<String> paramNames = new ArrayList<String>();
				boolean hasNoPkField = false;
				for (int i = 0; i < fieldValues.length; i++) {
					if (null != fieldValues[i]) {
						// 非主键字段
						if (i < rejectIdFieldsSize) {
							hasNoPkField = true;
						}
						// 存在主键字段，则主键值仅仅作为返回结果的比较，判断是否是记录本身
						if (i >= rejectIdFieldsSize && hasNoPkField) {
							break;
						}
						paramNames.add(fieldsArray[i]);
						paramValueList.add(fieldValues[i]);
					}
				}
				paramValues = paramValueList.toArray();
				realParamNamed = paramNames.toArray(new String[paramNames.size()]);
			} else {
				realParamNamed = paramsNamed;
				paramValues = BeanUtil.reflectBeanToAry(entity, paramsNamed);
			}
			// 取出符合条件的2条记录
			String queryStr = uniqueSqlHandler.process(entityMeta, realParamNamed, tableName, 2);
			SqlExecuteStat.showSql("唯一性验证", queryStr, paramValues);
			List result = SqlUtil.findByJdbcQuery(sqlToyContext.getTypeHandler(), queryStr, paramValues, null, null,
					null, conn, dbType, false, null, -1, -1);
			if (result.size() == 0) {
				return true;
			}
			if (result.size() > 1) {
				return false;
			}
			// 表没有主键,单条记录算重复
			if (null == entityMeta.getIdArray()) {
				return false;
			}
			boolean allPK = false;
			// 判断是否是主键字段的唯一性验证
			if (realParamNamed.length == entityMeta.getIdArray().length) {
				allPK = true;
				for (String field : realParamNamed) {
					if (!entityMeta.getFieldMeta(field).isPK()) {
						allPK = false;
						break;
					}
				}
			}
			// 针对主键字段的唯一性验证,查询有记录则表示主键已经存在
			if (allPK) {
				return false;
			}
			// 判断是否是本身
			Object[] idValues = BeanUtil.reflectBeanToAry(entity, entityMeta.getIdArray());
			List compareValues = (List) result.get(0);
			// 相等表示唯一
			boolean isEqual = true;
			for (int i = 0, n = idValues.length; i < n; i++) {
				// result 第一列数据为固定的1(select 1,pk1,pk2模式),因此compareValues(i+1);
				if (null == idValues[i] || null == compareValues.get(i + 1)
						|| !idValues[i].toString().equals(compareValues.get(i + 1).toString())) {
					isEqual = false;
					break;
				}
			}
			return isEqual;
		} catch (Exception e) {
			logger.error("执行唯一性查询失败:{}", e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @todo 判断是否复杂分页查询(union,多表关联、存在top 、distinct等)
	 * @param queryStr
	 * @return
	 */
	public static boolean isComplexPageQuery(String queryStr) {
		// 清除不必要的字符并转小写
		String tmpQuery = SqlUtil.clearMistyChars(queryStr.toLowerCase(), " ");
		boolean isComplexQuery = SqlUtil.hasUnion(tmpQuery, false);
		// from 和 where之间有","表示多表查询
		if (!isComplexQuery) {
			int fromIndex = StringUtil.getSymMarkMatchIndex(SELECT_REGEX, FROM_REGEX, tmpQuery, 0);
			int fromWhereIndex = StringUtil.getSymMarkMatchIndex(FROM_REGEX, WHERE_REGEX, tmpQuery, fromIndex - 1);
			String fromLastStr = (fromWhereIndex == -1) ? tmpQuery.substring(fromIndex)
					: tmpQuery.substring(fromIndex, fromWhereIndex);
			if (fromLastStr.indexOf(",") != -1 || fromLastStr.indexOf(" join ") != -1
					|| fromLastStr.indexOf("(") != -1) {
				isComplexQuery = true;
			}

			// 不存在union且非复杂关联查询
			if (!isComplexQuery) {
				// 截取select 到 from之间的字段
				String tmpColumn = tmpQuery.substring(0, fromIndex);
				if (tmpColumn.indexOf(" top ") != -1 || tmpColumn.indexOf(" distinct ") != -1) {
					isComplexQuery = true;
				}
			}
		}
		return isComplexQuery;
	}

	/**
	 * @todo 判断是否有order by 和union 逻辑语句
	 * @param sql
	 * @return
	 */
	public static boolean hasOrderByOrUnion(String sql) {
		String unDisturbSql = clearDisturbSql(sql);
		if (StringUtil.matches(unDisturbSql, UNION_PATTERN) || StringUtil.matches(unDisturbSql, ORDER_BY_PATTERN)) {
			return true;
		}
		return false;
	}

	/**
	 * @todo 去除掉sql中的所有对称的括号中的内容，排除干扰
	 * @param sql
	 * @return
	 */
	public static String clearDisturbSql(String sql) {
		StringBuilder lastSql = new StringBuilder(sql);
		// 找到第一个select 所对称的from位置，排除掉子查询中的内容
		int fromIndex = StringUtil.getSymMarkMatchIndex(SELECT_REGEX, FROM_REGEX, sql.toLowerCase(), 0);
		if (fromIndex != -1) {
			lastSql.delete(0, fromIndex);
		}
		// 删除所有对称的括号中的内容
		int start = lastSql.indexOf("(");
		int symMarkEnd;
		while (start != -1) {
			symMarkEnd = StringUtil.getSymMarkIndex("(", ")", lastSql.toString(), start);
			if (symMarkEnd != -1) {
				lastSql.delete(start, symMarkEnd + 1);
				start = lastSql.indexOf("(");
			} else {
				break;
			}
		}
		return lastSql.toString();
	}

	/**
	 * @todo 去除掉sql中的所有对称的select 和 from 中的内容，排除干扰
	 * @param sql
	 * @return
	 */
	private static String clearSymSelectFromSql(String sql) {
		// 先转化为小写
		String realSql = sql.toLowerCase();
		StringBuilder lastSql = new StringBuilder(realSql);
		String SELECT_REGEX = "\\Wselect\\s+";
		String FROM_REGEX = "\\sfrom[\\(|\\s+]";

		// 删除所有对称的括号中的内容
		int start = StringUtil.matchIndex(realSql, SELECT_REGEX);
		int symMarkEnd;
		while (start != -1) {
			symMarkEnd = StringUtil.getSymMarkMatchIndex(SELECT_REGEX, FROM_REGEX, lastSql.toString(), start);
			if (symMarkEnd != -1) {
				lastSql.delete(start + 1, symMarkEnd + 5);
				start = StringUtil.matchIndex(lastSql.toString(), SELECT_REGEX);
			} else {
				break;
			}
		}
		return lastSql.toString();
	}

	/**
	 * @todo <b>通用的存储过程调用，inParam需放在outParam前面</b>
	 * @param sqlToyConfig
	 * @param sqlToyContext
	 * @param storeSql
	 * @param inParamValues
	 * @param outParamTypes
	 * @param conn
	 * @param dbType
	 * @param fetchSize
	 * @return
	 * @throws Exception
	 */
	public static StoreResult executeStore(final SqlToyConfig sqlToyConfig, final SqlToyContext sqlToyContext,
			final String storeSql, final Object[] inParamValues, final Integer[] outParamTypes, final Connection conn,
			final Integer dbType, final int fetchSize) throws Exception {
		CallableStatement callStat = null;
		ResultSet rs = null;
		return (StoreResult) SqlUtil.callableStatementProcess(null, callStat, rs, new CallableStatementResultHandler() {
			@Override
			public void execute(Object obj, CallableStatement callStat, ResultSet rs) throws Exception {
				callStat = conn.prepareCall(storeSql);
				if (fetchSize > 0) {
					callStat.setFetchSize(fetchSize);
				}
				boolean isFirstResult = StringUtil.matches(storeSql, STORE_PATTERN);
				int addIndex = isFirstResult ? 1 : 0;
				SqlUtil.setParamsValue(sqlToyContext.getTypeHandler(), conn, dbType, callStat, inParamValues, null,
						addIndex);
				int inCount = (inParamValues == null) ? 0 : inParamValues.length;
				int outCount = (outParamTypes == null) ? 0 : outParamTypes.length;

				// 注册输出参数
				if (outCount != 0) {
					if (isFirstResult) {
						callStat.registerOutParameter(1, outParamTypes[0]);
					}
					for (int i = addIndex; i < outCount; i++) {
						callStat.registerOutParameter(i + inCount + 1, outParamTypes[i]);
					}
				}
				callStat.execute();
				rs = callStat.getResultSet();
				StoreResult storeResult = new StoreResult();
				if (rs != null) {
					QueryResult tempResult = ResultUtils.processResultSet(sqlToyContext, sqlToyConfig, conn, rs, null,
							null, null, 0);
					storeResult.setLabelNames(tempResult.getLabelNames());
					storeResult.setLabelTypes(tempResult.getLabelTypes());
					storeResult.setRows(tempResult.getRows());
				}

				// 有返回参数如:(?=call (? in,? out) )
				if (outCount != 0) {
					Object[] outParams = new Object[outCount];
					if (isFirstResult) {
						outParams[0] = callStat.getObject(1);
					}
					for (int i = addIndex; i < outCount; i++) {
						outParams[i] = callStat.getObject(i + inCount + 1);
					}
					storeResult.setOutResult(outParams);
				}
				this.setResult(storeResult);
			}
		});
	}

	/**
	 * @todo 构造新增记录参数反射赋值处理器
	 * @param preHandler
	 * @param unifyFieldsHandler
	 * @return
	 */
	public static ReflectPropsHandler getAddReflectHandler(final ReflectPropsHandler preHandler,
			UnifyFieldsHandler unifyFieldsHandler) {
		if (unifyFieldsHandler == null) {
			return preHandler;
		}
		final Map<String, Object> keyValues = unifyFieldsHandler.createUnifyFields();
		if (keyValues == null || keyValues.isEmpty()) {
			return preHandler;
		}
		// 强制修改字段赋值
		IgnoreCaseSet tmpSet = unifyFieldsHandler.forceUpdateFields();
		final IgnoreCaseSet forceUpdateFields = (tmpSet == null) ? new IgnoreCaseSet() : tmpSet;
		ReflectPropsHandler handler = new ReflectPropsHandler() {
			@Override
			public void process() {
				if (preHandler != null) {
					preHandler.setPropertyIndexMap(this.getPropertyIndexMap());
					preHandler.setRowIndex(this.getRowIndex());
					preHandler.setRowData(this.getRowData());
					preHandler.process();
				}
				for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
					if (StringUtil.isBlank(this.getValue(entry.getKey()))
							|| forceUpdateFields.contains(entry.getKey())) {
						this.setValue(entry.getKey(), entry.getValue());
					}
				}
			}
		};
		return handler;
	}

	/**
	 * @todo 构造修改记录参数反射赋值处理器
	 * @param preHandler
	 * @param forceUpdateProps
	 * @param unifyFieldsHandler
	 * @return
	 */
	public static ReflectPropsHandler getUpdateReflectHandler(final ReflectPropsHandler preHandler,
			String[] forceUpdateProps, UnifyFieldsHandler unifyFieldsHandler) {
		if (unifyFieldsHandler == null) {
			return preHandler;
		}
		final Map<String, Object> keyValues = unifyFieldsHandler.updateUnifyFields();
		if (keyValues == null || keyValues.isEmpty()) {
			return preHandler;
		}
		// update操作强制更新字段优先
		final Set<String> forceSet = new HashSet<String>();
		if (forceUpdateProps != null && forceUpdateProps.length > 0) {
			for (String field : forceUpdateProps) {
				forceSet.add(field.toLowerCase().replace("_", ""));
			}
		}
		// 强制修改字段赋值
		IgnoreCaseSet tmpSet = unifyFieldsHandler.forceUpdateFields();
		final IgnoreCaseSet forceUpdateFields = (tmpSet == null) ? new IgnoreCaseSet() : tmpSet;
		ReflectPropsHandler handler = new ReflectPropsHandler() {
			@Override
			public void process() {
				if (preHandler != null) {
					preHandler.setPropertyIndexMap(this.getPropertyIndexMap());
					preHandler.setRowIndex(this.getRowIndex());
					preHandler.setRowData(this.getRowData());
					preHandler.process();
				}
				// 修改操作
				for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
					// 统一修改字段不在强制更新字段范围内
					if (!forceSet.contains(entry.getKey().toLowerCase())) {
						if (StringUtil.isBlank(this.getValue(entry.getKey()))
								|| forceUpdateFields.contains(entry.getKey())) {
							this.setValue(entry.getKey(), entry.getValue());
						}
					}
				}
			}
		};
		return handler;
	}

	/**
	 * @TODO 对字段值进行加密
	 * @param preHandler
	 * @param fieldsSecureProvider
	 * @param desensitizeProvider
	 * @param secureFields
	 * @return
	 */
	public static ReflectPropsHandler getSecureReflectHandler(final ReflectPropsHandler preHandler,
			final FieldsSecureProvider fieldsSecureProvider, final DesensitizeProvider desensitizeProvider,
			List<FieldSecureConfig> secureFields) {
		if (fieldsSecureProvider == null || secureFields == null || secureFields.isEmpty()) {
			return preHandler;
		}
		ReflectPropsHandler handler = new ReflectPropsHandler() {
			@Override
			public void process() {
				if (preHandler != null) {
					preHandler.setPropertyIndexMap(this.getPropertyIndexMap());
					preHandler.setRowIndex(this.getRowIndex());
					preHandler.setRowData(this.getRowData());
					preHandler.process();
				}
				Object value;
				String contents;
				String field;
				String sourceField;
				// 加密操作
				for (FieldSecureConfig config : secureFields) {
					field = config.getField();
					sourceField = config.getSourceField();
					// 安全脱敏便于检索的字段，优先依据其来源加密字段
					if (StringUtil.isNotBlank(sourceField)) {
						value = this.getValue(sourceField);
					} else {
						value = this.getValue(field);
					}
					if (value != null) {
						contents = value.toString();
						if (!"".equals(contents)) {
							// 加密
							if (config.getSecureType().equals(SecureType.ENCRYPT)) {
								this.setValue(field, fieldsSecureProvider.encrypt(contents));
							} // 脱敏
							else {
								this.setValue(field, desensitizeProvider.desensitize(contents, config.getMask()));
							}
						}
					}
				}
			}
		};
		return handler;
	}

	/**
	 * @todo 构造创建和修改记录时的反射
	 * @param idFields
	 * @param prepHandler
	 * @param forceUpdateProps
	 * @param unifyFieldsHandler
	 * @return
	 */
	public static ReflectPropsHandler getSaveOrUpdateReflectHandler(final String[] idFields,
			final ReflectPropsHandler prepHandler, String[] forceUpdateProps, UnifyFieldsHandler unifyFieldsHandler) {
		if (unifyFieldsHandler == null) {
			return prepHandler;
		}
		final Map<String, Object> addKeyValues = unifyFieldsHandler.createUnifyFields();
		final Map<String, Object> updateKeyValues = unifyFieldsHandler.updateUnifyFields();
		if ((addKeyValues == null || addKeyValues.isEmpty())
				&& (updateKeyValues == null || updateKeyValues.isEmpty())) {
			return prepHandler;
		}
		// update操作强制更新字段优先
		final Set<String> forceSet = new HashSet<String>();
		if (forceUpdateProps != null && forceUpdateProps.length > 0) {
			for (String field : forceUpdateProps) {
				forceSet.add(field.toLowerCase().replace("_", ""));
			}
		}
		// 强制修改字段赋值
		IgnoreCaseSet tmpSet = unifyFieldsHandler.forceUpdateFields();
		final IgnoreCaseSet forceUpdateFields = (tmpSet == null) ? new IgnoreCaseSet() : tmpSet;
		final int idLength = (idFields == null) ? 0 : idFields.length;
		// 构造一个新的包含update和save 的字段处理
		ReflectPropsHandler handler = new ReflectPropsHandler() {
			@Override
			public void process() {
				if (prepHandler != null) {
					prepHandler.setPropertyIndexMap(this.getPropertyIndexMap());
					prepHandler.setRowIndex(this.getRowIndex());
					prepHandler.setRowData(this.getRowData());
					prepHandler.process();
				}
				// 主键为空表示save操作
				if (idLength > 0 && this.getValue(idFields[0]) == null) {
					for (Map.Entry<String, Object> entry : addKeyValues.entrySet()) {
						if (StringUtil.isBlank(this.getValue(entry.getKey()))) {
							this.setValue(entry.getKey(), entry.getValue());
						}
					}
				}
				// 修改属性值
				for (Map.Entry<String, Object> entry : updateKeyValues.entrySet()) {
					// 统一修改字段不在强制更新字段范围内
					if (!forceSet.contains(entry.getKey().toLowerCase())) {
						if (StringUtil.isBlank(this.getValue(entry.getKey()))
								|| forceUpdateFields.contains(entry.getKey())) {
							this.setValue(entry.getKey(), entry.getValue());
						}
					}
				}
			}
		};
		return handler;
	}

	/**
	 * @todo 提取sql中参数的个数
	 * @param queryStr
	 * @return
	 */
	public static int getParamsCount(String queryStr) {
		if (StringUtil.isBlank(queryStr)) {
			return 0;
		}
		String sql = SqlConfigParseUtils.clearDblQuestMark(queryStr);
		// 判断sql中参数模式，?或:named 模式，两种模式不可以混合使用
		if (sql.indexOf(SqlConfigParseUtils.ARG_NAME) == -1) {
			return StringUtil.matchCnt(sql, SqlToyConstants.SQL_NAMED_PATTERN);
		}
		return StringUtil.matchCnt(sql, SqlConfigParseUtils.ARG_REGEX);
	}

	/**
	 * @TODO 判断主键值是否为空，用于saveOrUpdate判断是否save
	 * @param sqlToyContext
	 * @param entity
	 * @return
	 */
	public static boolean isEmptyPK(SqlToyContext sqlToyContext, Serializable entity) {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		if (entityMeta.getIdArray() != null && entityMeta.getIdArray().length > 0) {
			Object[] idValues = BeanUtil.reflectBeanToAry(entity, entityMeta.getIdArray());
			for (Object obj : idValues) {
				if (null == obj) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @TODO 针对批量sql执行，判断是新增还是修改，并统一填充公共字段信息
	 * @param sql
	 * @param reflectPropsHandler
	 * @param unifyFieldsHandler
	 * @return
	 */
	public static ReflectPropsHandler wrapReflectWithUnifyFields(String sql, ReflectPropsHandler reflectPropsHandler,
			UnifyFieldsHandler unifyFieldsHandler) {
		if ((reflectPropsHandler == null && unifyFieldsHandler == null) || StringUtil.isBlank(sql)) {
			return null;
		}
		ReflectPropsHandler result = null;
		// insert 语句
		if (StringUtil.matches(sql.trim(), "(?i)^insert\\s+into\\W")) {
			result = getAddReflectHandler(reflectPropsHandler, unifyFieldsHandler);
		} // update
		else if (StringUtil.matches(sql.trim(), "(?i)^update\\s+")
				|| StringUtil.matches(sql.trim(), "(?i)^merge\\s+into\\W")
				|| StringUtil.matches(sql.trim(), "(?i)^replace\\s+into\\W")) {
			result = getUpdateReflectHandler(reflectPropsHandler, null, unifyFieldsHandler);
		}
		return result;
	}
}