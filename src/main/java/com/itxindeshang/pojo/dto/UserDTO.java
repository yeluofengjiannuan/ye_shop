package com.itxindeshang.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserDTO implements Serializable {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
