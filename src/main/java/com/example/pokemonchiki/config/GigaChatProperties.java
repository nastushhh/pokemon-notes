package com.example.pokemonchiki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gigachat")
public class GigaChatProperties {
    private String oauthUrl;
    private String apiUrl;
    private String filesUrl;
    private String clientId;
    private String clientSecret;
    private String scope;

    public String getOauthUrl() { return oauthUrl; }
    public void setOauthUrl(String oauthUrl) { this.oauthUrl = oauthUrl; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getFilesUrl() { return filesUrl; }
    public void setFilesUrl(String filesUrl) { this.filesUrl = filesUrl; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
