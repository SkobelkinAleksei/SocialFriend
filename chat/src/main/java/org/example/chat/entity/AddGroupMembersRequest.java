package org.example.chat.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AddGroupMembersRequest {
    private List<Long> memberIds = new ArrayList<>();
}
