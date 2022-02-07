package com.supportportal.supportportal.controller;

import com.supportportal.supportportal.domain.User;
import com.supportportal.supportportal.exception.ExceptionHandling;
import com.supportportal.supportportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/", "/user"})
@RequiredArgsConstructor
public class UserController extends ExceptionHandling {

    @Qualifier("userDetailsService")
    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        return new ResponseEntity<>(userService.register(user), HttpStatus.CREATED);
    }
}
