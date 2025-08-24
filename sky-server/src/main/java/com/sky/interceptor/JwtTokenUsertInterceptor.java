package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenUsertInterceptor implements HandlerInterceptor {


    @Autowired
    private JwtProperties jwtProperties;


    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse response, Object handler) throws  Exception{
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        try{
            String token = httpServletRequest.getHeader(jwtProperties.getUserTokenName());
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(),token);
            Long userId = Long.valueOf( claims.get(JwtClaimsConstant.USER_ID).toString());
            BaseContext.setCurrentId(userId);
            log.info("当前user id：{}", userId);
            return true;
        }catch (Exception e){
            response.setStatus(401);
            return false;
        }

    }
}
