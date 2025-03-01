package web.digital.human;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.QianfanV2;
import com.baidubce.qianfan.core.StreamIterator;
import com.baidubce.qianfan.model.chat.v2.request.RequestV2;
import com.baidubce.qianfan.model.chat.v2.response.ResponseV2;
import com.baidubce.qianfan.model.chat.v2.response.StreamResponseV2;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.ByteString;
import okio.Okio;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

@Controller
public class HumanController {
    private final static Logger LOGGER = LoggerFactory.getLogger(HumanController.class);
    private final HumanConfig config;
    private final Gson gson;
    private final QianfanV2 client;
    private final OkHttpClient httpClient;
    private final Credentials credentials = new Credentials();

    public HumanController(HumanConfig config, Gson gson, Qianfan client, OkHttpClient httpClient) {
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
    public String speechRecognitions(@RequestBody byte[] vop) throws IOException {
        this.refreshCredentials();
        try (Response response = this.httpClient.newCall(new Request.Builder()
                .header("format", "pcm").header("rate", "16000")
                .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("audio/pcm;rate=16000")
                        , Base64.decodeBase64(vop)))
                .url(String.format("https://vop.baidu.com/pro_api?token=%s&cuid=SC1234567890&dev_pid=80001"
                        , this.credentials.getAccessToken()))
                .build()).execute()) {
            ByteString byteString = ByteString.of(response.body().bytes());
            LOGGER.info("ASR：{}", byteString.utf8());
            return byteString.utf8();
        }
    }

    @PostMapping("/api/chat/completions")
    public ResponseEntity<StreamingResponseBody> chatCompletions(@RequestBody RequestV2 request) {
        LOGGER.info("问：{}", this.gson.toJson(request));
        if (request.isStream()) {
            StreamIterator<StreamResponseV2> response = this.client.chatCompletionStream(request);
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(os -> {
                while (response.hasNext()) {
                    StreamResponseV2 stream = response.next();
                    if (stream == null) {
                        continue;
                    }
                    ByteString byteString = ByteString.encodeUtf8(this.gson.toJson(stream));
                    LOGGER.info("答：{}", byteString.utf8());
                    Okio.buffer(Okio.sink(os)).write(byteString)
                            .writeUtf8("\r\n").writeUtf8("\r\n").flush();
                }
                response.close();
            });
        } else {
            ResponseV2 response = this.client.chatCompletion(request);
            ByteString byteString = ByteString.encodeUtf8(this.gson.toJson(response));
            LOGGER.info("答：{}", byteString.utf8());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(os ->
                    Okio.buffer(Okio.sink(os)).write(byteString));
        }
    }

    void refreshCredentials() throws IOException {
        if (!StringUtils.hasText(this.credentials.getAccessToken())
                || this.credentials.getExpiresIn() < System.currentTimeMillis()) {
            try (Response response = this.httpClient.newCall(new Request.Builder()
                    .url(String.format("https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s"
                            , config.getBaidu().getAppKey(), config.getBaidu().getAppSecretKey()))
                    .build()).execute()) {
                ByteString byteString = ByteString.of(response.body().bytes());
                LOGGER.info("TOKEN：{}", byteString.utf8());
                Credentials credentials = this.gson.fromJson(byteString.utf8(), Credentials.class);
                this.credentials.setError(credentials.getError());
                this.credentials.setErrorDescription(credentials.getErrorDescription());
                this.credentials.setAccessToken(credentials.getAccessToken());
                this.credentials.setExpiresIn(System.currentTimeMillis() + credentials.getExpiresIn() * 1000);
            }
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
