package milky_way.service;

import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserRequestLogin;
import milky_way.dto.response.AuthResponse;
import milky_way.dto.response.UserResponse;
import milky_way.entity.Role;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.UserMapper;
import milky_way.repository.UserRepository;
import milky_way.utils.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserMapper userMapper;

    public UserResponse registerUser(UserRequest userRequest){
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        User user = userMapper.toUser(userRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role role = Role.builder()
                .name(milky_way.utils.Role.USER.getValue())
                .description("Default role for new users")
                .build();
        user.setRoles(Collections.singleton(role));
        User saveUser = userRepository.save(user);
        return userMapper.toUserResponse(saveUser);
    }

    public Optional<AuthResponse> loginUser(UserRequestLogin userRequest){
        Optional<User> user = Optional.ofNullable(userRepository.findByEmail(userRequest.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_DOSE_NOT_EXIST)));
        if(!passwordEncoder.matches(userRequest.getPassword(), user.get().getPassword())){
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }
        String token = jwtTokenProvider.generateToken(user.get().getEmail());
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);

        return Optional.of(authResponse);
    }

}
