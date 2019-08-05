package com.cmy.hoteltmallgenie.interceptor;

import com.alibaba.fastjson.JSON;
import com.cmy.hoteltmallgenie.ErrorCode;
import com.cmy.hoteltmallgenie.Result;
import com.cmy.hoteltmallgenie.cache.OAuth2Cache;
import com.cmy.hoteltmallgenie.service.TokenExtractor;
import org.apache.oltu.oauth2.common.OAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author: codeyung  E-mail:yjc199308@gmail.com
 * @date: 2017/11/23.下午1:44
 */
public class OAuth2Interceptor implements HandlerInterceptor {

    @Autowired
    private OAuth2Cache oAuth2Cache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Result result = new Result();
        result.setError(ErrorCode.INVALID_ACCESS_TOKEN);
        try {
            String accessToken = TokenExtractor.extractToken(request);

            if (StringUtils.isEmpty(accessToken)) {
                // 如果不存在/过期了，返回未验证错误，需重新验证
                oAuthFaileResponse(response, result);
                return false;
            }

            //验证Access Token
            if (!oAuth2Cache.checkAccessToken(accessToken)) {
                // 如果不存在/过期了，返回未验证错误，需重新验证
                oAuthFaileResponse(response, result);
                return false;
            }
        } catch (Exception e) {
            System.out.println("错误");
            oAuthFaileResponse(response, result);
            return false;
        }
        return true;


    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }

    /**
     * oAuth认证失败时的输出
     *
     * @param res
     * @throws IOException
     */
    private void oAuthFaileResponse(HttpServletResponse res, Result result) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Type", "application/json; charset=utf-8");
        res.setCharacterEncoding("UTF-8");
        res.addHeader(OAuth.HeaderType.WWW_AUTHENTICATE, OAuth.HeaderType.WWW_AUTHENTICATE);
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PrintWriter writer = res.getWriter();
        writer.write(JSON.toJSONString(result));
        writer.flush();
        writer.close();
    }
}
