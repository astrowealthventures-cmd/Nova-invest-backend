package com.novainvest.bootstrap;

import com.novainvest.model.Plan;
import com.novainvest.model.User;
import com.novainvest.repository.PlanRepository;
import com.novainvest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    private static final List<Plan> DEFAULT_PLANS = List.of(
            new Plan("starter", "Starter", 50, 999, 1.2, 20, false, "Perfect for first-time investors"),
            new Plan("silver", "Silver", 1000, 4999, 1.8, 25, false, "Balanced growth, steady returns"),
            new Plan("gold", "Gold", 5000, 19999, 2.5, 30, true, "Our most popular tier"),
            new Plan("platinum", "Platinum", 20000, 250000, 3.4, 40, false, "Institutional-grade yields")
    );

    public DataSeeder(PlanRepository planRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedPlans();
        seedAdmin();
    }

    private void seedPlans() {
        for (Plan plan : DEFAULT_PLANS) {
            planRepository.save(plan); // upsert by id
        }
    }

    private void seedAdmin() {
        String email = adminEmail.toLowerCase();
        User existing = userRepository.findByEmail(email).orElse(null);

        if (existing == null) {
            User admin = new User();
            admin.setId(UUID.randomUUID().toString());
            admin.setEmail(email);
            admin.setFirstName("Casper");
            admin.setRole("admin");
            admin.setBalance(0.0);
            admin.setReferralCode("ADMIN");
            admin.setReferredBy(null);
            admin.setCreatedAt(Instant.now().toString());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setEnabled(true); // seeded admin should never need email verification
            userRepository.save(admin);
        } else if (!passwordEncoder.matches(adminPassword, existing.getPasswordHash())) {
            existing.setPasswordHash(passwordEncoder.encode(adminPassword));
            userRepository.save(existing);
        }
    }
}
