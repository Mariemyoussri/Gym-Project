package com.example.gymserver.dto;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
    private Long sessionId;
    private String name;
    private String date;
    private String attendee;
}
