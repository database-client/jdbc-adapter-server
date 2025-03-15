package com.dbclient.jdbc.server.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectResponse {
    private boolean success;
    private String err;
    private String fullErr;
    private String version;
}
