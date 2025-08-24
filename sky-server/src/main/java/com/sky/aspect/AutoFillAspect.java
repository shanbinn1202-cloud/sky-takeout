package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){};


    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("start to autofill common segment");
        //the database operation type
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();
        //get the argument - entity object
        Object[] args = joinPoint.getArgs();
        if(args==null) return;

        Object arg = args[0];
        //set the value
        LocalDateTime time=LocalDateTime.now();
        Long id = BaseContext.getCurrentId();

        if(operationType==OperationType.INSERT){
            try {
                Method setCreateTime =  arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                setCreateTime.invoke(arg,time);
                setCreateUser.invoke(arg,id);
                setUpdateTime.invoke(arg,time);
                setUpdateUser.invoke(arg,id);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            try {

                Method setUpdateTime = arg.getClass().getDeclaredMethod("setUpdateTime",LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod("setUpdateUser",Long.class);

                setUpdateTime.invoke(arg,time);
                setUpdateUser.invoke(arg,id);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

}
