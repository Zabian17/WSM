package com.mycompany.tugas_akhir.integration.erp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@ConfigurationProperties(prefix = "gudangku.erp.accurate")
public class AccurateApiProperties {
    private String apiToken = "";
    private URI accountBaseUrl = URI.create("https://account.accurate.id");

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public URI getAccountBaseUrl() { return accountBaseUrl; }
    public void setAccountBaseUrl(URI accountBaseUrl) { this.accountBaseUrl = accountBaseUrl; }
}
