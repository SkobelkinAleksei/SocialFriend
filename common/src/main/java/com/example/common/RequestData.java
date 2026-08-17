package com.example.common;

import java.util.Collections;
import java.util.Map;

public record RequestData(
        String url,
        Map<String, String> headers
) {
    public RequestData(String url) {
        this(url, Collections.emptyMap());
    }
}
