package web.digital.human;

import com.fasterxml.jackson.databind.*;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;

/**
 * @author znn
 */
@Component
public class HumanComponent {
    private final static Logger logger = LoggerFactory.getLogger(HumanComponent.class);

    @Bean
    public SslContext sslContext() throws SSLException {
        return SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
    }

    @Bean
    public HttpClient httpClient(SslContext sslContext) {
        return HttpClient.create(ConnectionProvider.create("custom"))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000 * 10)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> conn
                        .addHandlerFirst(new ReadTimeoutHandler(10))
                        .addHandlerFirst(new WriteTimeoutHandler(10)))
                .doOnRequest((request, conn) ->
                        logger.info("Request: {} {} {}", request.version(), request.method(), request.uri()))
                .doOnResponse((response, conn) ->
                        logger.info("Response status: {} headers: {}", response.status(), response.responseHeaders()))
                .secure(ssl -> ssl.sslContext(sslContext));
    }

    @Bean
    public WebClient webClient(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> {
                            configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(new ObjectMapper()
                                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES), MediaType.APPLICATION_JSON));
                            configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(new ObjectMapper()
                                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES), MediaType.APPLICATION_JSON));
                        }).build())
                .build();
    }
}
