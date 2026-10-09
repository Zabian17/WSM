package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.ApprovalRoutingRule;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.ApprovalRoutingRuleRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalRoutingServiceTest {

    @Mock private ApprovalRoutingRuleRepository ruleRepository;
    @Mock private UserRepository userRepository;

    private ApprovalRoutingService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalRoutingService(ruleRepository, userRepository);
    }

    @Test
    void onlyConfiguredApproverDepartmentCanReview() {
        User requester = user(1, "purchasing");
        User approver = user(2, "finance");
        when(ruleRepository.findByRequesterDepartmentIgnoreCase("purchasing"))
                .thenReturn(Optional.of(new ApprovalRoutingRule("Purchasing", "Finance")));

        assertTrue(service.canReview(requester, approver));
        assertFalse(service.canReview(requester, user(3, "operations")));
        assertFalse(service.canReview(requester, user(1, "finance")));
        assertFalse(service.canReview(user(4, null), approver));
    }

    @Test
    void onlyAdminCanSaveRoutingAndDepartmentsMustDifferAndExist() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user(1, "admin")));
        when(userRepository.findById(2)).thenReturn(Optional.of(user(2, "user")));
        when(userRepository.existsByDepartmentIgnoreCase("purchasing")).thenReturn(true);
        when(userRepository.existsByDepartmentIgnoreCase("finance")).thenReturn(true);
        when(ruleRepository.findByRequesterDepartmentIgnoreCase("purchasing")).thenReturn(Optional.empty());
        when(ruleRepository.save(any(ApprovalRoutingRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.saveRule(1, " Purchasing ", "Finance"));
        assertFalse(service.saveRule(2, "Purchasing", "Finance"));
        assertFalse(service.saveRule(1, "Purchasing", "purchasing"));
        assertFalse(service.saveRule(1, "Unknown", "Finance"));
        verify(ruleRepository).save(any(ApprovalRoutingRule.class));
    }

    @Test
    void onlyAdminCanAssignUserDepartment() {
        User admin = user(1, "admin");
        User member = user(2, null);
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2)).thenReturn(Optional.of(member));
        when(userRepository.save(member)).thenReturn(member);

        assertTrue(service.assignDepartment(1, 2, " Logistics "));
        assertEquals("Logistics", member.getDepartment());
        assertFalse(service.assignDepartment(2, 2, "Finance"));
        verify(userRepository).save(member);
    }

    private User user(Integer id, String departmentOrRole) {
        return User.builder().id(id).fullName("User " + id).email("user" + id + "@example.test")
                .role(id == 1 && "admin".equals(departmentOrRole) ? "admin" : "user")
                .department("admin".equals(departmentOrRole) ? null : departmentOrRole)
                .build();
    }
}
