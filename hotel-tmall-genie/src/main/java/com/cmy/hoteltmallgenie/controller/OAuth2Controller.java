package com.cmy.hoteltmallgenie.controller;

import com.alibaba.fastjson.JSON;
import com.cmy.hoteltmallgenie.ErrorCode;
import com.cmy.hoteltmallgenie.Result;
import com.cmy.hoteltmallgenie.cache.OAuth2Cache;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Objects;

/**
 * 相关文档
 * http://www.ruanyifeng.com/blog/2014/05/oauth_2_0.html
 * https://cwiki.apache.org/confluence/display/OLTU/OAuth+2.0+Authorization+Server
 */
@Controller
@RequestMapping("oauth")
public class OAuth2Controller {

    @Value("${oauth2.client.client-id}")
    private String clientId;

    @Value("${oauth2.client.client-secret}")
    private String clientSecret;

    @Value("${oauth2.authorization.username}")
    private String authUsername;

    @Value("${oauth2.authorization.password}")
    private String authPassword;

    @Autowired
    OAuth2Cache oAuth2Cache;

    @RequestMapping(value = "authorize")
    public void authorize(HttpServletRequest req,
                            HttpServletResponse resp,Model model,
                            @RequestParam(value = "response_type", defaultValue = "") String response_type,
                            @RequestParam(value = "client_id", defaultValue = "") String client_id,
                            @RequestParam(value = "redirect_uri", defaultValue = "") String redirect_uri,
                            @RequestParam(value = "state", defaultValue = "") String state) throws IOException, ServletException {
        Result result = new Result();

        //responseType目前仅支持CODE，不支持TOKEN模式
        if (!ResponseType.CODE.toString().equals(response_type)) {
            result.setSuccess(false);
            result.setError(ErrorCode.RESPONSETYPE_ERROR);
            PrintWriter writer = resp.getWriter();
            writer.write(JSON.toJSONString(result));
            writer.flush();
            writer.close();
        }

        if (OAuthUtils.isEmpty(redirect_uri)) {
            //告诉客户端没有传入redirectUri直接报错
            result.setError(ErrorCode.INVALID_REDIRECT_URI);
            PrintWriter writer = resp.getWriter();
            writer.write(JSON.toJSONString(result));
            writer.flush();
            writer.close();
        }

        //检查提交的客户端id是否正确
        if (!Objects.equals(clientId, client_id)) {
            result.setError(ErrorCode.INVALID_CLIENT_ID);
            result.setSuccess(true);
            PrintWriter writer = resp.getWriter();
            writer.write(JSON.toJSONString(result));
            writer.flush();
            writer.close();
            //return OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
            //                .setError(OAuthError.TokenResponse.INVALID_CLIENT)
            //                .setErrorDescription(ErrorCode.INVALID_CLIENT_ID.getMessage())
            //                .buildJSONMessage();
        }

        //查看是否已经已有 authorizationCode
        String authorizationCode = oAuth2Cache.getAuthCodeByClientId(client_id);
        if (StringUtils.isEmpty(authorizationCode)) {
            //result.setSuccess(true);
            //result.put("redirectURI", redirect_uri + "?code=" + authorizationCode + "&state=" + state);
            //return result;

            //redirect_uri = resp.encodeRedirectURL(redirect_uri);
            if (!login(req)) {//登录失败时跳转到登陆页面
                model.addAttribute("clientId", clientId);
                req.getRequestDispatcher("/oauth2login").forward(req,resp);
                return;
            } else {
                //生成授权码
                OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
                try {
                    authorizationCode = oauthIssuerImpl.authorizationCode();
                } catch (OAuthSystemException e) {
                    e.printStackTrace();
                    result.setSuccess(false);
                    result.setError(ErrorCode.OAUT_CODE_FAIL);
                    PrintWriter writer = resp.getWriter();
                    writer.write(JSON.toJSONString(result));
                    writer.flush();
                    writer.close();
                }
            }
        }

        oAuth2Cache.addAuthCode(authorizationCode, client_id);
        //得到到客户端重定向地址
        //result.setSuccess(true);
        //result.put("redirectURI", redirect_uri + "?code=" + authorizationCode + "&state=" + state);
        //return result;

        if (redirect_uri.contains("?")) {
            redirect_uri = String.format("%s&code=%s&state=%s", redirect_uri, authorizationCode, state);
        } else {
            String decodeRedirectUrl = URLDecoder.decode(redirect_uri, "UTF-8");
            if (decodeRedirectUrl.contains("?")) {
                redirect_uri = String.format("%s&code=%s&state=%s", decodeRedirectUrl, authorizationCode, state);
            } else {
                redirect_uri = String.format("%s?code=%s&state=%s", redirect_uri, authorizationCode, state);
            }
        }
        resp.sendRedirect(redirect_uri);
    }


