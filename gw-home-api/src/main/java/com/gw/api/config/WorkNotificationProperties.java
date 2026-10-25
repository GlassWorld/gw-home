package com.gw.api.config;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix="work.notifications")
public class WorkNotificationProperties {
    public enum SmtpAuthMode { PASSWORD, MICROSOFT365_OAUTH2 }
    private boolean enabled;
    private SmtpAuthMode smtpAuthMode=SmtpAuthMode.PASSWORD;
    private String smtpHost="";
    private int smtpPort=587;
    private String smtpUsername="";
    private String smtpPassword="";
    private boolean starttls=true;
    private boolean ssl;
    private String from="";
    private String fromName="";
    private String appUrl="";
    private String microsoftTenantId="";
    private String microsoftClientId="";
    private String microsoftClientSecret="";

    // 임의 토큰 서버 경로를 받지 않고 등록된 테넌트·앱 식별자만 허용한다.
    public boolean hasMicrosoftIdentity() {
        return canonicalUuid(microsoftTenantId) && canonicalUuid(microsoftClientId)
                && !microsoftClientSecret.isBlank() && !smtpUsername.isBlank();
    }

    public boolean isReady() {
        return enabled && !smtpHost.isBlank() && !from.isBlank() && !appUrl.isBlank()
                && (smtpAuthMode!=SmtpAuthMode.MICROSOFT365_OAUTH2 || hasMicrosoftIdentity());
    }

    private boolean canonicalUuid(String value) {
        try { return UUID.fromString(value).toString().equalsIgnoreCase(value); }
        catch (RuntimeException failure) { return false; }
    }
}
