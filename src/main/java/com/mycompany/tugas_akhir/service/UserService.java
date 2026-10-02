package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.PasswordHelper;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final long MAX_PROFILE_PICTURE_BYTES = 2 * 1024 * 1024;
    private static final Path PROFILE_PICTURE_DIRECTORY = Path.of(
            System.getProperty("user.home"), ".gudangku", "profile-pictures");
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean register(String fullName, String email, String plainPassword) {
        email = normalizeEmail(email);
        if (fullName == null || fullName.isBlank() || email == null || plainPassword == null || plainPassword.length() < 6) {
            return false;
        }
        if (userRepository.existsByEmail(email)) {
            log.warn("[UserService] Email sudah terdaftar: {}", email);
            return false;
        }

        String hashedPassword = PasswordHelper.hashPassword(plainPassword);
        User newUser = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(hashedPassword)
                .role("user")
                .build();

        userRepository.save(newUser);
        log.info("[UserService] User berhasil mendaftar: {}", email);
        return true;
    }

    @Transactional
    public User updateProfile(Integer userId, String fullName, String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userId == null || fullName == null || fullName.isBlank() || fullName.trim().length() > 100
                || normalizedEmail == null || normalizedEmail.length() > 150
                || userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            return null;
        }

        return userRepository.findById(userId).map(user -> {
            user.setFullName(fullName.trim());
            user.setEmail(normalizedEmail);
            return userRepository.save(user);
        }).orElse(null);
    }

    @Transactional
    public boolean changePassword(Integer userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return false;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !PasswordHelper.verifyPassword(oldPassword, user.getPasswordHash())) {
            return false;
        }
        user.setPasswordHash(PasswordHelper.hashPassword(newPassword));
        userRepository.save(user);
        return true;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public User login(String email, String plainPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordHelper.verifyPassword(plainPassword, user.getPasswordHash())) {
                if (PasswordHelper.needsUpgrade(user.getPasswordHash())) {
                    user.setPasswordHash(PasswordHelper.hashPassword(plainPassword));
                    userRepository.save(user);
                }
                log.info("[UserService] Login berhasil untuk user: {}", email);
                return user;
            } else {
                log.warn("[UserService] Password salah untuk user: {}", email);
            }
        } else {
            log.warn("[UserService] Email tidak ditemukan: {}", email);
        }

        return null;
    }

    @Transactional
    public User updateProfilePicture(User user, Path sourceFile) throws IOException {
        validateProfilePicture(sourceFile);
        Files.createDirectories(PROFILE_PICTURE_DIRECTORY);

        String extension = extensionFor(sourceFile);
        Path destination = PROFILE_PICTURE_DIRECTORY.resolve(UUID.randomUUID() + extension);
        String previousPath = user.getProfilePicturePath();

        try {
            Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePicturePath(destination.toString());
            User savedUser = userRepository.save(user);
            deleteStoredProfilePicture(previousPath, destination);
            return savedUser;
        } catch (RuntimeException | IOException ex) {
            Files.deleteIfExists(destination);
            throw ex;
        }
    }

    @Transactional
    public User removeProfilePicture(User user) throws IOException {
        String previousPath = user.getProfilePicturePath();
        user.setProfilePicturePath(null);
        User savedUser = userRepository.save(user);
        deleteStoredProfilePicture(previousPath, null);
        return savedUser;
    }

    private void validateProfilePicture(Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IOException("File gambar tidak ditemukan.");
        }
        if (Files.size(sourceFile) > MAX_PROFILE_PICTURE_BYTES) {
            throw new IOException("Ukuran foto maksimal 2 MB.");
        }

        String extension = extensionFor(sourceFile);
        if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
            throw new IOException("Format foto harus JPG, JPEG, PNG, atau WEBP.");
        }

        try (InputStream input = Files.newInputStream(sourceFile)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null && !isWebp(sourceFile)) {
                throw new IOException("File bukan gambar yang valid.");
            }
        }
    }

    private boolean isWebp(Path file) throws IOException {
        byte[] header = Files.readAllBytes(file);
        return header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }

    private String extensionFor(Path file) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    private void deleteStoredProfilePicture(String pathValue, Path replacement) throws IOException {
        if (pathValue == null || pathValue.isBlank()) return;
        Path oldPath = Path.of(pathValue).toAbsolutePath().normalize();
        Path storageRoot = PROFILE_PICTURE_DIRECTORY.toAbsolutePath().normalize();
        if (oldPath.startsWith(storageRoot) && (replacement == null || !oldPath.equals(replacement.toAbsolutePath().normalize()))) {
            Files.deleteIfExists(oldPath);
        }
    }
}
