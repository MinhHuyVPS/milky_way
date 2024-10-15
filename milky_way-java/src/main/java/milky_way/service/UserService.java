package milky_way.service;

import milky_way.dto.request.UserRequest;
import milky_way.dto.response.UserResponse;
import milky_way.entity.Role;
import milky_way.entity.User;
import milky_way.mapper.UserMapper;
import milky_way.repository.RoleRepository;
import milky_way.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleRepository roleRepository;

    public UserResponse registerUser(UserRequest userRequest){
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

    public Optional<UserResponse> loginUser(UserRequest userRequest){
        Optional<User> user = userRepository.findByEmail(userRequest.getEmail());
        if(user.isPresent() && passwordEncoder.matches(userRequest.getPassword(), user.get().getPassword())){
            return Optional.of(userMapper.toUserResponse(user.get()));
        }
        return Optional.empty();
    }
}
