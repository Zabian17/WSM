package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.PasswordHelper;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyPlaintextPasswordMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyPlaintextPasswordMigration.class);
    private final UserRepository userRepository;

    public LegacyPlaintextPasswordMigration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migrated = 0;
        for (User user : userRepository.findAll()) {
            String storedPassword = user.getPasswordHash();
            if (PasswordHelper.isLegacyPlaintextHash(storedPassword)) {
                user.setPasswordHash(PasswordHelper.hashPassword(storedPassword));
                migrated++;
            }
        }
        if (migrated > 0) log.info("Migrated {} legacy plaintext password(s) to BCrypt", migrated);
    }
}
