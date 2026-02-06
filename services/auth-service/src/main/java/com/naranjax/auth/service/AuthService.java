package com.naranjax.auth.service;

import com.naranjax.auth.dto.*;
import com.naranjax.auth.entity.Role;
import com.naranjax.auth.entity.User;
import com.naranjax.auth.entity.UserStatus;
import com.naranjax.auth.repository.RoleRepository;
import com.naranjax.auth.repository.UserRepository;
import com.naranjax.auth.security.JwtUtils;
import com.naranjax.auth.producer.AuthProducer;
import com.naranjax.common.event.UserRegisteredEvent;
import com.naranjax.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtils jwtUtils;
        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;
        private final AuthProducer authProducer;

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new BusinessException("El email ya está registrado");
                }
                if (userRepository.existsByDocumentNumber(request.getDocumentNumber())) {
                        throw new BusinessException("El número de documento ya está registrado");
                }

                Role userRole = roleRepository.findByName("USER")
                                .orElseGet(() -> roleRepository.save(
                                                Role.builder().name("USER").description("Standard User").build()));

                Set<Role> roles = new HashSet<>();
                roles.add(userRole);

                User user = User.builder()
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .documentNumber(request.getDocumentNumber())
                                .documentType(request.getDocumentType())
                                .phone(request.getPhone())
                                .status(UserStatus.ACTIVE)
                                .roles(roles)
                                .build();

                User savedUser = userRepository.save(user);

                // Disparar evento de Kafka para creación de billetera
                authProducer.sendUserRegisteredEvent(UserRegisteredEvent.builder()
                                .userId(savedUser.getId())
                                .email(savedUser.getEmail())
                                .firstName(savedUser.getFirstName())
                                .lastName(savedUser.getLastName())
                                .build());

                UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
                String accessToken = jwtUtils.generateToken(userDetails, savedUser.getId());
                String refreshToken = jwtUtils.generateRefreshToken(userDetails);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .user(mapToDto(savedUser))
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String accessToken = jwtUtils.generateToken(userDetails, user.getId());
                String refreshToken = jwtUtils.generateRefreshToken(userDetails);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .user(mapToDto(user))
                                .build();
        }

        private UserDto mapToDto(User user) {
                return UserDto.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .documentNumber(user.getDocumentNumber())
                                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                                .build();
        }
}
