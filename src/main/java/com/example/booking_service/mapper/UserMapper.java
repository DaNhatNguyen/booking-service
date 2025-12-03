package com.example.booking_service.mapper;

import com.example.booking_service.dto.request.UserCreationRequest;
import com.example.booking_service.dto.request.UserUpdationRequest;
import com.example.booking_service.dto.response.UserResponse;
import com.example.booking_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring") // báo ms biết gen để sử dụng trong spring theo DI
public interface UserMapper {
    User toUser(UserCreationRequest request);

    // map user về userResponse
    // @Mapping(source = "firstName", target = "lastName") // dùng khi map cac object khác field
    UserResponse toUserResponse(User user);

    void updateUser(@MappingTarget User user, UserUpdationRequest request);
}
