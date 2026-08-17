package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlockStatusDto {
    private boolean blockedByMe;
    private boolean blockedMe;

    public boolean isEitherWay() {
        return blockedByMe || blockedMe;
    }
}
