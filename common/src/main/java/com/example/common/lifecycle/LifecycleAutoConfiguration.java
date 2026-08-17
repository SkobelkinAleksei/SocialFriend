package com.example.common.lifecycle;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(EventLifecycleSettings.class)
public class LifecycleAutoConfiguration {
}
