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

        assertThrows(com.naranjax.common.exception.BusinessException.class, 
                () -> authService.getUserById(999L));
        System.out.println("[TEST-LOG] ✅ Excepción de usuario no encontrado verificada.");
    }

    @Test
    void mapToDto_ConsistencyCheck() {
         // [TEST-LOG] Test de consistencia de mapeo interno
         User user = User.builder()
                .id(100L)
                .firstName("Test")
                .roles(Set.of())
                .build();
         // Mock the repository call for this specific test to avoid NotFound exception
         when(userRepository.findById(100L)).thenReturn(Optional.of(user));
         UserDto dto = authService.getUserById(100L); // Llama indirectamente al mapper
         assertNotNull(dto);
         assertEquals(user.getId(), dto.getId());
         assertEquals(user.getFirstName(), dto.getFirstName());
         System.out.println("[TEST-LOG] ✅ Mapeo de identidad verificado.");
    }
}
