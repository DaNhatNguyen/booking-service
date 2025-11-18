package com.example.booking_service.service;

import com.example.booking_service.dto.request.UserCreationRequest;
import com.example.booking_service.dto.request.UserUpdationRequest;
import com.example.booking_service.dto.response.UserResponse;
import com.example.booking_service.entity.User;
import com.example.booking_service.enums.Role;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.mapper.UserMapper;
import com.example.booking_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private UserMapper userMapper;

//    public UserService(UserRepository userRepository, UserMapper userMapper) {
//        this.userRepository = userRepository;
//        this.userMapper = userMapper;
//    }

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        // use mapstruct
        User user = userMapper.toUser(request);

        // user builder
//        User user = User.builder()
//                .username(request.getUsername())
//                .password(request.getPassword())
//                .firstName(request.getFirstName())
//                .lastName(request.getLastName())
//                .dob(request.getDob())
//                .build();

        // normally
//        user.setUsername(request.getUsername());
//        user.setPassword(request.getPassword());
//        user.setFirstName(request.getFirstName());
//        user.setLastName(request.getLastName());
//        user.setDob(request.getDob());

        // use Bcypt to encode
//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<String> roles = new HashSet<>();
        roles.add(Role.USER.name());

//        user.setRoles(roles);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    public UserResponse getMyInfo(){
        // get info user currently logged in
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String name = authentication.getName();

        User user = userRepository.findByEmail(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @PostAuthorize("returnObject.username == authentication.name") // chi la nguoi dung dang dang nhap moi duoc goi thong tin cua minh
    public UserResponse getUserById(String id) {
        log.info("In method getUserById");
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_EXISTED)));
    }

    public UserResponse updateUser(String id, UserUpdationRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_EXISTED));
        userMapper.updateUser(user, request);

//        user.setPassword(request.getPassword());
//        user.setFirstName(request.getFirstName());
//        user.setLastName(request.getLastName());
//        user.setDob(request.getDob());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }
}
