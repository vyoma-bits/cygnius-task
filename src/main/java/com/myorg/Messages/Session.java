package com.myorg.Messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private String sessionId;        // Unique ID of the session
    private String therapistId;      // The ID of the therapist who conducted the session
//    private String slotId;           // Unique ID for the scheduled slot
    private String privateNotes;     // Private notes visible only to the therapist
    private String sharedNotes;      // Shared notes visible to both the therapist and client
    private String sessionDate;      // The exact date of the session (in YYYY-MM-DD format)
    private String startTime;        // The start time of the session (in HH:MM:SS format)
    private String endTime;          // The end time of the session (in HH:MM:SS format)
    private String clientId;
}
