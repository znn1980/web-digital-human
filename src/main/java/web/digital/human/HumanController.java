package web.digital.human;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    /*
     * 模型列表
     */
    @GetMapping(value = "/chat/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<?>> models() {
        return Mono.just(this.properties.getChat().getModels());
    }

    /*
     * 对话
     */
    @PostMapping(value = "/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatCompletions(@RequestBody ChatRequest request) {
        return this.webClient.post()
                .uri(this.properties.getChat().getBaseUrl() + "/chat/completions")
                .header("Authorization", String.format("Bearer %s", this.properties.getChat().getApiKey()))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request).retrieve().bodyToFlux(String.class);
    }

    /*
     * 阿里云获取Token
     * https://help.aliyun.com/zh/isi/getting-started/use-http-or-https-to-obtain-an-access-token?spm=a2c4g.11186623.help-menu-30413.d_1_2_2.5e196b9bL8hfht&scm=20140722.H_113251._.OR_help-T_cn~zh-V_1
     */
    @GetMapping(value = "/aliyun/credentials", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> token() {
        return this.refreshToken().flatMap(token -> Mono.just(token.getId()));
    }

    /*
     * 阿里云语音识别
     * https://help.aliyun.com/zh/isi/developer-reference/restful-api-2?spm=a2c4g.11186623.help-menu-30413.d_3_0_0_1.11686b9brDCn5O&scm=20140722.H_92131._.OR_help-T_cn~zh-V_1
     */
    @PostMapping(value = "/aliyun/speech/recognitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> speechRecognitions(String appKey, String vop) {
        return this.refreshToken().flatMap(token -> this.webClient.post()
                .uri("https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/asr?appkey={0}&format=pcm&sample_rate=16000"
                        , appKey)
                .header("X-NLS-Token", token.getId())
                .header("Host", "nls-gateway-cn-shanghai.aliyuncs.com")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(Base64.decodeBase64(urlDecoder(vop))).retrieve().bodyToMono(String.class));
    }

    /*
     * 百度鉴权认证
     * https://ai.baidu.com/ai-doc/REFERENCE/Ck3dwjhhu
     */
    @GetMapping(value = "/baidu/credentials", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> credentials() {
        return this.refreshCredentials().flatMap(credentials -> Mono.just(credentials.getAccessToken()));
    }

    /*
     * 百度语音识别
     * https://ai.baidu.com/ai-doc/SPEECH/4lbxdz34z
     */
    @PostMapping(value = "/baidu/speech/recognitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> speechRecognitions(String vop) {
        return this.refreshCredentials().flatMap(credentials -> this.webClient.post()
                .uri("https://vop.baidu.com/pro_api?token={0}&cuid=SC1234567890&dev_pid=80001"
                        , credentials.getAccessToken())
                .header("format", "pcm")
                .header("rate", "16000")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.parseMediaType("audio/pcm;rate=16000"))
                .bodyValue(Base64.decodeBase64(urlDecoder(vop))).retrieve().bodyToMono(String.class));
    }

    //百度鉴权认证刷新
    Mono<Credentials> refreshCredentials() {
        if (StringUtils.hasText(this.credentials.getAccessToken())
                && this.credentials.getExpiresIn() > System.currentTimeMillis()) {
            return Mono.just(this.credentials);
        }
        return this.webClient.get()
                .uri("https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id={0}&client_secret={1}"
                        , this.properties.getBaidu().getApiKey(), this.properties.getBaidu().getSecretKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(Credentials.class).flatMap(credentials -> {
                    this.credentials.setAccessToken(credentials.getAccessToken());
                    this.credentials.setExpiresIn(System.currentTimeMillis() + credentials.getExpiresIn() * 1000);
                    return Mono.just(this.credentials);
                });
    }

    //阿里云获取Token刷新
    Mono<Token> refreshToken() {
        if (StringUtils.hasText(this.token.getId())
                && this.token.getExpireTime() > System.currentTimeMillis()) {
            return Mono.just(this.token);
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("AccessKeyId", this.properties.getAliyun().getAccessKeyId());
        params.add("Action", "CreateToken");
        params.add("Format", "JSON");
        params.add("RegionId", "cn-shanghai");
        params.add("SignatureMethod", "HMAC-SHA1");
        params.add("SignatureNonce", UUID.randomUUID().toString());
        params.add("SignatureVersion", "1.0");
        params.add("Timestamp", LocalDateTime.now(Clock.systemUTC())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
        params.add("Version", "2019-02-28");

        String[] keys = params.toSingleValueMap().keySet().toArray(new String[0]);
        Arrays.sort(keys);
        StringJoiner sign = new StringJoiner("&");
        for (String key : keys) {
            sign.add(String.format("%s=%s", urlEncoder(key), urlEncoder(params.getFirst(key))));
        }

        String key = String.format("%s&", this.properties.getAliyun().getAccessKeySecret());
        String value = String.format("GET&%s&%s", urlEncoder("/"), urlEncoder(sign.toString()));
        params.add("Signature", Base64.encodeBase64String(new HmacUtils("HmacSHA1", key).hmac(value)));

        return this.webClient.get()
                .uri("https://nls-meta.cn-shanghai.aliyuncs.com", uriBuilder -> uriBuilder.queryParams(params).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(Token.class).flatMap(token -> {
                    this.token.setId(token.getId());
                    this.token.setExpireTime(token.getExpireTime() * 1000);
                    return Mono.just(this.token);
                });
    }

    static String urlDecoder(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String urlEncoder(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (IOException e) {
            throw new RuntimeException(e);
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
