package com.srFoodDelivery.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.srFoodDelivery.model.SiteMode;

import jakarta.servlet.http.HttpSession;

/**
 * Central place to resolve, persist, and expose the currently selected site
 * mode. Mode is stored in the HTTP session so it survives across requests
 * without additional cookies.
 */
@Service
public class SiteModeManager {

    public SiteMode resolveMode(String modeParam, HttpSession session) {
        if (StringUtils.hasText(modeParam)) {
            SiteMode resolved = SiteMode.fromValue(modeParam);
            if (resolved != null) {
                setMode(resolved, session);
                return resolved;
            }
        }
        return getCurrentMode(session);
    }

    public SiteMode getCurrentMode(HttpSession session) {
        if (session == null) {
            return SiteMode.RESTAURANT;
        }
        Object sessionValue = session.getAttribute(SiteMode.SESSION_ATTRIBUTE);
        if (sessionValue instanceof SiteMode) {
            return (SiteMode) sessionValue;
        }
        if (sessionValue instanceof String) {
            SiteMode resolved = SiteMode.fromValue((String) sessionValue);
            if (resolved != null) {
                session.setAttribute(SiteMode.SESSION_ATTRIBUTE, resolved);
                return resolved;
            }
        }
        session.setAttribute(SiteMode.SESSION_ATTRIBUTE, SiteMode.RESTAURANT);
        return SiteMode.RESTAURANT;
    }

    public void setMode(SiteMode mode, HttpSession session) {
        if (session == null || mode == null) {
            return;
        }
        session.setAttribute(SiteMode.SESSION_ATTRIBUTE, mode);
    }
}

