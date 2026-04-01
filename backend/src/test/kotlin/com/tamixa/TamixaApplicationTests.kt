package com.tamixa

import com.tamixa.api.ApiVersion
import com.tamixa.api.config.RequestTracingFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.profiles.active=test"]
)
class TamixaApplicationTests : IntegrationTestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun contextLoads() {
    }

    @Test
    fun `RequestTracingFilter is applied and echoes trace headers`() {
        val result = mockMvc.perform(get("${ApiVersion.V1}/health"))
            .andExpect(status().isOk)
            .andReturn()
        assertThat(result.response.getHeader(RequestTracingFilter.TRACE_ID_HEADER)).isNotBlank()
        assertThat(result.response.getHeader(RequestTracingFilter.CORRELATION_ID_HEADER)).isNotBlank()
    }
}
