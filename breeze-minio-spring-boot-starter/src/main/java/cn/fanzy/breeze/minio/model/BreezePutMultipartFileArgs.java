package cn.fanzy.breeze.minio.model;

import lombok.Data;

/**
 * 分片上传前端请求参数
 * @author fanzaiyang
 */
@Data
public class BreezePutMultipartFileArgs {
    /**
     * minio配置文件名
     */
    private String minioConfigName;
    /**
     * 合并后的文件存储桶名 称
     */
    private String bucketName;
    /**
     * 合并后的文件名称唯一
     */
    private String objectName;
    /**
     * 每个分片大小
     */
    private long chunkSize;
    /**
     * 分片总个数
     */
    private int totalChunks;
    /**
     * 文件唯一值MD5
     */
    private String identifier;

}