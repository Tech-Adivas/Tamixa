package com.tamixa.api.config

import com.tamixa.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * When [com.tamixa.infrastructure.config.AppProperties.ApiExposureProperties] flags match prod/staging,
 * parent JWTs must not reach OpenAPI/Swagger or Actuator (except health/info).
 */
@TestPropertySource(
    properties = [
        "app.api-exposure.swagger-requires-admin-role=true",
        "app.api-exposure.actuator-requires-admin-role=true",
    ]
)
class ApiExposureSecurityIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `actuator health remains public without auth`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `actuator metrics without auth returns 401`() {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `parent cannot access actuator metrics`() {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `parent cannot access swagger ui`() {
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "parent@test.com", roles = ["PARENT"])
    fun `parent cannot access open api docs`() {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `admin can access open api docs`() {
        val result = mockMvc.perform(get("/v3/api-docs"))
            .andReturn()
        assertThat(result.response.status).isEqualTo(HttpStatus.OK.value())
        assertThat(result.response.contentAsString).contains("openapi")
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `admin can access actuator metrics`() {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isOk)
    }
}
