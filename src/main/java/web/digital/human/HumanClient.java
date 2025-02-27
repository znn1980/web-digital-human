package web.digital.human;

import com.baidubce.qianfan.Qianfan;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class HumanClient {
    private final HumanConfig config;

    public HumanClient(HumanConfig config) {
        this.config = config;
    }

    @Bean
    public Qianfan client() {
        return new Qianfan(config.getBaidu().getAccessKey(), config.getBaidu().getAccessSecretKey());
    }

    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient.Builder().build();
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
}
