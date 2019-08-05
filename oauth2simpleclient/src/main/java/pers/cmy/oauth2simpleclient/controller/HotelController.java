package pers.cmy.oauth2simpleclient.controller;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>@author chenmingyi
 * <p>@version 1.0
 * <p>date: 2019/8/5
 */
@Controller
@RequestMapping("hotel")
public class HotelController {
    private String accessToken = "";
    private String refreshToken = "";
    private int expireIn;

    @RequestMapping(value = "controlPage")
    public String controlPage(Model model) {
        model.addAttribute("token", accessToken);
        return "controlPage";
    }

    @RequestMapping(value = "apply4TokenPage")
    public String controlPage() {
        return "apply4TokenPage";
    }

    @RequestMapping(value = "control")
    public String control(String hotelId, String roomName, String skillId, HttpServletRequest req, HttpServletResponse resp) throws UnirestException, ServletException, IOException {
        HttpResponse<String> asString = Unirest.post("http://localhost:8011/hotel/control")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(String.format("hotelId=%s&roomName=%s&skillId=%s", hotelId, roomName, skillId))
                .asString();
        System.out.println(asString.getBody());
        if (asString.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            return "apply4TokenPage";
        }
        return "controlPage";
    }

    @RequestMapping(value = "apply4Token")
    public String apply4Token(String code, HttpServletResponse resp) {
        HttpResponse<String> response = null;
        try {
            response = Unirest.post("http://localhost:8011/oauth/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("cache-control", "no-cache")
                    .body(String.format("grant_type=%s&client_id=%s&client_secret=%s&code=%s","authorization_code","hotel-client","hotel-secret",code))
                    .asString();
            System.out.println(response.getBody());
            AccessToken token = new Gson().fromJson(response.getBody(), AccessToken.class);
            accessToken = token.access_token;
            refreshToken = token.refresh_token;
            expireIn = token.expires_in;
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
        return "controlPage";
    }

    private static class AccessToken {
        private String access_token;
        private String token_type;
        private String refresh_token;
        private int expires_in;
        private String scope;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        public String getToken_type() {
            return token_type;
        }

        public void setToken_type(String token_type) {
            this.token_type = token_type;
        }

        public String getRefresh_token() {
            return refresh_token;
        }

        public void setRefresh_token(String refresh_token) {
            this.refresh_token = refresh_token;
        }

        public int getExpires_in() {
            return expires_in;
        }

        public void setExpires_in(int expires_in) {
            this.expires_in = expires_in;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
