package com.myorg.Messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO Class for Journal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journal {
    private String journalGroupId;
    private String journalId;
    private String clientId;
    private String emotion;
    private String feeling;
    private Integer intensity;
    private String notes;
    private String timestamp;
}