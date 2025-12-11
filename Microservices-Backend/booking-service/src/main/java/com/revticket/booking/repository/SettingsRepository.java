package com.revticket.booking.repository;

import com.revticket.booking.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, String> {
    Optional<Settings> findByKey(String key);
}
