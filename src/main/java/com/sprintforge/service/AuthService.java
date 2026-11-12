
package com.sprintforge.service;
import com.sprintforge.dto.RegisterRequest;
import com.sprintforge.dto.UserResponse;
import com.sprintforge.entity.User;
import com.sprintforge.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.dto.LoginRequest;
import com.sprintforge.dto.LoginResponse;
import org.springframework.security.authentication.BadCredentialsException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
        JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request) {

    String email = request.email().trim().toLowerCase();

    User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() ->
                    new BadCredentialsException("Invalid email or password")
            );

    if (!passwordEncoder.matches(
            request.password(),
            user.getPasswordHash())) {

        throw new BadCredentialsException(
                "Invalid email or password"
        );
    }

    String token = jwtService.generateToken(user.getEmail());

    return new LoginResponse(token, "Bearer");
}
}
