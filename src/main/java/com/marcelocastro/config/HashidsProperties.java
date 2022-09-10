package com.marcelocastro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("com.marcelocastro.hashids")
public class HashidsProperties {

  private String salt;
  private Integer minHashLength;
  private String alphabet;
}
