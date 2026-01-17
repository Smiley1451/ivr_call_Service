package com.labourconnect.dto;

import java.util.List;

public record WhatsAppBotJobEvent(
        String providerId,
        String providerName,
        String title,
        String description,
        Double wage,
        Double latitude,
        Double longitude,
        List<String> requiredSkills,
        Integer numberOfEmployees
) {}