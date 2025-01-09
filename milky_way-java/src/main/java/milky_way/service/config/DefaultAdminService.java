package milky_way.service.config;

import jakarta.annotation.PostConstruct;
import milky_way.entity.Role;
import milky_way.entity.User;
import milky_way.repository.RoleRepository;
import milky_way.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class DefaultAdminService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostConstruct
    public void createDefaultAdmin() {
        try {
            if (userRepository.findByAnyEmail("ducminhhuy99tbvn4@gmail.com").isEmpty()) {
                User admin = new User();
                admin.setUsername("MinhHuy");
                admin.setEmail("ducminhhuy99tbvn4@gmail.com");
                admin.setStatus(true);
                admin.setPassword(passwordEncoder.encode("Ducthihoi4869!"));
                if (admin.getRoles() == null) {
                    admin.setRoles(new HashSet<>());
                }
                Role role = roleRepository.findByName("ADMIN").orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("ADMIN");
                    newRole.setDescription("Quyền cao nhất");
                    roleRepository.save(newRole);
                    return newRole;
                });
                admin.getRoles().add(role);
                userRepository.save(admin);
            }
        } catch (Exception e) {
//            System.err.println("Lỗi khi tạo Admin: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
