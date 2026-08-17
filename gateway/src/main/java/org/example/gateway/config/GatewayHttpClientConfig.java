package org.example.gateway.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayHttpClientConfig {

    /**
     * Netty DNS на Windows часто зависает на AAAA через роутер (192.168.x.1).
     * JDK-резолвер использует системный DNS — как ping/браузер.
     */
    @Bean
    public HttpClientCustomizer jdkDnsResolverCustomizer() {
        return httpClient -> httpClient.resolver(DefaultAddressResolverGroup.INSTANCE);
    }
}
