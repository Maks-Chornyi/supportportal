package com.supportportal.supportportal.service;

import com.supportportal.supportportal.domain.User;

import java.util.List;

public interface UserService {

    User register(User user);

    List<User> findAll();

    User findUserByUserName(String username);

    User findUserByEmail(String email);
}
