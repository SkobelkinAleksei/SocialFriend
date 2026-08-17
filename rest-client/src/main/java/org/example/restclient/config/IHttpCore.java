package org.example.restclient.config;

import com.example.common.RequestData;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public interface IHttpCore {
    <T> ResponseEntity<T> get(RequestData requestData, Class<T> responseType);
    <T> ResponseEntity<T> post(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType);
}