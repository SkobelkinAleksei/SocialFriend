package org.example.event.dto;

import org.example.event.entity.enums.VoteType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteDto {
    private Long targetId;
    private VoteType voteType;
}
