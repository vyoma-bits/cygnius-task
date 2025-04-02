package com.myorg.Messages;

import com.myorg.Messages.enums.JournalAccessStatus;
import com.myorg.Messages.enums.MappingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO Class for Mappings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mapping {
    private String mappingId;
    private String clientId;
    private String therapistId;
    private MappingStatus mappingStatus;
    private JournalAccessStatus journalAccessStatus;
    private long createdAt;
}
