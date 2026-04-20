package org.example.restclient.config;

import com.example.common.RequestData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new SecurityHeaderInterceptor()));

        return restTemplate;
    }

    @Bean
    public IHttpCore httpCore(RestTemplate restTemplate) {
        return new IHttpCore() {
            @Override
            public <T> ResponseEntity<T> get(RequestData requestData, Class<T> responseType) {
                return restTemplate.getForEntity(requestData.url(), responseType);
            }
        };
    }
}