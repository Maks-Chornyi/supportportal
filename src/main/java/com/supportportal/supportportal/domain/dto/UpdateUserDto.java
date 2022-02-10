package com.supportportal.supportportal.domain.dto;

import com.supportportal.supportportal.domain.User;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateUserDto {
    private String currentUsername;
    private User user;
    private MultipartFile profileImg;
}
