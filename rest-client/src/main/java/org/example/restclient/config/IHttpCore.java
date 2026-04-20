package org.example.restclient.config;

import com.example.common.RequestData;
import org.springframework.http.ResponseEntity;

public interface IHttpCore {
    <T> ResponseEntity<T> get(RequestData requestData, Class<T> responseType);
}