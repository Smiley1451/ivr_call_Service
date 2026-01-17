package com.labourconnect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Twilio configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "twilio")
@Data
public class TwilioConfig {

    private Account account;
    private Auth auth;
    private Phone phone;
    private Webhook webhook;

    @Data
    public static class Account {
        private String sid;
    }

    @Data
    public static class Auth {
        private String token;
    }

    @Data
    public static class Phone {
        private String number;
    }

    @Data
    public static class Webhook {
        private Base base;

        @Data
        public static class Base {
            private String url;
        }
    }
}