    @PostMapping(value = {"token"})
    @ResponseBody
    public Result token(HttpServletRequest request,
                        @RequestParam(value = "grant_type", defaultValue = "") String grant_type,
                        @RequestParam(value = "client_id", defaultValue = "") String client_id,
                        @RequestParam(value = "client_secret", defaultValue = "") String client_secret,
                        @RequestParam(value = "code", defaultValue = "") String code,
                        @RequestParam(value = "state", defaultValue = "") String state,
                        @RequestParam(value = "redirect_uri", defaultValue = "") String redirect_uri,
                        @RequestParam(value = "refresh_token", defaultValue = "") String refresh_token) {
        Result result = new Result();

        if (grant_type.equals(GrantType.AUTHORIZATION_CODE.toString())) {
            // 检查验证类型，此处只检查AUTHORIZATION_CODE类型
            result = checkAuthCode(code, client_id);
            if ((Boolean) result.get("success") == false) {
                return result;
            }
        } else if (grant_type.equals(GrantType.REFRESH_TOKEN.toString())) {
            // 检查验证类型，此处只检查REFRESH_TOKEN类型
            result = checkRefreshToken(refresh_token);

            if ((Boolean) result.get("success") == false) {
                return result;
            } else {
                client_id = oAuth2Cache.getClientIdByRefreshToken(refresh_token);
                String accessToken = oAuth2Cache.updateAccessToken(client_id);
                if (!StringUtils.isEmpty(accessToken)) {
                    result.put("access_token", accessToken);
                    result.put("token_type", "bearer");
                    result.put("refresh_token", refresh_token);
                    result.put("expires_in", oAuth2Cache.getExpireIn(accessToken));
                    return result;
                }
            }

        } else {
            //不支持的类型
            result.setSuccess(false);
            result.setError(ErrorCode.RESPONSETYPE_ERROR);
            return result;
        }

        //查看是否已经已有 Access Token
        result = checkExistAccessToken(client_id);
        if ((Boolean) result.get("success") == true) {
            return result;
        }

        //生成Access Token
        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        String accessToken = null;
        try {
            accessToken = oauthIssuerImpl.accessToken();
        } catch (OAuthSystemException e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setError(ErrorCode.ACCESS_TOKEN_FAIL);
            return result;
        }

        //绑定Access Token
        long expireIn = oAuth2Cache.addAccessToken(accessToken, client_id);
        result.put("access_token", accessToken);
        result.put("token_type", "bearer");

        //查看是否已经已有 RefreshToken
        String refreshToken = oAuth2Cache.getRefreshTokenByClientId(client_id);
        if (StringUtils.isEmpty(refreshToken)) {
            //生成RefreshToken
            try {
                refreshToken = oauthIssuerImpl.accessToken();
            } catch (OAuthSystemException e) {
                e.printStackTrace();
                result.setSuccess(false);
                result.setError(ErrorCode.REFRESH_TOKEN_FAIL);
                return result;
            }
            //绑定RefreshToken
            oAuth2Cache.addRefreshToken(refreshToken, client_id);
        }

        result.put("refresh_token", refreshToken);
        result.put("expires_in", expireIn);
        result.setSuccess(true);
        return result;

    }


    @PostMapping(value = "check")
    @ResponseBody
    public Result check(@RequestParam("access_token") String accessToken) {
        Result result = new Result();
        if (!oAuth2Cache.checkAccessToken(accessToken)) {
            result.setError(ErrorCode.INVALID_ACCESS_TOKEN);
            return result;
        }
        result.put("expires_in", oAuth2Cache.getExpireIn(accessToken));
        result.setSuccess(true);
        return result;
    }

    /**
     * 检查验证类型，此处只检查AUTHORIZATION_CODE类型
     *
     * @param authCode
     * @param client_id
     * @return
     */
    Result checkAuthCode(String authCode, String client_id) {
        Result result = new Result();
        if (!oAuth2Cache.checkAuthCode(authCode, client_id)) {
            result.setError(ErrorCode.INVALID_AUTH_CODE);
            return result;
        }
        result.setSuccess(true);
        return result;
    }

    /**
     * 检查验证类型，此处只检查REFRESH_TOKEN类型
     *
     * @param refresh_token
     * @return
     */
    Result checkRefreshToken(String refresh_token) {
        Result result = new Result();
        if (!oAuth2Cache.checkRefreshToken(refresh_token)) {
            result.setError(ErrorCode.INVALID_REFRESH_TOKEN);
            return result;
        }
        result.setSuccess(true);
        return result;
    }

    /**
     * 查看是否已经已有 Access Token
     *
     * @param client_id
     * @return
     */
    Result checkExistAccessToken(String client_id) {
        Result result = new Result();
        String accessToken = oAuth2Cache.getAccessTokenByClientId(client_id);
        String refresh_token = oAuth2Cache.getRefreshTokenByClientId(client_id);
        if (!StringUtils.isEmpty(accessToken)) {
            result.setSuccess(true);
            result.put("access_token", accessToken);
            result.put("token_type", "bearer");
            result.put("expires_in", oAuth2Cache.getExpireIn(accessToken));
            result.put("refresh_token", refresh_token);
            return result;
        }
        return result;
    }

    private boolean login(HttpServletRequest request) {
        if ("get".equalsIgnoreCase(request.getMethod())) {
            request.setAttribute("error", "非法的请求");
            return false;
        }
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            request.setAttribute("error", "登录失败:用户名或密码不能为空");
            return false;
        }
        try {
            // 写登录逻辑
            if (Objects.equals(username,authUsername)&&Objects.equals(password,authPassword)) {
                return true;
            } else {
                request.setAttribute("error", "登录失败:用户名不正确");
                return false;
            }
        } catch (Exception e) {
            request.setAttribute("error", "登录失败:" + e.getClass().getName());
            return false;
        }
    }
}
