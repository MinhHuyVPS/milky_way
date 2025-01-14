package milky_way.service.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import milky_way.dto.request.user.ChangeStatusRequest;
import milky_way.dto.request.user.UserUpdateRequest;
import milky_way.dto.response.user.UserChangeStatusResponse;
import milky_way.dto.response.user.UserResponse;
import milky_way.dto.response.user.UserUpdateResponse;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.UserMapper;
import milky_way.repository.RoleRepository;
import milky_way.repository.UserRepository;
import milky_way.service.config.RedisService;
import milky_way.service.token.TokenBlacklistService;
import milky_way.utils.config.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RedisService redisService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    public UserChangeStatusResponse changeUserStatus(ChangeStatusRequest changeStatusRequest){
        User user = userRepository.findById(changeStatusRequest.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(changeStatusRequest.getStatus());
        userRepository.save(user);
        return userMapper.toChangeStatus(user);
    }

    public List<UserResponse> getAll(){
        String cacheKey = "users:getAll";
        List<UserResponse> users = redisService.getList(cacheKey, UserResponse.class);
        if (users != null) {
            return users;
        }
        users = userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
        redisService.setList(cacheKey, users, 24 * 60 * 60);
        return users;
    }

    public UserResponse getById(String id) {
        String cacheKey = "users:" + id;
        UserResponse cachedUser = redisService.get(cacheKey, UserResponse.class);
        if (cachedUser != null) {
            return cachedUser;
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserResponse userResponse = userMapper.toUserResponse(user);
        redisService.set(cacheKey, userResponse, 24 * 60 * 60);
        return userResponse;
    }

    public UserUpdateResponse update(String id, UserUpdateRequest userUpdateRequest){
        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->new AppException(ErrorCode.USER_NOT_FOUND));
        existingUser.setUsername(userUpdateRequest.getUsername());
        existingUser.setEmail(userUpdateRequest.getEmail());
        existingUser.setFirstname(userUpdateRequest.getFirstname());
        existingUser.setLastname(userUpdateRequest.getLastname());
        existingUser.setPhone(userUpdateRequest.getPhone());
        existingUser.setAddress(userUpdateRequest.getAddress());
        existingUser.setBirthday(userUpdateRequest.getBirthday());
        User updateUser = userRepository.save(existingUser);
        return userMapper.toUserUpdateResponse(updateUser);
    }

    public void deleted(String id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }

    public void deleteAddFieldDeletedAtUser(String id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setDeleteDate(LocalDateTime.now());
        userRepository.save(user);
    }
}
