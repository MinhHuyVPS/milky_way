package milky_way.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import milky_way.dto.request.user.ChangeStatusRequest;
import milky_way.dto.response.user.UserChangeStatusResponse;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.UserMapper;
import milky_way.repository.RoleRepository;
import milky_way.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @PersistenceContext
    private EntityManager entityManager;

    public UserChangeStatusResponse changeUserStatus(ChangeStatusRequest changeStatusRequest){
        User user = userRepository.findById(changeStatusRequest.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(changeStatusRequest.getStatus());
        userRepository.save(user);
        return userMapper.toChangeStatus(user);
    }

}
