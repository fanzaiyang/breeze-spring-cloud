package cn.fanzy.breeze.core.html2img.table;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author wyq
 * @Date 2021/8/26 15:00
 * @Version 1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HtmlTh {
    //字段中文名
    String name() default "";

    //标题 th 的样式
    String thStyle() default "border:solid #add9c0; border-width:0px 1px 1px 0px;padding:4px;text-align:center;";
    //td 的样式
    String tdStyle() default "border:solid #add9c0; border-width:0px 1px 1px 0px;padding:4px;text-align:center;";
}
