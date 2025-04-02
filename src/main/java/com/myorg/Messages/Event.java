package com.myorg.Messages;

import com.myorg.Messages.enums.RequestType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * POJO Class for Event(Request)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String requestId;
    private String createdAt;
    private RequestType type;
    private String from;
    private String to;
    private String message;
    private Boolean isApproved;
}
