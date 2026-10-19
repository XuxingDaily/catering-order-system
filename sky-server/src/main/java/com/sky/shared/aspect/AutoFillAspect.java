package com.sky.shared.aspect;

import com.sky.shared.annotation.AutoFill;
import com.sky.shared.constant.AutoFillConstant;
import com.sky.shared.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Aspect
@Component
public class AutoFillAspect {

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.shared.annotation.AutoFill)")
    public void pointcut() {}

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = methodSignature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = Objects.requireNonNull(autoFill).value();

        // 获取输入参数
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        // 默认第一个参数为具体的对象
        Object param = args[0];

        // 获取当前所需的输入参数
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 获取对应的 setter 方法
        try {
            Method setUpdateTime = param.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            setUpdateTime.invoke(param, now);

            Method setUpdateUser = param.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
            setUpdateUser.invoke(param, currentId);

            if (operationType == OperationType.INSERT) {
                Method setCreateTime = param.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                setCreateTime.invoke(param, now);

                Method setCreateUser = param.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                setCreateUser.invoke(param, currentId);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("AutoFillAspect 面向的对象第一个参数缺乏 setter 方法 e: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
