package org.example.user.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AdminReportDto {
    Long id;
    LocalDateTime createdAt;
    String category;
    String reason;
    String status;
    Long reporterId;
    String reporterName;
    Long accusedId;
    String accusedName;
    String accusedStatus;
    long accusedReportsTotal;
    long accusedReportsUpheld;
    Long targetId;
    Long roomId;
    String targetTitle;
    String snapshotText;
    String details;
}
