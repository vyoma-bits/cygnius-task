package com.myorg.Messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template class for Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {
    private boolean status;
    private String message;
    private Object data;
}
