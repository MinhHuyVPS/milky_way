package milky_way.service;

import jakarta.transaction.Transactional;
import milky_way.dto.request.user.UserRequestRole;
import milky_way.dto.response.user.UserResponse;
import milky_way.entity.Role;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.UserMapper;
import milky_way.repository.PermissionRepository;
import milky_way.repository.RoleRepository;
import milky_way.repository.UserRepository;
import milky_way.utils.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class RoleService {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PermissionRepository permissionRepository;

    @Transactional
    public UserResponse addRolesToUser(UserRequestRole userRequestRole) {
        if (userRequestRole.getId() == null || userRequestRole.getId().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        User user = userRepository.findById(userRequestRole.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Set<Role> rolesToAdd = roleRepository.findByNameIn(userRequestRole.getName());
        if (rolesToAdd.isEmpty()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        user.getRoles().addAll(rolesToAdd);
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateRolesForUser(UserRequestRole userRequestRole) {
        if (userRequestRole.getId() == null || userRequestRole.getId().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        User user = userRepository.findById(userRequestRole.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Set<Role> newRoles = roleRepository.findByNameIn(userRequestRole.getName());
        if (newRoles.isEmpty()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        user.getRoles().clear();
        user.getRoles().addAll(newRoles);
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

}
