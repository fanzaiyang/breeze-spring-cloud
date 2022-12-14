package cn.fanzy.breeze.sqltoy.plus.conditions;

import cn.fanzy.breeze.sqltoy.plus.conditions.eumn.CompareEnum;
import cn.fanzy.breeze.sqltoy.plus.conditions.eumn.SqlKeyword;
import cn.fanzy.breeze.sqltoy.plus.conditions.interfaces.Operated;
import cn.fanzy.breeze.sqltoy.plus.conditions.segments.FiledMappingStrategy;
import cn.fanzy.breeze.sqltoy.plus.conditions.segments.FiledValueFilterStrategy;
import cn.fanzy.breeze.sqltoy.plus.conditions.segments.MergeSegments;
import cn.fanzy.breeze.sqltoy.plus.conditions.segments.SqlSegmentMeta;
import cn.fanzy.breeze.sqltoy.plus.conditions.toolkit.StringPool;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public abstract class AbstractWrapper<T, R, Children extends AbstractWrapper<T, R, Children>> implements Operated<Children, R>, Wrapper<T>{

    /**
     * 占位符
     */
    protected final Children typedThis = (Children)this;

    /**
     * 必要度量
     */
    protected AtomicInteger paramNameSeq;

    /**
     * sql语句存储对象
     */
    protected MergeSegments expression;

    /**
     * 实体对象
     */
    protected Class<T> entityClass;

    /**
     * sql组装器
     */
    protected List<ISqlAssembler> sqlAssemblers;

    protected AbstractWrapper(MergeSegments expression, AtomicInteger paramNameSeq) {
        this.expression = expression;
        this.paramNameSeq = paramNameSeq;
        sqlAssemblers = new ArrayList<>();
    }

    public AbstractWrapper(MergeSegments expression, Class<T> entityClass) {
        this(expression, new AtomicInteger(0));
        this.entityClass = entityClass;
    }

    /**
     * 创建sql组装器
     * @param sqlAssembler
     *               -组装器具体实现
     * @return
     *               -返回自身
     */
    protected Children addAssembler(ISqlAssembler sqlAssembler) {
        if (sqlAssembler != null) {
            sqlAssemblers.add(sqlAssembler);
        }
        return typedThis;
    }

    /**
     * 获取属性名称
     * @param column
     *             -抽象的属性定义
     * @return
     *             -返回自身
     */
    protected abstract String columnToString(R column);

    /**
     * 子类返回一个自己的新对象
     * @return
     *              -返回自身
     */
    protected abstract Children instance();

    @Override
    public Children eq(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.EQ, val));
    }

    @Override
    public Children ne(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.NE, val));
    }

    @Override
    public Children gt(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.GT, val));
    }

    @Override
    public Children ge(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.GE, val));
    }

    @Override
    public Children lt(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.LT, val));
    }

    @Override
    public Children le(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.LE, val));
    }

    @Override
    public Children between(boolean condition, R column, Object val1, Object val2) {
        if (!validateFiledValue(val1) || !validateFiledValue(val2)) {
            return typedThis;
        }
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> {
                String entityFiledName = columnsToString(column);
                String val1Name = getParamName(entityFiledName);
                String val2Name = getParamName(entityFiledName);
                appendSqlSegments(() -> strategy.getColumnName(entityFiledName), SqlKeyword.BETWEEN, new ISqlSegment() {
                    @Override
                    public String getSqlSegment() {
                        return val1Name + StringPool.SPACE + SqlKeyword.AND.getSqlSegment() + StringPool.SPACE + val2Name;
                    }
                    @Override
                    public Map<String, Object> getSqlSegmentParamMap() {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put(val1Name, val1);
                        map.put(val2Name, val2);
                        return map;
                    }
                });
            });
        });
    }

    @Override
    public Children notBetween(boolean condition, R column, Object val1, Object val2) {
        if (!validateFiledValue(val1) || !validateFiledValue(val2)) {
            return typedThis;
        }
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> {
                String entityFiledName = columnsToString(column);
                String val1Name = getParamName(entityFiledName);
                String val2Name = getParamName(entityFiledName);
                appendSqlSegments(() -> strategy.getColumnName(entityFiledName), SqlKeyword.NOT_BETWEEN, new ISqlSegment() {
                    @Override
                    public String getSqlSegment() {
                        return val1Name + StringPool.SPACE + SqlKeyword.AND.getSqlSegment() + StringPool.SPACE + val2Name;
                    }
                    @Override
                    public Map<String, Object> getSqlSegmentParamMap() {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put(val1Name, val1);
                        map.put(val2Name, val2);
                        return map;
                    }
                });
            });
        });
    }

    @Override
    public Children like(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.LIKE, val));
    }

    @Override
    public Children notLike(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.LIKE, val));
    }

    @Override
    public Children likeLeft(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.LIKE_LEFT, val));
    }

    @Override
    public Children likeRight(boolean condition, R column, Object val) {
        if (!validateFiledValue(val)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.LIKE_RIGHT, val));
    }

    @Override
    public Children isNull(boolean condition, R column) {
        return addAssembler((strategy) -> maybeDo(condition, () -> {
            appendSqlSegments(() -> CompareEnum.IS_NULL.getMetaSql(null, columnToString(column)));
        }));
    }

    @Override
    public Children isNotNull(boolean condition, R column) {
        return addAssembler((strategy) -> maybeDo(condition, () -> {
            appendSqlSegments(() -> CompareEnum.IS_NOT_NULL.getMetaSql(null, columnToString(column)));
        }));
    }

    @Override
    public Children in(boolean condition, R column, Collection<?> coll) {
        if (!validateFiledValue(coll)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.IN, coll));
    }

    @Override
    public Children in(boolean condition, R column, Object... values) {
        if (!validateFiledValue(values)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.IN, values));
    }

    @Override
    public Children inb(boolean condition, Collection<R> columns, Collection<Object[]> values) {
        if (columns == null || columns.size() == 0 || values == null || values.size() == 0) {
            return typedThis;
        }
        return addAssembler((strategy) -> addBatchInCondition(strategy, condition, columns, values));
    }

    @Override
    public Children notIn(boolean condition, R column, Collection<?> coll) {
        if (!validateFiledValue(coll)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.NOT_IN, coll));
    }

    @Override
    public Children notIn(boolean condition, R column, Object... values) {
        if (!validateFiledValue(values)) {
            return typedThis;
        }
        return addAssembler((strategy) -> addNeedValCondition(strategy, condition, column, CompareEnum.NOT_IN, values));
    }

    @Override
    public Children groupBy(boolean condition, R column) {
        return addAssembler((strategy) -> {
            String filedName = columnToString(column);
            maybeDo(condition, () -> appendSqlSegments(SqlKeyword.GROUP_BY, () -> strategy.getColumnName(filedName)));
        });
    }

    @Override
    public Children groupBy(boolean condition, List<R> columns) {
        return addAssembler((strategy) -> {
            List<String> filedNames = columns.stream().map(this::columnToString).collect(Collectors.toList());
            maybeDo(condition, () -> appendSqlSegments(SqlKeyword.GROUP_BY, () -> strategy.getSplitColumnName(filedNames, StringPool.COMMA)));
        });
    }

    @Override
    public Children groupBy(boolean condition, R column, R... columns) {
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> {
                String one = strategy.getColumnName(columnToString(column));
                if (columns != null && columns.length > 0) {
                    one += (StringPool.COMMA + columnsToString(columns));
                }
                final String finalOne = one;
                appendSqlSegments(SqlKeyword.GROUP_BY, () -> finalOne);
            });
        });
    }

    @Override
    public Children orderBy(boolean condition, boolean isAsc, R column) {
        return addAssembler((strategy) -> {
            String filedName = columnsToString(columnSqlInjectFilter(column));
            maybeDo(condition, () -> appendSqlSegments(SqlKeyword.ORDER_BY, () -> strategy.getColumnName(filedName),
                    isAsc ? SqlKeyword.ASC : SqlKeyword.DESC));
        });
    }

    @Override
    public Children orderBy(boolean condition, boolean isAsc, List<R> columns) {
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> columns.forEach(c -> appendSqlSegments(SqlKeyword.ORDER_BY,
                    () -> strategy.getColumnName(columnsToString(columnSqlInjectFilter(c))), isAsc ? SqlKeyword.ASC : SqlKeyword.DESC)));
        });
    }

    @Override
    public Children orderBy(boolean condition, boolean isAsc, R column, R... columns) {
        return addAssembler((strategy) -> {
            final SqlKeyword mode = isAsc ? SqlKeyword.ASC : SqlKeyword.DESC;
            appendSqlSegments(SqlKeyword.ORDER_BY, () -> strategy.getColumnName(columnsToString(columnSqlInjectFilter(column))), mode);
            if (columns != null && columns.length > 0) {
                Arrays.stream(columns).forEach(c -> appendSqlSegments(SqlKeyword.ORDER_BY,
                        () -> strategy.getColumnName(columnsToString(columnSqlInjectFilter(c))), mode));
            }
        });
    }

    @Override
    public Children having(boolean condition, String sqlHaving, Map<String, Object> paramMap) {
        if (!validateFiledValue(paramMap)) {
            return typedThis;
        }
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> appendSqlSegments(SqlKeyword.HAVING, new ISqlSegment() {
                @Override
                public String getSqlSegment() {
                    return sqlHaving;
                }
                @Override
                public Map<String, Object> getSqlSegmentParamMap() {
                    return paramMap;
                }
            }));
        });
    }

    @Override
    public Children accept(boolean condition, Consumer<Children> consumer) {
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> consumer.accept(typedThis));
        });
    }

    @Override
    public Children or(boolean condition) {
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> appendSqlSegments(SqlKeyword.OR));
        });
    }

    @Override
    public Children last(boolean condition, String lastSql) {
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> appendSqlSegments(() -> lastSql));
        });
    }

    @Override
    public Children and(boolean condition) {
        return addAssembler((strategy) -> {
            maybeDo(condition, () -> appendSqlSegments(SqlKeyword.AND));
        });
    }

    @Override
    public Children and(boolean condition, Function<Children, Children> function) {
        return and(condition).addAssembler((strategy) -> {
            addNestedCondition(strategy, condition, function);
        });

    }

    @Override
    public Children or(boolean condition, Function<Children, Children> function) {
        return or(condition).addAssembler((strategy) -> {
            addNestedCondition(strategy,condition, function);
        });
    }

    @Override
    public Children nested(boolean condition, Function<Children, Children> function) {
        return addAssembler((strategy) -> {
            addNestedCondition(strategy, condition, function);
        });
    }

    @Override
    public Children not(boolean condition, Function<Children, Children> function) {
        return not(condition).addAssembler((strategy) -> {
            addNestedCondition(strategy,condition, function);
        });
    }

    /**
     * 内部自用
     * <p>NOT 关键词</p>
     * @param condition boolean
     * @return Children
     */
    protected Children not(boolean condition) {
        return addAssembler((mappingStrategy -> {
            maybeDo(condition, () -> appendSqlSegments(SqlKeyword.NOT));
        }));
    }

    @Override
    public String getSqlSegment() {
        return expression.getSqlSegment();
    }

    @Override
    public Map<String, Object> getSqlSegmentParamMap() {
        return expression.getSqlSegmentParamMap();
    }

    @Override
    public Class<T> entityClass() {
        return entityClass;
    }

    @Override
    public void assemble(FiledMappingStrategy mappingStrategy) {
        if (sqlAssemblers == null || sqlAssemblers.isEmpty()) {
            return;
        }
        for (ISqlAssembler sqlAssembler : sqlAssemblers) {
            sqlAssembler.assemble(mappingStrategy);
        }
        return;
    }


    protected String columnsToString(R... columns) {
        return Arrays.stream(columns).map(this::columnToString).collect(joining(StringPool.COMMA));
    }


    protected Children addNeedValCondition(FiledMappingStrategy mappingStrategy, boolean condition, R column, CompareEnum compareEnum, Object val) {
        return maybeDo(condition, () -> addSqlSegment(mappingStrategy, column, compareEnum, val));
    }

    private void addBatchInCondition(FiledMappingStrategy mappingStrategy, boolean condition, Collection<R> columns, Collection<Object[]> values) {
        List<String> fieldNames = columns.stream().map(this::columnToString).collect(Collectors.toList());
        List<String> columnNames = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            columnNames.add(mappingStrategy.getColumnName(fieldName));
            String paramName = getParamName(fieldName);
            paramNames.add(paramName);
            int finalI = i;
            paramMap.put(paramName, values.stream().map(e -> e[finalI]).collect(Collectors.toList()));
        }
        maybeDo(condition, () -> appendSqlSegments(new ISqlSegment() {
            @Override
            public String getSqlSegment() {
                return CompareEnum.IN_BATCH.getBatchMetaSql(paramNames, columnNames);
            }
            @Override
            public Map<String, Object> getSqlSegmentParamMap() {
                return paramMap;
            }
        }));
    }


    protected void addSqlSegment(FiledMappingStrategy mappingStrategy, R column, CompareEnum compareEnum, Object val) {
        boolean isAllow = FiledValueFilterStrategy.FiledValueFilterStrategyHolder.getInstance().validate(val);
        if (isAllow) {
            String entityFiledName = columnToString(column);
            final String paramName = getParamName(entityFiledName);
            String columnName = mappingStrategy.getColumnName(entityFiledName);
            SqlSegmentMeta sqlSegmentMeta = new SqlSegmentMeta();
            sqlSegmentMeta.setCompareEnum(compareEnum);
            sqlSegmentMeta.setEntityFiledName(entityFiledName);
            sqlSegmentMeta.setParamName(paramName);
            sqlSegmentMeta.setColumnName(columnName);
            sqlSegmentMeta.putPair(paramName, val);
            appendSqlSegments(sqlSegmentMeta);
        }
    }

    private boolean validateFiledValue(Object value) {
        return FiledValueFilterStrategy.FiledValueFilterStrategyHolder.getInstance().validate(value);
    }

    private String getParamName(String entityFiledName) {
        return entityFiledName + StringPool.WRAPPER_PARAM_MIDDLE + StringPool.WRAPPER_PARAM + paramNameSeq.incrementAndGet();
    }

    /**
     * 添加 where 片段
     *
     * @param sqlSegments ISqlSegment 数组
     */
    protected void appendSqlSegments(ISqlSegment... sqlSegments) {
        expression.add(sqlSegments);
    }


    protected final Children maybeDo(boolean condition, DoSomething something) {
        if (condition) {
            something.doIt();
        }
        return typedThis;
    }


    protected final ISqlSegment columnToSqlSegment(R column) {
        return () -> columnToString(column);
    }


    protected R columnSqlInjectFilter(R column) {
        return column;
    }


    protected Children addNestedCondition(FiledMappingStrategy mappingStrategy, boolean condition, Function<Children, Children> function) {
        return maybeDo(condition, () -> {
            final Children instance = instance();
            Children nested = function.apply(instance);
            //获取组装器组装sql
            nested.assemble(mappingStrategy);
            String sql = instance.getSqlSegment();
            Map<String, Object> paramMap = instance.getSqlSegmentParamMap();
            appendSqlSegments(new ISqlSegment() {
                @Override
                public String getSqlSegment() {
                    if (sql == null) {
                        return StringPool.EMPTY;
                    }
                    return StringPool.LEFT_BRACKET + sql + StringPool.RIGHT_BRACKET;
                }
                @Override
                public Map<String, Object> getSqlSegmentParamMap() {
                    return paramMap;
                }
            });
        });
    }


    @FunctionalInterface
    public interface DoSomething {
        void doIt();
    }
}
