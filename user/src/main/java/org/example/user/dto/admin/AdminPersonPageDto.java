package org.example.user.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminPersonPageDto {
    List<AdminPersonDto> items;
    long total;
    int page;
    int size;
}
