package web.digital.human;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.QianfanV2;
import com.baidubce.qianfan.core.StreamIterator;
import com.baidubce.qianfan.model.chat.ChatRequest;
import com.baidubce.qianfan.model.chat.v2.request.RequestV2;
import com.baidubce.qianfan.model.chat.v2.response.ResponseV2;
import com.baidubce.qianfan.model.chat.v2.response.StreamResponseV2;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class HumanController {
    private final static Logger LOGGER = LoggerFactory.getLogger(HumanController.class);
    private final static String ASR_URL = "https://vop.baidu.com/pro_api?token=%s&cuid=SC1234567890&dev_pid=80001";
    private final static String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s";
    private final HumanConfig config;
    private final Gson gson;
    private final QianfanV2 client;
    private final CloseableHttpClient httpClient;
    private final Credentials credentials = new Credentials();

    public HumanController(HumanConfig config, Gson gson, Qianfan client, CloseableHttpClient httpClient) {
        this.config = config;
        this.gson = gson;
        this.client = client.v2();
        this.httpClient = httpClient;
    }

    @GetMapping(value = "/api/credentials", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String credentials() throws IOException {
        this.refreshCredentials();
        return this.credentials.getAccessToken();
    }

    @PostMapping(value = "/api/speech/recognitions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String speechRecognitions(@RequestBody String vop) throws IOException {
        this.refreshCredentials();
        return this.httpClient.execute(ClassicRequestBuilder
                .post(String.format(ASR_URL, this.credentials.getAccessToken()))
                .addHeader("format", "pcm")
                .addHeader("rate", "16000")
                .setEntity(EntityBuilder.create()
                        .setContentType(ContentType.create("audio/pcm"
                                , new BasicNameValuePair("rate", "16000")))
                        .setBinary(Base64.decodeBase64(URLDecoder.decode(vop, "UTF-8"))).build())
                .build(), response -> {
            String json = EntityUtils.toString(response.getEntity());
            LOGGER.info("ASR：{}", json);
            return json;
        });
    }

    @PostMapping("/api/chat/completions")
    public ResponseEntity<StreamingResponseBody> chatCompletions(@RequestBody RequestV2 request) {
        LOGGER.info("问：{}", this.gson.toJson(request));
        if (request.isStream()) {
            StreamIterator<StreamResponseV2> stream = this.client.chatCompletionStream(request);
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(os -> {
                try {
                    while (stream.hasNext()) {
                        StreamResponseV2 response = stream.next();
                        if (response == null) {
                            break;
                        }
                        String json = this.gson.toJson(response);
                        LOGGER.info("答：{}", json);
                        StreamUtils.copy("data:" + json + "\n\n", StandardCharsets.UTF_8, os);
                    }
                    StreamUtils.copy("data:[DONE]\n\n", StandardCharsets.UTF_8, os);
                } finally {
                    stream.close();
                }
            });
        } else {
            ResponseV2 response = this.client.chatCompletion(request);
            String json = this.gson.toJson(response);
            LOGGER.info("答：{}", json);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(os ->
                    StreamUtils.copy(json, StandardCharsets.UTF_8, os));
        }
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<StreamingResponseBody> chatCompletions(@RequestBody ChatRequest request) throws IOException {
        LOGGER.info("问：{}", this.gson.toJson(request));
        CloseableHttpResponse response = this.httpClient.execute(ClassicRequestBuilder
                .post(String.format("%s/chat/completions", config.getOpenai().getBaseUrl()))
                .addHeader("Authorization", String.format("Bearer %s", config.getOpenai().getApiKey()))
                .setEntity(EntityBuilder.create()
                        .setContentType(ContentType.APPLICATION_JSON)
                        .setText(this.gson.toJson(request)).build()).build());
        return ResponseEntity.ok().contentType(request.getStream() ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON).body(os -> {
            try {
                if (request.getStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) {
                                continue;
                            }
                            LOGGER.info("答：{}", line);
                            StreamUtils.copy(line + "\n\n", StandardCharsets.UTF_8, os);
                        }
                    }
                } else {
                    String json = EntityUtils.toString(response.getEntity());
                    LOGGER.info("答：{}", json);
                    StreamUtils.copy(json, StandardCharsets.UTF_8, os);
                }
            } catch (ParseException e) {
                throw new IOException(e);
            } finally {
                response.close();
            }
        });
    }

    void refreshCredentials() throws IOException {
        if (!StringUtils.hasText(this.credentials.getAccessToken())
                || this.credentials.getExpiresIn() < System.currentTimeMillis()) {
            Credentials credentials = this.httpClient.execute(ClassicRequestBuilder
                    .get(String.format(TOKEN_URL, config.getBaidu().getAppKey(), config.getBaidu().getAppSecretKey())).build(), response -> {
                String json = EntityUtils.toString(response.getEntity());
                LOGGER.info("TOKEN：{}", json);
                return this.gson.fromJson(json, Credentials.class);
            });
            this.credentials.setError(credentials.getError());
            this.credentials.setErrorDescription(credentials.getErrorDescription());
            this.credentials.setAccessToken(credentials.getAccessToken());
            this.credentials.setExpiresIn(System.currentTimeMillis() + credentials.getExpiresIn() * 1000);
        }
    }

    static class Credentials {
        private String error;
        private String errorDescription;
        private String accessToken;
        private long expiresIn;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public void setErrorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }
}
