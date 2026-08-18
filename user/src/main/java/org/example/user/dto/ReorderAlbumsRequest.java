package org.example.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReorderAlbumsRequest {
    private List<Long> albumIds;
}
