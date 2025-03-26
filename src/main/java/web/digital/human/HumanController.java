package web.digital.human;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class HumanController {
    private final HumanProperties properties;
    private final WebClient webClient;
    private final Credentials credentials = new Credentials();
    private final Token token = new Token();

    public HumanController(HumanProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    @GetMapping(value = "/chat/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<?> models() {
        return this.properties.getChat().getModels();
    }

    @PostMapping(value = "/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatCompletions(@RequestBody ChatRequest request) {
        return this.webClient.post()
                .uri(this.properties.getChat().getBaseUrl() + "/chat/completions")
                .header("Authorization", String.format("Bearer %s", this.properties.getChat().getApiKey()))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request).retrieve().bodyToFlux(String.class);
    }

    @GetMapping(value = "/aliyun/credentials", produces = MediaType.TEXT_PLAIN_VALUE)
    public String token() throws IOException {
        this.refreshToken();
        return this.token.getId();
    }

    @GetMapping(value = "/baidu/credentials", produces = MediaType.TEXT_PLAIN_VALUE)
    public String credentials() {
        this.refreshCredentials();
        return this.credentials.getAccessToken();
    }

    @PostMapping(value = "/baidu/speech/recognitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public String speechRecognitions(@RequestBody String vop) throws IOException {
        this.refreshCredentials();
        byte[] bytes = Base64.decodeBase64(URLDecoder.decode(vop, "UTF-8"));
        return this.webClient.post()
                .uri("https://vop.baidu.com/pro_api?token={0}&cuid=SC1234567890&dev_pid=80001"
                        , this.credentials.getAccessToken())
                .header("format", "pcm")
                .header("rate", "16000")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.parseMediaType("audio/pcm;rate=16000"))
                .bodyValue(bytes).retrieve().bodyToMono(String.class).block();
    }

    void refreshCredentials() {
        if (!StringUtils.hasText(this.credentials.getAccessToken())
                || this.credentials.getExpiresIn() < System.currentTimeMillis()) {
            Credentials credentials = this.webClient.get()
                    .uri("https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id={0}&client_secret={1}"
                            , this.properties.getBaidu().getApiKey(), this.properties.getBaidu().getSecretKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(Credentials.class).block();
            if (!ObjectUtils.isEmpty(credentials)) {
                this.credentials.setAccessToken(credentials.getAccessToken());
                this.credentials.setExpiresIn(System.currentTimeMillis() + credentials.getExpiresIn() * 1000);
            }
        }
    }

    void refreshToken() throws IOException {
        if (!StringUtils.hasText(this.token.getId())
                || this.token.getExpireTime() < System.currentTimeMillis()) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("AccessKeyId", properties.getAliyun().getAccessKeyId());
            params.add("Action", "CreateToken");
            params.add("Format", "JSON");
            params.add("RegionId", "cn-shanghai");
            params.add("SignatureMethod", "HMAC-SHA1");
            params.add("SignatureNonce", UUID.randomUUID().toString());
            params.add("SignatureVersion", "1.0");
            params.add("Timestamp", LocalDateTime.now(Clock.systemUTC()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
            params.add("Version", "2019-02-28");
            StringJoiner sign = new StringJoiner("&");
            for (Map.Entry<String, String> param : params.toSingleValueMap().entrySet()) {
                sign.add(String.format("%s=%s", param.getKey(), URLEncoder.encode(param.getValue(), "UTF-8")));
            }
            String key = String.format("%s&", this.properties.getAliyun().getAccessKeySecret());
            String value = String.format("GET&%s&%s", URLEncoder.encode("/", "UTF-8"), URLEncoder.encode(sign.toString(), "UTF-8"));
            params.add("Signature", Base64.encodeBase64String(new HmacUtils("HmacSHA1", key).hmac(value)));

            Token token = this.webClient.get()
                    .uri("https://nls-meta.cn-shanghai.aliyuncs.com", uriBuilder -> uriBuilder.queryParams(params).build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(Token.class).block();
            if (!ObjectUtils.isEmpty(token)) {
                this.token.setId(token.getId());
                this.token.setExpireTime(token.getExpireTime() * 1000);
            }
        }
    }

    static class ChatRequest {
        public String model;
        public boolean stream;
        public List<Message> messages;

        public ChatRequest() {

            this.model = null;
            this.stream = false;
            this.messages = null;
        }

        static class Message {
            public String role;
            public String content;

            public Message() {
                this.role = null;
                this.content = null;
            }
        }
    }

    static class Credentials {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private long expiresIn;

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

    static class Token {
        @JsonProperty("Token")
        public _Token token = new _Token();

        public String getId() {
            return this.token.id;
        }

        public void setId(String id) {
            this.token.id = id;
        }

        public long getExpireTime() {
            return this.token.expireTime;
        }

        public void setExpireTime(long expireTime) {
            this.token.expireTime = expireTime;
        }

        static class _Token {
            @JsonProperty("Id")
            public String id;
            @JsonProperty("ExpireTime")
            public long expireTime;
        }
    }
}
