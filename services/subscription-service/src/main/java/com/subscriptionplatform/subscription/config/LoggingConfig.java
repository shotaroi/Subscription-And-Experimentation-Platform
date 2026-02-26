package com.subscriptionplatform.subscription.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.UUID;

@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<Filter> traceIdFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                try {
                    String traceId = request instanceof HttpServletRequest req
                            ? req.getHeader("X-Trace-Id")
                            : null;
                    if (traceId == null || traceId.isBlank()) {
                        traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    }
                    MDC.put("traceId", traceId);
                    chain.doFilter(request, response);
                } finally {
                    MDC.clear();
                }
            }
        });
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
