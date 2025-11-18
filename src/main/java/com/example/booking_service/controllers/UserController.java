package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.UserCreationRequest;
import com.example.booking_service.dto.request.UserUpdationRequest;
import com.example.booking_service.dto.response.UserResponse;
import com.example.booking_service.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

//    @Autowired
//    private UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {

//        ApiResponse apiResponse = new ApiResponse();
//
//        apiResponse.setResult(userService.createUser(request));

        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("username: {}", authentication.getName());
        authentication.getAuthorities()
                .forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable String userId) {
        return userService.getUserById(userId);
    }

    // get info of current user
    @GetMapping("/myInfo")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(@PathVariable String userId, @RequestBody UserUpdationRequest request) {
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    public String deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return "User has been deleted";
    }


}
