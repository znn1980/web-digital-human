package web.digital.human;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.QianfanV2;
import com.baidubce.qianfan.core.StreamIterator;
import com.baidubce.qianfan.model.chat.v2.request.RequestV2;
import com.baidubce.qianfan.model.chat.v2.response.StreamResponseV2;
import com.google.gson.Gson;
import okhttp3.*;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

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

    @GetMapping("/api/credentials")
    public void credentials(HttpServletResponse response) throws IOException {
        this.refreshCredentials();
        response.setHeader("Content-Type", "text/plain;charset=UTF-8");
        try (BufferedSink sink = Okio.buffer(Okio.sink(response.getOutputStream()))) {
            sink.writeUtf8(this.credentials.getAccessToken());
        }
    }

    @PostMapping("/api/speech/recognitions")
    public void speechRecognitions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.refreshCredentials();
        try (BufferedSource source = Okio.buffer(Okio.source(request.getInputStream()))) {
            try (Response vop = this.httpClient.newCall(new Request.Builder()
                    .header("format", "pcm")
                    .header("rate", "16000")
                    .post(RequestBody.create(MediaType.parse("audio/pcm;rate=16000")
                            , Base64.getDecoder().decode(source.readByteString().toByteArray())))
                    .url(String.format("https://vop.baidu.com/pro_api?token=%s&cuid=SC1234567890&dev_pid=80001"
                            , this.credentials.getAccessToken()))
                    .build()).execute()) {
                ByteString byteString = ByteString.of(vop.body().bytes());
                LOGGER.info("ASR：{}", byteString.utf8());
                response.setHeader("Content-Type", "application/json;charset=UTF-8");
                try (BufferedSink sink = Okio.buffer(Okio.sink(response.getOutputStream()))) {
                    sink.write(byteString);
                }
            }
        }
    }

    @PostMapping("/api/chat/completions")
    public void chatCompletions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (BufferedSource source = Okio.buffer(Okio.source(request.getInputStream()))) {
            ByteString byteString = source.readByteString();
            LOGGER.info("问：{}", byteString.utf8());
            RequestV2 v2 = this.gson.fromJson(byteString.utf8(), RequestV2.class);
            try (BufferedSink sink = Okio.buffer(Okio.sink(response.getOutputStream()))) {
                if (v2.isStream()) {
                    response.setHeader("Content-Type", "text/event-stream;charset=UTF-8");
                    try (StreamIterator<StreamResponseV2> streams = client.chatCompletionStream(v2)) {
                        while (streams.hasNext()) {
                            StreamResponseV2 stream = streams.next();
                            if (stream != null) {
                                LOGGER.info("答：{}", this.gson.toJson(stream));
                                sink.writeUtf8(this.gson.toJson(stream)).writeUtf8("\n\n").flush();
                            }
                        }
                    }
                } else {
                    response.setHeader("Content-Type", "application/json;charset=UTF-8");
                    byteString = ByteString.encodeUtf8(this.gson.toJson(client.chatCompletion(v2)));
                    LOGGER.info("答：{}", byteString.utf8());
                    sink.write(byteString);
                }
            }
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
