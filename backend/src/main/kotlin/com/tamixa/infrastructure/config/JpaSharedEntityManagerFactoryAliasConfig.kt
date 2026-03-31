package com.tamixa.infrastructure.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Data JPA binds a shared `EntityManager` on repository proxies using the bean name
 * `jpaSharedEM_entityManagerFactory`. If that name is not registered (while `entityManagerFactory`
 * from Boot auto-configuration exists), repository creation fails with:
 * "Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'".
 *
 * This registers an alias to the primary `EntityManagerFactory` so repositories always resolve.
 */
@Configuration
class JpaSharedEntityManagerFactoryAliasConfig {

    @Bean(name = ["jpaSharedEM_entityManagerFactory"])
    @ConditionalOnBean(name = ["entityManagerFactory"])
    @ConditionalOnMissingBean(name = ["jpaSharedEM_entityManagerFactory"])
    fun jpaSharedEM_entityManagerFactory(
        @Qualifier("entityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): EntityManagerFactory = entityManagerFactory
}
