package com.supportportal.supportportal.service.impl;

import com.supportportal.supportportal.domain.User;
import com.supportportal.supportportal.domain.UserPrincipal;
import com.supportportal.supportportal.exception.domain.EmailExistException;
import com.supportportal.supportportal.exception.domain.EmailNofFoundException;
import com.supportportal.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.supportportal.exception.domain.UsernameExistException;
import com.supportportal.supportportal.repository.UserRepository;
import com.supportportal.supportportal.service.LoginAttemptService;
import com.supportportal.supportportal.service.EmailService;
import com.supportportal.supportportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.supportportal.supportportal.config.constant.Role.ROLE_HR;
import static com.supportportal.supportportal.config.constant.Role.ROLE_USER;
import static com.supportportal.supportportal.config.constant.UserImplConstant.*;

@Service
@Transactional
@Qualifier("userDetailsService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(NO_USER_FOUND_WITH_USERNAME + username));

        validateLoginAttempt(user);

        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        LOGGER.info("Returning found user: " + username);

        return userPrincipal;
    }

    private void validateLoginAttempt(User user) {
        if (user.isNotLocked()) {
            user.setNotLocked(!loginAttemptService.hasExceededMaxAttempts(user.getUsername()));
        } else {
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }

    @Override
    public User register(User user) {
        validateNewUsernameAndEmail(StringUtils.EMPTY, user.getUsername(), user.getEmail());
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(ROLE_USER.name());
        user.setAuthorities(ROLE_HR.getAuthorities());
        user.setProfileImgUrl(getTemporaryProfileImageUrl());
        userRepository.save(user);
        emailService.sendRegistrationEmail(user.getFirstName(), password, user.getEmail());

        return user;
    }

    private String getTemporaryProfileImageUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/image/profile/temp").toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateNewUsernameAndEmail(String currentUserName, String newUserName, String newEmail) {
        if (StringUtils.isNotBlank(currentUserName)) {
            final User currentUser = userRepository.findUserByUsername(currentUserName)
                    .orElseThrow(() -> new UserNotFoundException(NO_USER_FOUND_WITH_USERNAME + currentUserName));

            final Optional<User> userByUsername = userRepository.findUserByUsername(newUserName);
            userByUsername.ifPresent(user -> {
                throw new UsernameExistException(USERNAME_ALREADY_EXIST + newUserName);
            });

            final Optional<User> userByEmail = userRepository.findUserByEmail(newEmail);
            userByEmail.ifPresent(user -> {
                throw new EmailExistException(EMAIL_ALREADY_EXIST + newEmail);
            });
            return currentUser;
        } else {
            userRepository.findUserByUsername(newUserName).
                    ifPresent(usr -> {
                        throw new UsernameExistException(USERNAME_ALREADY_EXIST + newUserName);
                    });
            userRepository.findUserByEmail(newEmail)
                    .ifPresent(user -> {
                        throw new EmailExistException(EMAIL_ALREADY_EXIST + newEmail);
                    });
            return null;
        }
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUserName(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(NO_USER_FOUND_WITH_USERNAME + username));
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new EmailNofFoundException("No email found: " + email));
    }
}
