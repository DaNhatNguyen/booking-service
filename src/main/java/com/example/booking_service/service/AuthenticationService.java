package com.example.booking_service.service;

import com.example.booking_service.dto.request.AuthenticationRequest;
import com.example.booking_service.dto.request.IntrospectRequest;
import com.example.booking_service.dto.request.LogoutRequest;
import com.example.booking_service.dto.request.RegisterRequest;
import com.example.booking_service.dto.response.AuthenticationResponse;
import com.example.booking_service.dto.response.IntrospectResponse;
import com.example.booking_service.dto.response.RegisterResponse;
import com.example.booking_service.entity.InvalidatedToken;
import com.example.booking_service.entity.User;
import com.example.booking_service.enums.Role;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.mapper.UserMapper;
import com.example.booking_service.repository.InvalidatedTokenRepository;
import com.example.booking_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    UserMapper userMapper;

    @NonFinal // đánh dấu để không inject vào constructor
    @Value("${jwt.signerKey}") // đọc giá trị từ file cấu hình gán vào biến
    protected String SIGNER_KEY;

    // verify token
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        var isValid = true;
        try {
            verifyToken(token);
        } catch (AppException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    // login
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Tìm user theo email
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .message("Đăng nhập thành công!")
                .token(token)
                .authenticated(true)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    // register
    public RegisterResponse register(RegisterRequest request) {
        // Kiểm tra trùng email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .avatar(request.getAvatar())
                .role(Role.USER) // mặc định role USER
                .build();

        userRepository.save(newUser);

        return RegisterResponse.builder()
                .message("Đăng ký thành công!")
                .user(newUser)
                .build();
    }

    // Logout
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        // read tokenId, expiryTime
        var signToken = verifyToken(request.getToken());

        String jit = signToken.getJWTClaimsSet().getJWTID();
        Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        // Save Id and exiryTime in InvalidatedToken for check
        invalidatedTokenRepository.save(invalidatedToken);
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier jwsVerifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(jwsVerifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // data trong body
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail()) // dai dien cho user dang nhap
                .issuer("nhatgioi.com") // ai là người phát hành
                .issueTime(new Date()) // lấy tại thời điểm hiện tại
                .expirationTime(new Date(
                        Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli()
                )) // Hết hạn sau 1h
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user)) // thêm các trường tùy chọn
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            System.out.println("vào đây" + jwsObject);
            return jwsObject.serialize(); // tra ve chuoi token da ma hoa
        } catch (JOSEException e) {
            log.error("Can not create token", e);
            throw new RuntimeException(e);
        }
    }

    // build scope bang cach chien tu cac mang role sang chuoi cach nhau boi dau cach
//    String buildScope(User user) {
//        StringJoiner stringJoiner = new StringJoiner(" ");
//        if(!CollectionUtils.isEmpty(user.getRoles())) {
//            user.getRoles().forEach(stringJoiner::add);
//        }
//
//        return stringJoiner.toString();
//    }

    // Xây dựng scope (authorities) từ ENUM role của user
    String buildScope(User user) {
        if (user.getRole() == null) {
            return "";
        }

        // Trả về quyền ở dạng ROLE_... để tương thích với Spring Security
        return "ROLE_" + user.getRole().name();
    }
}
