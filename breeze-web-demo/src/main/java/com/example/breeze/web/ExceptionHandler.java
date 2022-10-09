package com.example.breeze.web;

import cn.fanzy.breeze.web.exception.handler.BreezeExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExceptionHandler implements BreezeExceptionHandler{
    @Override
    public void after(Exception e) {
        log.info("异常后置处理器：{}-{}",e.getStackTrace()[0].getFileName(),e.getStackTrace()[0].getLineNumber());
    }
}
