package cn.fanzy.infra.web.json.convert;

import cn.fanzy.infra.web.json.property.JsonProperty;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.io.Serial;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * spider jackson对象映射器
 *
 * @author fanzaiyang
 * @date 2023-04-24
 */
public class FrameworkJacksonObjectMapper extends ObjectMapper {
    @Serial
    private static final long serialVersionUID = 4349248944480408489L;

    public FrameworkJacksonObjectMapper(String jacksonDateFormat, String mvcDateFormat, String mvcTimeFormat,
                                        JsonProperty properties) {
        super();
        // 收到未知属性时不报异常
        this.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        //反序列化的时候如果多了其他属性,不抛出异常
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 日期禁止时间戳
        this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        //日期序列化
        javaTimeModule.addSerializer(Date.class, new DateSerializer(false, new SimpleDateFormat(StrUtil.blankToDefault(jacksonDateFormat, "yyyy-MM-dd HH:mm:ss"))));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(jacksonDateFormat)));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(mvcDateFormat)));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(mvcTimeFormat)));
        //日期反序列化
        javaTimeModule.addDeserializer(Date.class, new DateDeserializers.DateDeserializer());
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(jacksonDateFormat)));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(mvcDateFormat)));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(mvcTimeFormat)));
        this.setSerializerFactory(this.getSerializerFactory()
                .withSerializerModifier(new FrameworkBeanSerializerModifier(properties)));
        this.getSerializerProvider()
                .setNullValueSerializer(new FrameworkNullJsonSerializer.NullAnyJsonSerializer(properties.getConvert()));
        this.registerModules(simpleModule, javaTimeModule);
        String format = StrUtil.blankToDefault(jacksonDateFormat, mvcDateFormat);
        this.setDateFormat(new SimpleDateFormat(StrUtil.blankToDefault(format, "yyyy-MM-dd HH:mm:ss")));
    }

}
