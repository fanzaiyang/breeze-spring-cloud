package cn.fanzy.breeze.admin.module.entity;

import cn.fanzy.breeze.sqltoy.model.IBaseEntity;
import cn.fanzy.breeze.sqltoy.utils.IdStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.sagacity.sqltoy.config.annotation.Column;
import org.sagacity.sqltoy.config.annotation.Entity;
import org.sagacity.sqltoy.config.annotation.Id;

import java.sql.Types;
import java.util.Date;
@Schema(description = "sys_log")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(tableName = "sys_log")
public class SysLog extends IBaseEntity {
    private static final long serialVersionUID = 6530809208568681351L;
    @Id(strategy = IdStrategy.GENERATOR, generator = IdStrategy.Generator.DEFAULT)
    @Column(name = "id", type = Types.VARCHAR)
    @Schema(description = "id")
    private String id;
    /**
     * 线程ID
     */
    @Column(name = "trace_id", type = Types.VARCHAR)
    private String traceId;
    /**
     * 业务名称
     */
    @Column(name = "name", type = Types.VARCHAR)
    @Schema(description = "业务名称")
    private String name;

    /**
     * IP地址
     */
    @Column(name = "client_ip", type = Types.VARCHAR)
    @Schema(description = "IP地址")
    private String clientIp;

    /**
     * 当前登录用户主键
     */
    @Column(name = "user_id", type = Types.VARCHAR)
    @Schema(description = "当前登录用户主键")
    private String userId;

    /**
     * 当前用户详情
     */
    @Column(name = "user_info", type = Types.VARCHAR)
    @Schema(description = "当前用户详情")
    private String userInfo;

    /**
     * 开始时间
     */
    @Column(name = "start_time", type = Types.DATE)
    @Schema(description = "开始时间")
    private Date startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time", type = Types.DATE)
    @Schema(description = "结束时间")
    private Date endTime;

    /**
     * 请求耗时（秒）
     */
    @Column(name = "spend_second", type = Types.INTEGER, length = 11)
    @Schema(description = "请求耗时（秒）")
    private Integer spendSecond;
    @Column(name = "request_uri", type = Types.VARCHAR)
    @Schema(description = "请求地址")
    private String requestUri;
    @Column(name = "request_method", type = Types.VARCHAR)
    @Schema(description = "请求方法GET/POST")
    private String requestMethod;
    /**
     * 请求参数JSON
     */
    @Column(name = "request_data", type = Types.LONGVARCHAR)
    @Schema(description = "请求参数JSON")
    private String requestData;

    /**
     * 响应结果JSON
     */
    @Column(name = "response_data", type = Types.LONGVARCHAR)
    @Schema(description = "响应结果JSON")
    private String responseData;

    /**
     * 是否成功，1-是，0-否
     */
    @Column(name = "success", type = Types.SMALLINT, length = 1)
    @Schema(description = "是否成功，1-是，0-否")
    private Integer success;
}