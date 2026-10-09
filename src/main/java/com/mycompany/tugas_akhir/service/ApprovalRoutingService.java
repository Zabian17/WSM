package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.ApprovalRoutingRule;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.ApprovalRoutingRuleRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ApprovalRoutingService implements ApprovalRouter {

    private final ApprovalRoutingRuleRepository ruleRepository;
    private final UserRepository userRepository;

    public ApprovalRoutingService(ApprovalRoutingRuleRepository ruleRepository, UserRepository userRepository) {
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean canReview(User requester, User reviewer) {
        if (requester == null || reviewer == null || requester.getId() == null
                || requester.getId().equals(reviewer.getId())) return false;
        String requesterDepartment = normalize(requester.getDepartment());
        String reviewerDepartment = normalize(reviewer.getDepartment());
        if (requesterDepartment == null || reviewerDepartment == null) return false;
        return ruleRepository.findByRequesterDepartmentIgnoreCase(requesterDepartment)
                .map(rule -> reviewerDepartment.equals(normalize(rule.getApproverDepartment())))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRoutingRule> getRules() {
        return ruleRepository.findAll(Sort.by(Sort.Direction.ASC, "requesterDepartment"));
    }

    @Transactional
    public boolean saveRule(Integer adminId, String requesterDepartment, String approverDepartment) {
        if (!isAdmin(adminId)) return false;
        String requester = normalize(requesterDepartment);
        String approver = normalize(approverDepartment);
        if (requester == null || approver == null || requester.equals(approver)
                || !userRepository.existsByDepartmentIgnoreCase(requester)
                || !userRepository.existsByDepartmentIgnoreCase(approver)) return false;
        ApprovalRoutingRule rule = ruleRepository.findByRequesterDepartmentIgnoreCase(requester)
                .orElseGet(() -> new ApprovalRoutingRule(requester, approver));
        rule.setApproverDepartment(approver);
        ruleRepository.save(rule);
        return true;
    }

    @Transactional
    public boolean deleteRule(Integer adminId, Integer ruleId) {
        if (!isAdmin(adminId) || ruleId == null || !ruleRepository.existsById(ruleId)) return false;
        ruleRepository.deleteById(ruleId);
        return true;
    }

    @Transactional
    public boolean assignDepartment(Integer adminId, Integer userId, String department) {
        if (!isAdmin(adminId) || userId == null || department == null
                || department.isBlank() || department.trim().length() > 100) return false;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        user.setDepartment(department.trim());
        userRepository.save(user);
        return true;
    }

    private boolean isAdmin(Integer userId) {
        if (userId == null) return false;
        User user = userRepository.findById(userId).orElse(null);
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    private String normalize(String department) {
        if (department == null || department.isBlank()) return null;
        return department.trim().toLowerCase(Locale.ROOT);
    }
}
