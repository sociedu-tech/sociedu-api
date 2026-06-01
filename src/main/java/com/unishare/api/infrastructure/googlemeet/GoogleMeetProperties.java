package com.unishare.api.infrastructure.googlemeet;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.google.meet")
public class GoogleMeetProperties {

    private boolean enabled = false;

    /** File JSON OAuth client (client_secret_*.apps.googleusercontent.com.json). */
    private String oauthClientPath;

    /** Redirect URI đã đăng ký trên Google Cloud — phải khớp chính xác. */
    private String oauthRedirectUri;

    /** Thư mục lưu refresh token sau khi mentor authorize (gitignore). */
    private String tokenStorePath = "./data/google-oauth-tokens";

    /** Calendar ID — thường là {@code primary} với OAuth user. */
    private String calendarId = "primary";

    private String timezone = "Asia/Ho_Chi_Minh";
}
