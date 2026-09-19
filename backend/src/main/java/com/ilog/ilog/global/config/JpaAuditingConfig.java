package com.ilog.ilog.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(modifyOnCreate = false)   // INSERT 때 updated_at을 채우지 않음
public class JpaAuditingConfig {
}
