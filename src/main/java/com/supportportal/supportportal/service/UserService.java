package com.supportportal.supportportal.service;

import com.supportportal.supportportal.domain.User;
import com.supportportal.supportportal.domain.dto.UpdateUserDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    User register(User user);

    List<User> findAll();

    User findUserByUserName(String username);

    User findUserByEmail(String email);

    User addNewUser(User user);

    User updateUser(UpdateUserDto userDto);

    void deleteUser(long id);

    void resetPassword(String email);

    User updateProfileImage(UpdateUserDto userDto);
}
