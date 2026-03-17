package com.example.seckill.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(0)
public class ReadOnlyDataSourceAspect {

    @Around("@annotation(com.example.seckill.datasource.ReadOnlyDataSource) || @within(com.example.seckill.datasource.ReadOnlyDataSource)")
    public Object routeToRead(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.set(DataSourceType.READ);
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
