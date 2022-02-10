package com.supportportal.supportportal.service.impl;

import com.supportportal.supportportal.config.constant.Role;
import com.supportportal.supportportal.domain.User;
import com.supportportal.supportportal.domain.UserPrincipal;
import com.supportportal.supportportal.domain.dto.UpdateUserDto;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.supportportal.supportportal.config.constant.FileConstant.*;
import static com.supportportal.supportportal.config.constant.Role.ROLE_HR;
import static com.supportportal.supportportal.config.constant.Role.ROLE_USER;
import static com.supportportal.supportportal.config.constant.UserImplConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
        user.setAuthorities(ROLE_USER.getAuthorities());
        user.setProfileImgUrl(getTemporaryProfileImageUrl(user.getUsername()));
        userRepository.save(user);
        emailService.sendRegistrationEmail(user.getFirstName(), password, user.getEmail());

        return user;
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

    @Override
    public User addNewUser(User user) {
        String password = generatePassword();
        user.setUserId(generateUserId());
        user.setJoinDate(new Date());
        user.setPassword(encodePassword(password));
        user.setAuthorities(getRoleEnumName(user.getRole()).getAuthorities());
        user.setProfileImgUrl(getTemporaryProfileImageUrl(user.getUsername()));
        userRepository.save(user);
        //saveProfileImage(user, profileImg);

        return null;
    }

    @Override
    public User updateUser(UpdateUserDto userDto) {
        User newUser = userDto.getUser();
        final User currentUser = validateNewUsernameAndEmail(userDto.getCurrentUsername(), newUser.getUsername(), newUser.getEmail());
        //TODO: provided non null for controller validation
        currentUser.setFirstName(newUser.getFirstName());
        currentUser.setLastName(newUser.getLastName());
        currentUser.setUsername(newUser.getUsername());
        currentUser.setEmail(newUser.getEmail());
        currentUser.setActive(newUser.isActive());
        currentUser.setNotLocked(newUser.isNotLocked());
        currentUser.setRole(newUser.getRole());
        userRepository.save(currentUser);
        saveProfileImage(currentUser, userDto.getProfileImg());

        return currentUser;
    }

    @Override
    public void deleteUser(long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void resetPassword(String email) {
        final User userByEmail = findUserByEmail(email);
        String password = generatePassword();
        userByEmail.setPassword(encodePassword(password));
        userRepository.save(userByEmail);
        emailService.sendRegistrationEmail(userByEmail.getFirstName(), password, userByEmail.getEmail());
    }

    @Override
    public User updateProfileImage(UpdateUserDto userDto) {
        User user = validateNewUsernameAndEmail(userDto.getUser().getUsername(), null, null);
        saveProfileImage(user, userDto.getProfileImg());
        return user;
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
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

    private void saveProfileImage(User user, MultipartFile profileImg) {
        if (Objects.nonNull(profileImg)) {
            String usrFolderPathStr = USER_FOLDER + user.getUsername();
            Path userFolder = Paths.get(usrFolderPathStr).toAbsolutePath().normalize();
            try {
                if (!Files.exists(userFolder)) {
                    Files.createDirectories(userFolder);
                    LOGGER.info(DIRECTORY_CREATED);
                }
                Files.deleteIfExists(Paths.get(usrFolderPathStr + DOT + JPG_EXTENSION));
                Files.copy(profileImg.getInputStream(), userFolder.resolve(user.getUsername() + DOT + JPG_EXTENSION), REPLACE_EXISTING);
                user.setProfileImgUrl(setProfileImageUrl(user.getUsername()));
                userRepository.save(user);
                LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM + profileImg.getOriginalFilename());
            } catch (IOException ex) {
                ex.getStackTrace();
                LOGGER.error("Error occurred during saving profile picture");
            }
        }
    }

    private String setProfileImageUrl(String username) {
        String path = USER_IMAGE_PATH + username + FORWARD_SLASH + username + DOT + JPG_EXTENSION;
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).toUriString();
    }

    private Role getRoleEnumName(String role) {
        //TODO: check if it is correct
        return Role.valueOf(role.toUpperCase(Locale.ROOT));
    }

    private void validateLoginAttempt(User user) {
        if (user.isNotLocked()) {
            user.setNotLocked(!loginAttemptService.hasExceededMaxAttempts(user.getUsername()));
        } else {
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }
}
