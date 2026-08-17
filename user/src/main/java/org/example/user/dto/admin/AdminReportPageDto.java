package org.example.user.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminReportPageDto {
    List<AdminReportDto> items;
    long total;
    int page;
    int size;
    long openCount;
    long upheldCount;
    long warnedCount;
}
