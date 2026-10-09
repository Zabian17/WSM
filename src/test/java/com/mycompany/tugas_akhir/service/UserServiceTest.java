package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.PasswordHelper;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository);
    }

    @Test
    void listsUsersInNameOrderForPickerAssignment() {
        List<User> users = List.of(
                User.builder().id(1).fullName("Alice").build(),
                User.builder().id(2).fullName("Bob").build());
        when(userRepository.findAll(any(Sort.class))).thenReturn(users);

        assertEquals(users, service.getUsersForAssignment());
        verify(userRepository).findAll(Sort.by(Sort.Direction.ASC, "fullName"));
    }

    @Test
    void passwordChangeRequiresTheCurrentPasswordAndPersistsBcrypt() {
        User user = User.builder().id(4).passwordHash(PasswordHelper.hashPassword("old-password")).build();
        when(userRepository.findById(4)).thenReturn(Optional.of(user));

        assertFalse(service.changePassword(4, "wrong-password", "new-password"));
        verify(userRepository, never()).save(any());

        assertTrue(service.changePassword(4, "old-password", "new-password"));
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(PasswordHelper.verifyPassword("new-password", user.getPasswordHash()));
        verify(userRepository).save(user);
    }

    @Test
    void profileUpdateRejectsAnEmailOwnedByAnotherUser() {
        when(userRepository.existsByEmailAndIdNot("used@example.com", 4)).thenReturn(true);

        assertNull(service.updateProfile(4, "Operator", " USED@example.com "));

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void profileUpdateNormalizesEmailAndPersistsName() {
        User user = User.builder().id(4).fullName("Old Name").email("old@example.com").build();
        when(userRepository.existsByEmailAndIdNot("new@example.com", 4)).thenReturn(false);
        when(userRepository.findById(4)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = service.updateProfile(4, " New Name ", " NEW@example.com ");

        assertNotNull(updated);
        assertEquals("New Name", updated.getFullName());
        assertEquals("new@example.com", updated.getEmail());
        verify(userRepository).save(user);
    }
}