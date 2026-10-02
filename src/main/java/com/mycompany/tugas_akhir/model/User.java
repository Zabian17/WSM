package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 20)
    private String role;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "profile_picture_path", length = 500)
    private String profilePicturePath;

    public User() {}

    public User(Integer id, String fullName, String email, String passwordHash, String role, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (role == null) role = "user";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProfilePicturePath() { return profilePicturePath; }
    public void setProfilePicturePath(String profilePicturePath) { this.profilePicturePath = profilePicturePath; }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Integer id;
        private String fullName;
        private String email;
        private String passwordHash;
        private String role;
        private LocalDateTime createdAt;
        private String profilePicturePath;

        public UserBuilder id(Integer id) { this.id = id; return this; }
        public UserBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public UserBuilder role(String role) { this.role = role; return this; }
        public UserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserBuilder profilePicturePath(String profilePicturePath) { this.profilePicturePath = profilePicturePath; return this; }

        public User build() {
            User user = new User(id, fullName, email, passwordHash, role, createdAt);
            user.setProfilePicturePath(profilePicturePath);
            return user;
        }
    }
}
