package com.nexusxva.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusxva.auth")
public class AuthProperties {

    private boolean enabled;
    private int sessionHours = 8;
    private String cookieName = "NEXUSXVA_SESSION";
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSessionHours() {
        return sessionHours;
    }

    public void setSessionHours(int sessionHours) {
        this.sessionHours = sessionHours;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    public static class BootstrapAdmin {
        private String username = "admin";
        private String password = "";
        private String displayName = "NexusXVA Admin";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
