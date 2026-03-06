package com.vpn.user.dto.response;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 255, message = "Username too long")
    private String username;

    @Size(max = 255, message = "First name too long")
    private String firstName;
}
