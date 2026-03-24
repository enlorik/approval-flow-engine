package com.approvalflow.engine.service;

import com.approvalflow.engine.dto.request.LoginRequest;
import com.approvalflow.engine.dto.request.RegisterRequest;
import com.approvalflow.engine.dto.response.AuthResponse;
import com.approvalflow.engine.entity.Role;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.AuditAction;
import com.approvalflow.engine.enums.RoleName;
import com.approvalflow.engine.exception.InvalidStateException;
import com.approvalflow.engine.repository.RoleRepository;
import com.approvalflow.engine.repository.UserRepository;
import com.approvalflow.engine.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;
    
    public AuthService(UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      JwtTokenProvider tokenProvider,
                      AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidStateException("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidStateException("Email already exists");
        }
        
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new InvalidStateException("Default role not found"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .roles(roles)
                .build();
        
        user = userRepository.save(user);
        
        auditLogService.logAction(user, AuditAction.USER_REGISTERED, "User", user.getId(), 
                "User registered: " + user.getUsername());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .build();
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidStateException("User not found"));
        
        auditLogService.logAction(user, AuditAction.USER_LOGIN, "User", user.getId(), 
                "User logged in: " + user.getUsername());
        
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .build();
    }
}
