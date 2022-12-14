package cn.fanzy.breeze.sqltoy.plus.conditions.segments;


import cn.fanzy.breeze.sqltoy.plus.conditions.ISqlSegment;
import cn.fanzy.breeze.sqltoy.plus.conditions.eumn.SqlKeyword;
import cn.fanzy.breeze.sqltoy.plus.conditions.toolkit.StringPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HavingSegmentList extends AbstractSegmentList{

    @Override
    public void merge() {
        List<Integer> havingIndexes = new ArrayList<>();
        List<ISqlSegment> mergeSqlSegments = new ArrayList<>();
        List<Integer> deleteIndexes = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < sqlSegments.size(); i++) {
            ISqlSegment sqlSegment = sqlSegments.get(i);
            if (MatchSegment.HAVING.match(sqlSegment)) {
                //第一个HAVING不删除
                if (count != 0) {
                    deleteIndexes.add(i);
                }
                count++;
                deleteIndexes.add(i + 1);
                havingIndexes.add(i);
                mergeSqlSegments.add(sqlSegments.get(i + 1));
            }
        }
        if (count > 1) {
            //删除多余元素
            deleteByIndex(deleteIndexes);
            //合并
            sqlSegments.add(havingIndexes.get(0) + 1, new ISqlSegment() {
                @Override
                public String getSqlSegment() {
                    return mergeSqlSegments.stream().filter(Objects::nonNull).map(ISqlSegment::getSqlSegment).collect(Collectors.joining(StringPool.SPACE + SqlKeyword.AND.getSqlSegment() + StringPool.SPACE));
                }

                @Override
                public Map<String, Object> getSqlSegmentParamMap() {
                    return mergeSqlSegments.stream().map(ISqlSegment::getSqlSegmentParamMap).filter(Objects::nonNull)
                            .flatMap(x -> x.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (p1, p2) -> p1));
                }
            });
        }
    }
}
