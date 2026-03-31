package com.tamixa.api.controller

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Sql("/db/ai-control-plane-test-seed.sql")
class AiControlPlaneAdminIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["SUPER_ADMIN"])
    fun `list projects returns seeded tamixa project`() {
        mockMvc.perform(get("${ApiVersion.V1}/admin/ai-control-plane/projects"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'tamixa')]").exists())
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["SUPER_ADMIN"])
    fun `list workflows for tamixa returns story workflow`() {
        mockMvc.perform(get("${ApiVersion.V1}/admin/ai-control-plane/projects/tamixa/workflows"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.workflowKey == 'story.create_with_narration')]").exists())
    }
}
