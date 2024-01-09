package cn.fanzy.atfield.sqltoy.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.sagacity.sqltoy.config.annotation.Column;
import org.sagacity.sqltoy.config.annotation.DataVersion;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基本实体
 *
 * @author fanzaiyang
 * @date 2023/12/14
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -2668694369032220300L;

    /**
     * 删除标志;1-已删除，0-未删除
     */
    @Column(name = "del_flag", comment = "删除标志;1-已删除，0-未删除", length = 5L, defaultValue = "0", type = java.sql.Types.SMALLINT, nativeType = "SMALLINT", nullable = true)
    private Integer delFlag;

    /**
     * 租户 ID
     */
    @Column(name = "tenant_id", comment = "租户号", length = 36L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
    private String tenantId;

    /**
     * 数据版本
     */
    @DataVersion(startDate = true)
    @Column(name = "revision", comment = "乐观锁", length = 10L, type = java.sql.Types.INTEGER, nativeType = "INT", nullable = true)
    private Integer revision;

    /**
     * 创建者
     */
    @Column(name = "create_by", comment = "创建人", length = 36L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
    private String createBy;

    /**
     * 创建时间
     */
    @Column(name = "create_time", comment = "创建时间", length = 19L, type = java.sql.Types.DATE, nativeType = "DATETIME", nullable = true)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @Column(name = "update_by", comment = "更新人", length = 36L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
    private String updateBy;

    /**
     * 更新时间
     */
    @Column(name = "update_time", comment = "更新时间", length = 19L, type = java.sql.Types.DATE, nativeType = "DATETIME", nullable = true)
    private LocalDateTime updateTime;
}
