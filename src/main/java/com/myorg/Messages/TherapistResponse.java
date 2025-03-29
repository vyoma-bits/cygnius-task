package com.myorg.Messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TherapistResponse {
    private int therapistId;
    private String email;
    private String firstName;
    private String lastName;
    private String specialization;
    private String address;
    private String createdAt;
}
