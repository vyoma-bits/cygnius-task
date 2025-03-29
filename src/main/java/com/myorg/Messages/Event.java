package com.myorg.Messages;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String requestId;
    private String createdAt;
    private String type;
    private String from;
    private String to;
    private String message;
    private Boolean isApproved;
}
