package web.digital.human;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "human")
public class HumanProperties {
    private final Baidu baidu = new Baidu();
    private final Alibaba alibaba = new Alibaba();
    private final Openai openai = new Openai();

    public Baidu getBaidu() {
        return this.baidu;
    }

    public Alibaba getAlibaba() {
        return this.alibaba;
    }

    public Openai getOpenai() {
        return this.openai;
    }

    static class Baidu {
        private String apiKey;
        private String secretKey;
        private final Qianfan qianfan = new Qianfan();

        public String getApiKey() {
            return this.apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecretKey() {
            return this.secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public Qianfan getQianfan() {
            return this.qianfan;
        }
    }

    static class Qianfan {
        private String apiKey;
        private String accessKey;
        private String secretKey;

        public String getApiKey() {
            return this.apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getAccessKey() {
            return this.accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return this.secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    static class Alibaba {
        private final Aliyun aliyun = new Aliyun();

        public Aliyun getAliyun() {
            return this.aliyun;
        }
    }

    static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }
    }

    static class Openai {
        private String baseUrl;
        private String apiKey;

        public String getBaseUrl() {
            return this.baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return this.apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
