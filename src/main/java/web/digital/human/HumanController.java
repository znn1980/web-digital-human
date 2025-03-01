package web.digital.human;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.QianfanV2;
import com.baidubce.qianfan.core.StreamIterator;
import com.baidubce.qianfan.model.chat.v2.request.RequestV2;
import com.baidubce.qianfan.model.chat.v2.response.ResponseV2;
import com.baidubce.qianfan.model.chat.v2.response.StreamResponseV2;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
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

import java.io.IOException;

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
    public String credentials() throws IOException, ParseException {
        this.refreshCredentials();
        return this.credentials.getAccessToken();
    }


    @PostMapping(value = "/api/speech/recognitions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String speechRecognitions(@RequestBody byte[] vop) throws IOException, ParseException {
        this.refreshCredentials();
        HttpPost httpPost = new HttpPost(String.format(ASR_URL, this.credentials.getAccessToken()));
        httpPost.addHeader("format", "pcm");
        httpPost.addHeader("rate", "16000");
        httpPost.setEntity(HttpEntities.create(Base64.decodeBase64(vop)
                , ContentType.create("audio/pcm", new BasicNameValuePair("rate", "16000"))));
        try (CloseableHttpResponse response = this.httpClient.execute(httpPost)) {
            String json = EntityUtils.toString(response.getEntity());
            LOGGER.info("ASR：{}", json);
            return json;
        }
    }

    @PostMapping("/api/chat/completions")
    public ResponseEntity<StreamingResponseBody> chatCompletions(@RequestBody RequestV2 request) {
        LOGGER.info("问：{}", this.gson.toJson(request));
        if (request.isStream()) {
            StreamIterator<StreamResponseV2> response = this.client.chatCompletionStream(request);
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(os -> {
                try {
                    while (response.hasNext()) {
                        StreamResponseV2 stream = response.next();
                        if (stream == null) {
                            break;
                        }
                        String json = this.gson.toJson(stream);
                        LOGGER.info("答：{}", json);
                        StreamUtils.copy((json + "\r\n\r\n").getBytes(), os);
                    }
                } finally {
                    response.close();
                }
            });
        } else {
            ResponseV2 response = this.client.chatCompletion(request);
            String json = this.gson.toJson(response);
            LOGGER.info("答：{}", json);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(os ->
                    StreamUtils.copy(json.getBytes(), os));
        }
    }

    void refreshCredentials() throws IOException, ParseException {
        if (!StringUtils.hasText(this.credentials.getAccessToken())
                || this.credentials.getExpiresIn() < System.currentTimeMillis()) {
            HttpGet httpGet = new HttpGet(String.format(TOKEN_URL, config.getBaidu().getAppKey(), config.getBaidu().getAppSecretKey()));
            try (CloseableHttpResponse response = this.httpClient.execute(httpGet)) {
                String json = EntityUtils.toString(response.getEntity());
                LOGGER.info("TOKEN：{}", json);
                Credentials credentials = this.gson.fromJson(json, Credentials.class);
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
