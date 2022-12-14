package cn.fanzy.breeze.sqltoy.plus.dao;

import cn.fanzy.breeze.sqltoy.plus.conditions.Wrapper;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.sagacity.sqltoy.config.model.EntityMeta;
import org.sagacity.sqltoy.dao.impl.SqlToyLazyDaoImpl;
import org.sagacity.sqltoy.exception.DataAccessException;
import org.sagacity.sqltoy.model.EntityQuery;
import org.sagacity.sqltoy.model.EntityUpdate;
import org.sagacity.sqltoy.model.Page;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * sqltoy
 *
 * @author fanzaiyang
 */
@Slf4j
public class SqlToyHelperDaoImpl extends SqlToyLazyDaoImpl implements SqlToyHelperDao {


    public <T> T findOne(Wrapper<T> wrapper) {
        List<T> rs = findList(wrapper);
        if (rs == null || rs.isEmpty()) {
            return null;
        }
        return rs.get(0);
    }


    public <T> List<T> findList(Wrapper<T> wrapper) {
        return super.findEntity(wrapper.entityClass(), getEntityQuery(wrapper));
    }


    public <T> Page<T> findPage(Wrapper<T> wrapper, Page<?> page) {
        return super.findPageEntity(page, wrapper.entityClass(), getEntityQuery(wrapper));
    }


    public <T> long delete(Wrapper<T> wrapper) {
        return super.deleteByQuery(wrapper.entityClass(), getEntityQuery(wrapper));
    }


    public <T> long count(Wrapper<T> wrapper) {
        return super.getCount(wrapper.entityClass(), getEntityQuery(wrapper));
    }


    public <T> long update(Wrapper<T> updateWrapper) {
        return super.updateByQuery(updateWrapper.entityClass(), getEntityUpdate(updateWrapper));
    }


    public <T> long update(T t, Wrapper<T> wrapper) {
        return super.updateByQuery(wrapper.entityClass(), getEntityUpdate(t, wrapper));
    }


    public <T> long update(Map<String, Object> setMap, Wrapper<T> wrapper) {
        return super.updateByQuery(wrapper.entityClass(), getEntityUpdate(setMap, wrapper));
    }


    private <T> EntityQuery getEntityQuery(Wrapper<T> wrapper) {
        EntityMeta entityMeta = super.getEntityMeta(wrapper.entityClass());
        //????????????sql
        wrapper.assemble(entityMeta::getColumnName);
        Map<String, Object> paramMap = wrapper.getSqlSegmentParamMap();
        EntityQuery entityQuery = EntityQuery.create().where(wrapper.getSqlSegment());
        if (paramMap != null && !paramMap.isEmpty()) {
            entityQuery.values(wrapper.getSqlSegmentParamMap());
        }
        if (wrapper.getSelectColumns() != null && !wrapper.getSelectColumns().isEmpty()) {
            entityQuery.select(wrapper.getSelectColumns().toArray(new String[0]));
        }
        return entityQuery;
    }

    /**
     * ??????????????????
     *
     * @param updateWrapper - ????????????
     * @param <T> T
     * @return EntityUpdate
     */
    private <T> EntityUpdate getEntityUpdate(Wrapper<T> updateWrapper) {
        Map<String, Object> setMap = updateWrapper.getSetMap();
        return getEntityUpdate(setMap, updateWrapper);
    }

    /**
     * @param t                - ???????????????
     * @param queryWrapper     - ?????????????????????
     * @param forceUpdateProps - ????????????
     * @param <T> T
     * @return EntityUpdate
     */
    private <T> EntityUpdate getEntityUpdate(T t, Wrapper<T> queryWrapper, String... forceUpdateProps) {
        EntityUpdate entityUpdate = EntityUpdate.create();
        EntityMeta entityMeta = super.getEntityMeta(queryWrapper.entityClass());
        Iterator<Map.Entry<String, String>> iterator = entityMeta.getColumnFieldMap().entrySet().iterator();
        Map<String, Object> setParamMap = objectToObjectMap(t, forceUpdateProps);
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            Object value = setParamMap.get(entry.getKey());
            if (value != null) {
                entityUpdate.set(entry.getKey(), value);
            }
        }
        return getEntityUpdate(setParamMap, queryWrapper);
    }

    /**
     * ??????????????????
     *
     * @param queryWrapper - ?????????????????????
     * @param <T> T
     * @return EntityUpdate
     */
    private <T> EntityUpdate getEntityUpdate(Map<String, Object> setMap, Wrapper<T> queryWrapper) {
        if (setMap == null || setMap.size() == 0) {
            throw new DataAccessException("sqlToy plus update param can not empty");
        }
        EntityMeta entityMeta = super.getEntityMeta(queryWrapper.entityClass());
        //????????????sql
        queryWrapper.assemble(entityMeta::getColumnName);
        EntityUpdate entityUpdate = EntityUpdate.create();
        setMap.forEach(entityUpdate::set);
        String sqlSegment = queryWrapper.getSqlSegment();
        Map<String, Object> paramMap = queryWrapper.getSqlSegmentParamMap();
        // ???where?????????????????????1=1?????????IllegalArgumentException??????
        if (paramMap.isEmpty()) {
            sqlSegment = StrUtil.isBlank(sqlSegment) ? "1=:DEFAULT_VALUE " : ("1=:DEFAULT_VALUE AND " + sqlSegment);
            paramMap.put("DEFAULT_VALUE", 1);
        }
        return entityUpdate.where(sqlSegment).values(paramMap);
    }

    /**
     * ???????????????map
     *
     * @param object Object
     * @param ignore String[]
     * @return HashMap
     */
    public static HashMap<String, Object> objectToObjectMap(Object object, String... ignore) {
        if (object == null) {
            return null;
        }
        HashMap<String, Object> tempMap = new LinkedHashMap<String, Object>();
        for (Class<?> clazz = object.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Field[] fields = clazz.getDeclaredFields();
                if (fields != null && fields.length > 0) {
                    for (Field field : fields) {
                        field.setAccessible(true);
                        // ???????????????
                        if (Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        boolean ig = false;
                        if (ignore != null && ignore.length > 0) {
                            for (String i : ignore) {
                                if (i.equals(field.getName())) {
                                    ig = true;
                                    break;
                                }
                            }
                        }
                        if (ig) {
                            continue;
                        }
                        Object o = null;
                        try {
                            o = field.get(object);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                        if (o != null) {
                            tempMap.put(field.getName(), o);
                        }
                    }
                }
            } catch (Exception e) {
                throw new DataAccessException("sqlToy plus entity to map occur exception");
            }
        }
        return tempMap;
    }
}
