package web.digital.human;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "config")
public class HumanConfig {
    private Baidu baidu;
    private Openai openai;

    public Baidu getBaidu() {
        return baidu;
    }

    public void setBaidu(Baidu baidu) {
        this.baidu = baidu;
    }

    public Openai getOpenai() {
        return openai;
    }

    public void setOpenai(Openai openai) {
        this.openai = openai;
    }

    static class Baidu {
        private String appKey;
        private String appSecretKey;
        private String accessKey;
        private String accessSecretKey;

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecretKey() {
            return appSecretKey;
        }

        public void setAppSecretKey(String appSecretKey) {
            this.appSecretKey = appSecretKey;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getAccessSecretKey() {
            return accessSecretKey;
        }

        public void setAccessSecretKey(String accessSecretKey) {
            this.accessSecretKey = accessSecretKey;
        }
    }

    static class Openai {
        private String baseUrl;
        private String apiKey;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
