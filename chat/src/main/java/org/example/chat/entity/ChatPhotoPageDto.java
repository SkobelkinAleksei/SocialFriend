package org.example.chat.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatPhotoPageDto {
    private List<ChatPhotoItemDto> items = new ArrayList<>();
    private int page;
    private int size;
    private long total;
    @JsonProperty("hasMore")
    private boolean hasMore;
}
