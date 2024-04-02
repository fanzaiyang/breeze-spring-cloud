/**
 *@Generated by sagacity-quickvo 5.0
 */
package cn.fanzy.atfield.infra.entity;

import cn.fanzy.atfield.sqltoy.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.sagacity.sqltoy.config.annotation.Column;
import org.sagacity.sqltoy.config.annotation.Entity;
import org.sagacity.sqltoy.config.annotation.Id;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @project spider-starter
 * @author fanzaiyang
 * @version 1.0.0 
 */
@Schema(description = "系统用户信息表")
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Entity(tableName = "ums_admin_info", comment = "系统用户信息表", pk_constraint = "PRIMARY")
public class UmsAdminInfoDO extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 4522576635101648332L;
	/*---begin-auto-generate-don't-update-this-area--*/

	@Schema(description = "主键", nullable = false)
	@Id(strategy = "generator", generator = "org.sagacity.sqltoy.plugins.id.impl.SnowflakeIdGenerator")
	@Column(name = "id", comment = "主键", length = 32L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = false)
	private String id;

	@Schema(description = "头像地址", nullable = true)
	@Column(name = "avatar", comment = "头像地址", length = 255L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String avatar;

	@Schema(description = "登录名", nullable = true)
	@Column(name = "username", comment = "登录名", length = 36L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String username;

	@Schema(description = "登录密码", nullable = true)
	@Column(name = "password", comment = "登录密码", length = 90L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String password;

	@Schema(description = "用户昵称", nullable = true)
	@Column(name = "nick_name", comment = "用户昵称", length = 90L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String nickName;

	@Schema(description = "用户类型;关联字典值", nullable = true)
	@Column(name = "user_type", comment = "用户类型;关联字典值", length = 32L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String userType;

	@Schema(description = "电子邮箱", nullable = true)
	@Column(name = "email", comment = "电子邮箱", length = 90L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String email;

	@Schema(description = "手机号", nullable = true)
	@Column(name = "mobile", comment = "手机号", length = 20L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String mobile;

	@Schema(description = "工作手机号", nullable = true)
	@Column(name = "work_mobile", comment = "工作手机号", length = 20L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String workMobile;

	@Schema(description = "登录IP", nullable = true)
	@Column(name = "login_ip", comment = "登录IP", length = 90L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String loginIp;

	@Schema(description = "登录设备", nullable = true)
	@Column(name = "login_device", comment = "登录设备", length = 90L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String loginDevice;

	@Schema(description = "登录时间", nullable = true)
	@Column(name = "login_time", comment = "登录时间", length = 19L, type = java.sql.Types.DATE, nativeType = "DATETIME", nullable = true)
	private LocalDateTime loginTime;

	@Schema(description = "性别;1-男，2-女，0-未知", nullable = true)
	@Column(name = "sex", comment = "性别;1-男，2-女，0-未知", length = 5L, type = java.sql.Types.SMALLINT, nativeType = "SMALLINT", nullable = true)
	private Integer sex;

	@Schema(description = "备注说明", nullable = true)
	@Column(name = "remarks", comment = "备注说明", length = 900L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String remarks;

	@Schema(description = "状态;1-有效，0-无效", nullable = true)
	@Column(name = "status", comment = "状态;1-有效，0-无效", length = 5L, defaultValue = "1", type = java.sql.Types.SMALLINT, nativeType = "SMALLINT", nullable = true)
	private Integer status;

	/**
	 * default constructor
	 */
	public UmsAdminInfoDO() {
	}

	/**
	 * pk constructor
	 */
	public UmsAdminInfoDO(String id) {
		this.id = id;
	}
	/*---end-auto-generate-don't-update-this-area--*/
}
