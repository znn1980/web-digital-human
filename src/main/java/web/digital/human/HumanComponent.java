package web.digital.human;

import com.baidubce.qianfan.Qianfan;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HumanComponent {
    private final HumanProperties properties;

    public HumanComponent(HumanProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .create();
    }

    @Bean
    public Qianfan client() {
        if (StringUtils.hasText(properties.getBaidu().getQianfan().getApiKey())) {
            return new Qianfan(properties.getBaidu().getQianfan().getApiKey());
        }
        return new Qianfan(properties.getBaidu().getQianfan().getAccessKey()
                , properties.getBaidu().getQianfan().getSecretKey());
    }

    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.custom().useSystemProperties()
                .setConnectionManager(new PoolingHttpClientConnectionManager() {{
                    this.setMaxTotal(10);
                    this.setDefaultMaxPerRoute(10);
                }})
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(10))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                        .build())
                .build();
    }
}
