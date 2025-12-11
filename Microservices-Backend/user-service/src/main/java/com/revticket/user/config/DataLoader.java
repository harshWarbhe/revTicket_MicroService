package com.revticket.user.config;

import com.revticket.user.entity.User;
import com.revticket.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createAdminUser();
    }

    private void createAdminUser() {
        String adminEmail = "admin@revticket.com";
        
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setName("Admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(User.Role.ADMIN);
            admin.setAuthProvider(User.AuthProvider.LOCAL);
            
            userRepository.save(admin);
            
            System.out.println("===========================================");
            System.out.println("✓ Admin user created successfully!");
            System.out.println("Email: " + adminEmail);
            System.out.println("Password: Admin@123");
            System.out.println("Role: ADMIN");
            System.out.println("===========================================");
        } else {
            System.out.println("Admin user already exists: " + adminEmail);
        }
    }
}
