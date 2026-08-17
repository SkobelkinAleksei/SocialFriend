package org.example.chat.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PollVoteRequest {
    private List<Long> optionIds = new ArrayList<>();
}
