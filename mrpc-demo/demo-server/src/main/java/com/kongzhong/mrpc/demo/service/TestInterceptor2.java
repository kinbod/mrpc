package com.kongzhong.mrpc.demo.service;

import com.kongzhong.mrpc.interceptor.RpcServerInterceptor;
import com.kongzhong.mrpc.interceptor.ServerInvocation;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author biezhi
 *         2017/4/24
 */
@Slf4j
public class TestInterceptor2 implements RpcServerInterceptor {

    @Override
    public Object execute(ServerInvocation invocation) throws Throwable {
        log.info("test interceptor2 execute.");
        Object obj = invocation.next();
        log.info("test interceptor2 execute, sleep 3s.");
        TimeUnit.SECONDS.sleep(3);
        return obj;
    }
}
