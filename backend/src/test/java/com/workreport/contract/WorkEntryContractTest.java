package com.workreport.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workreport.controller.WorkEntryController;
import com.workreport.dto.common.PageResponse;
import com.workreport.dto.workentry.CreateWorkEntryRequest;
import com.workreport.dto.workentry.UpdateWorkEntryRequest;
import com.workreport.dto.workentry.WorkEntryResponse;
import com.workreport.security.JwtAuthenticationFilter;
import com.workreport.security.JwtTokenProvider;
import com.workreport.service.WorkEntryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkEntryController.class)
@Import(WorkEntryContractTest.TestSecurityConfig.class)
class WorkEntryContractTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkEntryService workEntryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final Long USER_ID = 1L;

    private UsernamePasswordAuthenticationToken executorAuth() {
        return new UsernamePasswordAuthenticationToken(
                USER_ID, null,
                List.of(new SimpleGrantedAuthority("ROLE_EXECUTOR")));
    }

    @Test
    void createWorkEntry_responseShape() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        WorkEntryResponse mockResponse = new WorkEntryResponse(
                1L, 10L, "TaskName", "ProjectName",
                today, new BigDecimal("2.0"), true,
                new BigDecimal("88.0"), null, now, now);

        when(workEntryService.createWorkEntry(eq(USER_ID), any(CreateWorkEntryRequest.class)))
                .thenReturn(mockResponse);

        var request = new CreateWorkEntryRequest(10L, today, new BigDecimal("2.0"));

        mockMvc.perform(post("/api/work-entries")
                        .with(authentication(executorAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.taskId", is(10)))
                .andExpect(jsonPath("$.taskName", is("TaskName")))
                .andExpect(jsonPath("$.projectName", is("ProjectName")))
                .andExpect(jsonPath("$.workDate", notNullValue()))
                .andExpect(jsonPath("$.hours", is(2.0)))
                .andExpect(jsonPath("$.editable", is(true)))
                .andExpect(jsonPath("$.taskRemainingHours", is(88.0)))
                .andExpect(jsonPath("$.warning", nullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    void updateWorkEntry_responseShape() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        WorkEntryResponse mockResponse = new WorkEntryResponse(
                1L, 10L, "TaskName", "ProjectName",
                today, new BigDecimal("3.0"), true,
                new BigDecimal("87.0"), null, now, now);

        when(workEntryService.updateWorkEntry(eq(USER_ID), eq(1L), any(UpdateWorkEntryRequest.class)))
                .thenReturn(mockResponse);

        var request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));

        mockMvc.perform(put("/api/work-entries/1")
                        .with(authentication(executorAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.taskId", is(10)))
                .andExpect(jsonPath("$.taskName", is("TaskName")))
                .andExpect(jsonPath("$.hours", is(3.0)))
                .andExpect(jsonPath("$.taskRemainingHours", is(87.0)));
    }

    @Test
    void createWorkEntry_withWarning_responseShape() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        WorkEntryResponse mockResponse = new WorkEntryResponse(
                1L, 10L, "TaskName", "ProjectName",
                today, new BigDecimal("5.0"), true,
                BigDecimal.ZERO, "Task budget exhausted", now, now);

        when(workEntryService.createWorkEntry(eq(USER_ID), any(CreateWorkEntryRequest.class)))
                .thenReturn(mockResponse);

        var request = new CreateWorkEntryRequest(10L, today, new BigDecimal("5.0"));

        mockMvc.perform(post("/api/work-entries")
                        .with(authentication(executorAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warning", is("Task budget exhausted")))
                .andExpect(jsonPath("$.taskRemainingHours", is(0)));
    }

    @Test
    void getWorkEntries_responseShape() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        WorkEntryResponse entry = new WorkEntryResponse(
                1L, 10L, "TaskName", "ProjectName",
                today, new BigDecimal("2.0"), true,
                new BigDecimal("88.0"), null, now, now);
        PageResponse<WorkEntryResponse> pageResponse = new PageResponse<>(
                List.of(entry), 0, 20, 1, 1);

        when(workEntryService.getWorkEntries(eq(USER_ID), any(LocalDate.class),
                any(LocalDate.class), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/work-entries")
                        .with(authentication(executorAuth()))
                        .param("startDate", today.minusDays(7).toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].taskId", is(10)))
                .andExpect(jsonPath("$.content[0].taskName", is("TaskName")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }
}
