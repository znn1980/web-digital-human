package web.digital.human;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "config")
public class HumanConfig {
    private Baidu baidu;

    public Baidu getBaidu() {
        return baidu;
    }

    public void setBaidu(Baidu baidu) {
        this.baidu = baidu;
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
}
