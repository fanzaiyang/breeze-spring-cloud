package cn.fanzy.breeze.minio.model;

/**
 * 政策声明
 *
 * @author fanzaiyang
 * @date 2023/11/13
 */
public class PolicyStatement {

    /**
     * 效应
     *
     * @author fanzaiyang
     * @date 2023/11/13
     */
    public static enum Effect {
        /**
         * 允许
         */
        Allow,
        /**
         * 否认
         */
        Deny;
    }
    public static enum Action {
        /**
         * 允许
         */
        Allow,
        /**
         * 否认
         */
        Deny;
    }
}
