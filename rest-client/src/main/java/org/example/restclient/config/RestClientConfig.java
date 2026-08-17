package org.example.restclient.config;

import com.example.common.RequestData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public InternalApiKeyInterceptor internalApiKeyInterceptor(
            @Value("${app.internal.api-key:}") String apiKey
    ) {
        return new InternalApiKeyInterceptor(apiKey);
    }

    @Bean
    public InternalApiKeyFilter internalApiKeyFilter(
            @Value("${app.internal.api-key:}") String apiKey
    ) {
        return new InternalApiKeyFilter(apiKey);
    }

    @Bean
    public RestTemplate restTemplate(InternalApiKeyInterceptor internalApiKeyInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(internalApiKeyInterceptor));
        return restTemplate;
    }

    @Bean
    public IHttpCore httpCore(RestTemplate restTemplate) {
        return new IHttpCore() {
            @Override
            public <T> ResponseEntity<T> get(RequestData requestData, Class<T> responseType) {
                if (requestData.headers() == null || requestData.headers().isEmpty()) {
                    return restTemplate.getForEntity(requestData.url(), responseType);
                }
                HttpHeaders headers = new HttpHeaders();
                requestData.headers().forEach(headers::set);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                return restTemplate.exchange(requestData.url(), HttpMethod.GET, entity, responseType);
            }

            @Override
            public <T> ResponseEntity<T> post(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
                return restTemplate.exchange(url, method, entity, responseType);
            }
        };
    }
}
