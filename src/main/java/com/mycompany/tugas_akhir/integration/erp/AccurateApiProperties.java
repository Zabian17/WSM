package com.mycompany.tugas_akhir.integration.erp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@ConfigurationProperties(prefix = "gudangku.erp.accurate")
public class AccurateApiProperties {
    public enum AuthMode { OAUTH, API_TOKEN }

    private String apiToken = "";
    private String apiSecret = "";
    private AuthMode authMode = AuthMode.OAUTH;
    private URI accountBaseUrl = URI.create("https://account.accurate.id");

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    public AuthMode getAuthMode() { return authMode; }
    public void setAuthMode(AuthMode authMode) { this.authMode = authMode; }
    public boolean usesApiTokenAuth() { return authMode == AuthMode.API_TOKEN; }
    public URI getAccountBaseUrl() { return accountBaseUrl; }
    public void setAccountBaseUrl(URI accountBaseUrl) { this.accountBaseUrl = accountBaseUrl; }
}
