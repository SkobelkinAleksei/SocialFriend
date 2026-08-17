package com.example.common.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsDto {

    private Boolean allowDmFromAll;
    private Boolean allowCommentsFromAll;
    private String photoVisibility;
    private Boolean notifyComments;
    private Boolean notifyMessages;
    private Boolean notifyEventRequests;
    private Boolean notifyReputation;
    private Boolean showLastSeen;
}
