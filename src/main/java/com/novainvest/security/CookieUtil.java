package com.novainvest.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final JwtService jwtService;

    public CookieUtil(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie access = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(jwtService.getAccessTokenMaxAgeSeconds())
                .build();

        ResponseCookie refresh = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(jwtService.getRefreshTokenMaxAgeSeconds())
                .build();

        response.addHeader("Set-Cookie", access.toString());
        response.addHeader("Set-Cookie", refresh.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie access = ResponseCookie.from("access_token", "")
                .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
        ResponseCookie refresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();

        response.addHeader("Set-Cookie", access.toString());
        response.addHeader("Set-Cookie", refresh.toString());
    }
}
