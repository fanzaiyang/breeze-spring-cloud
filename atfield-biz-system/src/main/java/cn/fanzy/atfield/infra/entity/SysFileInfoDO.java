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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @project spider-starter
 * @author fanzaiyang
 * @version 1.0.0 
 */
@Schema(description = "文件信息表")
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Entity(tableName = "sys_file_info", comment = "文件信息表", pk_constraint = "PRIMARY")
public class SysFileInfoDO extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 6507703710496375701L;
	/*---begin-auto-generate-don't-update-this-area--*/

	@Schema(description = "主键", nullable = false)
	@Id(strategy = "generator", generator = "org.sagacity.sqltoy.plugins.id.impl.SnowflakeIdGenerator")
	@Column(name = "id", comment = "主键", length = 32L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = false)
	private String id;

	@Schema(description = "文件的唯一标识identifier（md5摘要）", nullable = true)
	@Column(name = "identifier", comment = "文件的唯一标识identifier（md5摘要）", length = 64L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String identifier;

	@Schema(description = "MinIO上传ID", nullable = true)
	@Column(name = "upload_id", comment = "MinIO上传ID", length = 128L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String uploadId;

	@Schema(description = "文件名称", nullable = true)
	@Column(name = "file_name", comment = "文件名称", length = 900L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String fileName;

	@Schema(description = "存储桶host地址", nullable = true)
	@Column(name = "bucket_host", comment = "存储桶host地址", length = 900L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String bucketHost;

	@Schema(description = "存储桶名称", nullable = true)
	@Column(name = "bucket_name", comment = "存储桶名称", length = 90L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String bucketName;

	@Schema(description = "文件名称（唯一）", nullable = true)
	@Column(name = "object_name", comment = "文件名称（唯一）", length = 900L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String objectName;

	@Schema(description = "分片个数", nullable = true)
	@Column(name = "total_chunk_num", comment = "分片个数", length = 10L, type = java.sql.Types.INTEGER, nativeType = "INT", nullable = true)
	private Integer totalChunkNum;

	@Schema(description = "总文件大小;单位：bytes", nullable = true)
	@Column(name = "total_file_size", comment = "总文件大小;单位：bytes", length = 10L, type = java.sql.Types.INTEGER, nativeType = "INT", nullable = true)
	private Integer totalFileSize;

	@Schema(description = "每片按照多大分割", nullable = true)
	@Column(name = "chunk_size", comment = "每片按照多大分割", length = 10L, type = java.sql.Types.INTEGER, nativeType = "INT", nullable = true)
	private Integer chunkSize;

	@Schema(description = "上传开始时间", nullable = true)
	@Column(name = "begin_time", comment = "上传开始时间", length = 19L, type = java.sql.Types.DATE, nativeType = "DATETIME", nullable = true)
	private LocalDateTime beginTime;

	@Schema(description = "上传结束时间", nullable = true)
	@Column(name = "end_time", comment = "上传结束时间", length = 19L, type = java.sql.Types.DATE, nativeType = "DATETIME", nullable = true)
	private LocalDateTime endTime;

	@Schema(description = "后台上传耗时", nullable = true)
	@Column(name = "spend_second", comment = "后台上传耗时", length = 10L, scale = 2, type = java.sql.Types.DECIMAL, nativeType = "DECIMAL", nullable = true)
	private BigDecimal spendSecond;

	@Schema(description = "状态;1-有效，0-无效", nullable = true)
	@Column(name = "status", comment = "状态;1-有效，0-无效", length = 5L, defaultValue = "1", type = java.sql.Types.SMALLINT, nativeType = "SMALLINT", nullable = true)
	private Integer status;

	@Schema(description = "备注说明", nullable = true)
	@Column(name = "remarks", comment = "备注说明", length = 900L, type = java.sql.Types.VARCHAR, nativeType = "VARCHAR", nullable = true)
	private String remarks;

	/**
	 * default constructor
	 */
	public SysFileInfoDO() {
	}

	/**
	 * pk constructor
	 */
	public SysFileInfoDO(String id) {
		this.id = id;
	}
	/*---end-auto-generate-don't-update-this-area--*/
}