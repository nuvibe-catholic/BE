package com.umc.nuvibe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void signupLoginLogoutAndWithdrawPersistCorrectly() throws Exception {
        String email="login-test@example.com";
        String password="Password123!";
        mvc.perform(post("/api/auth/sign-up").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("name","test","nickname","tester","email",email,"password",password,"confirmPassword",password))))
            .andExpect(status().isOk());
        var saved=users.findByEmail(email).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(password);
        assertThat(encoder.matches(password,saved.getPassword())).isTrue();
        String loginBody=mapper.writeValueAsString(Map.of("email",email,"password",password));
        var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty()).andReturn();
        var data=mapper.readTree(result.getResponse().getContentAsString()).get("data");
        String token=data.get("accessToken").asText();
        assertThat(users.findByEmail(email).orElseThrow().getRefreshToken()).isEqualTo(data.get("refreshToken").asText());
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThat(users.findByEmail(email).orElseThrow().getRefreshToken()).isNull();
        mvc.perform(delete("/api/auth/withdraw").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThat(users.existsByEmail(email)).isFalse();
    }
}
