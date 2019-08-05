package com.cmy.hoteltmallgenie.cache;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.netty.util.internal.StringUtil;
import org.apache.oltu.oauth2.common.token.BasicOAuthToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: codeyung  E-mail:yjc199308@gmail.com
 * @date: 2017/11/29.下午5:22
 */
@Repository
public class OAuth2Cache {
    @Value("${oauth2.client.access-token-validity-seconds:}")
    private int accessTokenExpire;

    @Value("${oauth2.client.refresh-token-validity-seconds:}")
    private int refreshTokenExpire;

    private Cache<String, String> accessTokenCache;
    private Cache<String, String> refreshTokenCache;
    private Cache<String, String> authorizationCodeCache;

    @PostConstruct
    public void init() {
        accessTokenCache = CacheBuilder.newBuilder()
                .concurrencyLevel(20)
                .initialCapacity(1400)
                .ticker(Ticker.systemTicker())
                .expireAfterWrite(accessTokenExpire, TimeUnit.SECONDS)
                .build();
        refreshTokenCache = CacheBuilder.newBuilder()
                .concurrencyLevel(20)
                .initialCapacity(1400)
                .ticker(Ticker.systemTicker())
                .expireAfterWrite(refreshTokenExpire, TimeUnit.SECONDS)
                .build();
        authorizationCodeCache = CacheBuilder.newBuilder()
                .concurrencyLevel(5)
                .initialCapacity(700)
                .ticker(Ticker.systemTicker())
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .build();
    }

    //给响应用户添加 accessToken
    public long addAccessToken(String accessToken, String client_id) {
        long beforeAdd = System.currentTimeMillis();
        accessTokenCache.put(accessToken, beforeAdd + "");
        accessTokenCache.put(client_id, accessToken);
        return accessTokenExpire - (System.currentTimeMillis() - beforeAdd)/1000;
        //redisService.set("access_token_" + accessToken, client_id, 3600);
        //redisService.set("access_token_client_id_" + client_id, accessToken, 3660);
    }

    // 更新 access_token
    public String updateAccessToken(String client_id) {
        String accessToken = this.getAccessTokenByClientId(client_id);
        if (StringUtils.isEmpty(accessToken)) {
            return "";
        }
        accessTokenCache.put(accessToken, System.currentTimeMillis() + "");
        accessTokenCache.put(client_id, accessToken);
        //redisService.set("access_token_" + accessToken, client_id, 3600);
        //redisService.set("access_token_client_id_" + client_id, accessToken, 3600);
        return accessToken;
    }

    //给响应用户添加 refreshToken
    public void addRefreshToken(String refreshToken, String client_id) {
        refreshTokenCache.put(refreshToken, client_id);
        refreshTokenCache.put(client_id, refreshToken);
        //redisService.set("refresh_token_" + refreshToken, client_id, 3600 * 30);
        //redisService.set("refresh_client_id_" + client_id, refreshToken, 3660 * 30);
    }

    //验证 token 是否存在
    public boolean checkAccessToken(String accessToken) {
        String createTime = accessTokenCache.getIfPresent(accessToken);
        return !StringUtils.isEmpty(createTime) && isNumeric(createTime) && System.currentTimeMillis() < Long.valueOf(createTime) + accessTokenExpire * 1000;
        //return redisService.hasKey("access_token_" + accessToken);
    }

    //auth code / access token 过期时间
    public long getExpireIn(String accessToken) {
        String createTimeStr = accessTokenCache.getIfPresent(accessToken);
        return StringUtils.isEmpty(createTimeStr) || !isNumeric(createTimeStr) ? 0L : accessTokenExpire - (System.currentTimeMillis() - Long.valueOf(createTimeStr))/1000;
        //return redisService.getExpire("access_token_" + accessToken);
    }

    //根据 client_id 获取 accessToken
    public String getAccessTokenByClientId(String client_id) {
        return accessTokenCache.getIfPresent(client_id);
        //return redisService.get("access_token_client_id_" + client_id);
    }

    //验证refresh_token是否存在
    public boolean checkRefreshToken(String refresh_token) {
        return !StringUtils.isEmpty(getClientIdByRefreshToken(refresh_token));
        //return redisService.hasKey("refresh_token_" + refresh_token);
    }

    //根据 refresh_token 获取 clientId
    public String getClientIdByRefreshToken(String refresh_token) {
        return refreshTokenCache.getIfPresent(refresh_token);
        //return redisService.get("refresh_token_" + refresh_token);
    }

    //根据 client_id 获取 refresh_token
    public String getRefreshTokenByClientId(String client_id) {
        return refreshTokenCache.getIfPresent(client_id);
        //return redisService.get("refresh_client_id_" + client_id);
    }

    // 检查code 是否存在
    public boolean checkAuthCode(String code, String client_id) {
        String authCode = authorizationCodeCache.getIfPresent(client_id);
        //String authCode = redisService.get("authorization_code_" + client_id);

        if (StringUtils.isEmpty(authCode) || !authCode.equals(code)) {
            return false;
        }
        //redisService.del("authorization_code_" + client_id);
        authorizationCodeCache.invalidate(client_id);
        return true;
    }

    //根据 client_id 获取 AuthCode
    public String getAuthCodeByClientId(String client_id) {
        return authorizationCodeCache.getIfPresent(client_id);
        //return redisService.get("authorization_code_" + client_id);
    }

    //code 授权码跟用户绑定
    public void addAuthCode(String code, String client_id) {
        //redisService.set("authorization_code_" + client_id, code, 600);
        authorizationCodeCache.put(client_id, code);
    }

    public static boolean isNumeric(CharSequence cs) {
        if (StringUtils.isEmpty(cs)) {
            return false;
        } else {
            int sz = cs.length();

            for (int i = 0; i < sz; ++i) {
                if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }
}
