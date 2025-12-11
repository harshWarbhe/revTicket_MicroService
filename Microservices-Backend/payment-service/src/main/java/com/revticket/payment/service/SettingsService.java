package com.revticket.payment.service;

import com.revticket.payment.dto.SettingsDTO;
import com.revticket.payment.entity.Settings;
import com.revticket.payment.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class SettingsService {

    @Autowired
    private SettingsRepository settingsRepository;

    private static final Map<String, String> DEFAULTS = new HashMap<>() {{
        put("siteName", "RevTicket");
        put("siteEmail", "support@revticket.com");
        put("sitePhone", "+91 1234567890");
        put("currency", "INR");
        put("timezone", "Asia/Kolkata");
        put("bookingCancellationHours", "2");
        put("convenienceFeePercent", "5.0");
        put("gstPercent", "18.0");
        put("maxSeatsPerBooking", "10");
        put("enableNotifications", "true");
        put("enableEmailNotifications", "true");
        put("enableSMSNotifications", "false");
        put("maintenanceMode", "false");
    }};

    @Cacheable("settings")
    @Transactional(readOnly = true)
    public SettingsDTO getSettings() {
        SettingsDTO dto = new SettingsDTO();
        dto.setSiteName(getSetting("siteName"));
        dto.setSiteEmail(getSetting("siteEmail"));
        dto.setSitePhone(getSetting("sitePhone"));
        dto.setCurrency(getSetting("currency"));
        dto.setTimezone(getSetting("timezone"));
        dto.setBookingCancellationHours(Integer.parseInt(getSetting("bookingCancellationHours")));
        dto.setConvenienceFeePercent(Double.parseDouble(getSetting("convenienceFeePercent")));
        dto.setGstPercent(Double.parseDouble(getSetting("gstPercent")));
        dto.setMaxSeatsPerBooking(Integer.parseInt(getSetting("maxSeatsPerBooking")));
        dto.setEnableNotifications(Boolean.parseBoolean(getSetting("enableNotifications")));
        dto.setEnableEmailNotifications(Boolean.parseBoolean(getSetting("enableEmailNotifications")));
        dto.setEnableSMSNotifications(Boolean.parseBoolean(getSetting("enableSMSNotifications")));
        dto.setMaintenanceMode(Boolean.parseBoolean(getSetting("maintenanceMode")));
        return dto;
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public SettingsDTO updateSettings(SettingsDTO dto) {
        saveSetting("siteName", dto.getSiteName());
        saveSetting("siteEmail", dto.getSiteEmail());
        saveSetting("sitePhone", dto.getSitePhone());
        saveSetting("currency", dto.getCurrency());
        saveSetting("timezone", dto.getTimezone());
        saveSetting("bookingCancellationHours", String.valueOf(dto.getBookingCancellationHours()));
        saveSetting("convenienceFeePercent", String.valueOf(dto.getConvenienceFeePercent()));
        saveSetting("gstPercent", String.valueOf(dto.getGstPercent()));
        saveSetting("maxSeatsPerBooking", String.valueOf(dto.getMaxSeatsPerBooking()));
        saveSetting("enableNotifications", String.valueOf(dto.getEnableNotifications()));
        saveSetting("enableEmailNotifications", String.valueOf(dto.getEnableEmailNotifications()));
        saveSetting("enableSMSNotifications", String.valueOf(dto.getEnableSMSNotifications()));
        saveSetting("maintenanceMode", String.valueOf(dto.getMaintenanceMode()));
        return getSettings();
    }

    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return settingsRepository.findByKey(key)
                .map(Settings::getValue)
                .orElse(DEFAULTS.getOrDefault(key, ""));
    }

    @Transactional
    public void saveSetting(String key, String value) {
        Settings setting = settingsRepository.findByKey(key)
                .orElse(new Settings(null, key, value, null));
        setting.setValue(value);
        settingsRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public boolean isMaintenanceMode() {
        return Boolean.parseBoolean(getSetting("maintenanceMode"));
    }

    @Transactional(readOnly = true)
    public int getCancellationWindowHours() {
        return Integer.parseInt(getSetting("bookingCancellationHours"));
    }

    @Transactional(readOnly = true)
    public int getMaxSeatsPerBooking() {
        return Integer.parseInt(getSetting("maxSeatsPerBooking"));
    }

    @Transactional(readOnly = true)
    public double getConvenienceFeePercent() {
        return Double.parseDouble(getSetting("convenienceFeePercent"));
    }

    @Transactional(readOnly = true)
    public double getGstPercent() {
        return Double.parseDouble(getSetting("gstPercent"));
    }

    @Transactional(readOnly = true)
    public boolean areNotificationsEnabled() {
        return Boolean.parseBoolean(getSetting("enableNotifications"));
    }

    @Transactional(readOnly = true)
    public boolean areEmailNotificationsEnabled() {
        return areNotificationsEnabled() && Boolean.parseBoolean(getSetting("enableEmailNotifications"));
    }

    @Transactional(readOnly = true)
    public boolean areSMSNotificationsEnabled() {
        return areNotificationsEnabled() && Boolean.parseBoolean(getSetting("enableSMSNotifications"));
    }
}
