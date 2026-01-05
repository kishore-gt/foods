package com.srFoodDelivery.security;

import java.io.IOException;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    Authentication authentication) throws IOException, ServletException {
		String targetUrl = "/";
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		for (GrantedAuthority authority : authorities) {
			String role = authority.getAuthority();
			if ("ROLE_ADMIN".equals(role)) {
				targetUrl = "/admin/dashboard";
				break;
			}
			if ("ROLE_OWNER".equals(role) || "ROLE_CAFE_OWNER".equals(role)) {
				targetUrl = "/owner/dashboard";
				break;
			}
			if ("ROLE_RIDER".equals(role)) {
				targetUrl = "/rider/dashboard";
				break;
			}
			if ("ROLE_COMPANY".equals(role)) {
				targetUrl = "/company/dashboard";
				break;
			}
			if ("ROLE_CUSTOMER".equals(role)) {
				targetUrl = "/customer/restaurants";
				break;
			}
		}
		response.sendRedirect(request.getContextPath() + targetUrl);
	}
}


