package milky_way.controller;

import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserRequestLogin;
import milky_way.dto.response.AuthResponse;
import milky_way.dto.response.UserResponse;
import milky_way.service.AuthService;
import milky_way.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("api/v1/user")
public class UserController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest userRequest){
        return ResponseEntity.ok(authService.registerUser(userRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserRequestLogin userRequest){
        Optional<AuthResponse> userResponse = authService.loginUser(userRequest);
        if(userResponse.isPresent()){
            return ResponseEntity.ok("Login successful!");
        }
        return ResponseEntity.status(401).body("Invalid credentials!");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(){
        return ResponseEntity.ok("Logout successful!");
    }
}
