package com.srFoodDelivery.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.srFoodDelivery.model.SiteMode;
import com.srFoodDelivery.service.SiteModeManager;

import jakarta.servlet.http.HttpSession;

/**
 * Ensures the current site mode is always available in Thymeleaf templates,
 * even on pages where controllers do not explicitly add it.
 */
@ControllerAdvice
public class SiteModeModelAdvice {

    private final SiteModeManager siteModeManager;

    public SiteModeModelAdvice(SiteModeManager siteModeManager) {
        this.siteModeManager = siteModeManager;
    }

    @ModelAttribute("siteMode")
    public SiteMode exposeSiteMode(HttpSession session) {
        return siteModeManager.getCurrentMode(session);
    }
}

