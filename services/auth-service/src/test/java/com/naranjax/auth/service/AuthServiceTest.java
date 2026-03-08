package com.naranjax.auth.service;

import com.naranjax.auth.entity.Role;
import com.naranjax.auth.entity.User;
import com.naranjax.auth.repository.UserRepository;
import com.naranjax.common.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void getUserById_Success() {
        // [TEST-LOG] Iniciando test de búsqueda de usuario por ID para Transaction/Notification Service
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@naranjax.com")
                .firstName("Juan")
                .lastName("Perez")
                .documentNumber("12345678")
                .roles(Set.of(Role.builder().name("USER").build()))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto result = authService.getUserById(userId);

        assertNotNull(result);
        assertEquals("Juan", result.getFirstName());
        assertEquals("test@naranjax.com", result.getEmail());
        System.out.println("[TEST-LOG] ✅ Búsqueda de usuario exitosa: " + result.getFirstName() + " " + result.getLastName());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // [TEST-LOG] Iniciando test de usuario no encontrado
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getUserById(999L));
        System.out.println("[TEST-LOG] ✅ Excepción de usuario no encontrado verificada.");
    }
}
