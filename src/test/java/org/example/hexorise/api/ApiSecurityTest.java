package org.example.hexorise.api;
import org.example.hexorise.config.*;
import org.example.hexorise.room.*;
import org.example.hexorise.bot.*;
import org.example.hexorise.client.HighriseClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(controllers = {RoomSettingsController.class, BotStatusController.class, RoomControlsController.class}, properties = {
    "hexora.highrise.room-id=room", "hexora.admin.username=admin", "hexora.admin.password=test-password-long-enough"})
@Import(SecurityConfiguration.class)
@EnableConfigurationProperties({AdminProperties.class, HighriseProperties.class})
class ApiSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean RoomSettingsRepository repository;
    @MockitoBean HighriseClient client;
    @MockitoBean EmoteService emotes;
    @MockitoBean EmoteCatalog catalog;
    @MockitoBean RoomDirectory directory;
    @MockitoBean ModerationService moderation;
    @MockitoBean BotAdminRepository admins;
    private static final String SETTINGS = "{\"welcomeEnabled\":true,\"welcomeMessage\":\"Hi {username}\",\"commandPrefix\":\"!\",\"commandCooldownSeconds\":3}";
    @Test void rejectsAnonymousAndWrongCredentials() throws Exception {
        mvc.perform(get("/api/v1/bot/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/bot/status").with(httpBasic("admin", "wrong"))).andExpect(status().isUnauthorized());
    }
    @Test void returnsSettingsWithValidCredentials() throws Exception {
        when(repository.find("room")).thenReturn(RoomSettings.defaults());
        mvc.perform(get("/api/v1/rooms/room/settings").with(httpBasic("admin", "test-password-long-enough")))
            .andExpect(status().isOk()).andExpect(jsonPath("commandPrefix").value("!"));
    }
    @Test void requiresCsrfForWrites() throws Exception {
        mvc.perform(put("/api/v1/rooms/room/settings").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content(SETTINGS)).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }
    @Test void csrfEndpointSupportsAuthenticatedWriteRoundTrip() throws Exception {
        var result = mvc.perform(get("/api/v1/csrf").with(httpBasic("admin", "test-password-long-enough")))
            .andExpect(status().isOk()).andReturn();
        var token = new tools.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        when(repository.save(eq("room"), any())).thenAnswer(invocation -> invocation.getArgument(1));
        mvc.perform(put("/api/v1/rooms/room/settings").with(httpBasic("admin", "test-password-long-enough"))
            .session((org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false))
            .header(token.path("headerName").asText(), token.path("token").asText())
            .contentType("application/json").content(SETTINGS)).andExpect(status().isOk());
    }
    @Test void acceptsValidWriteAndRejectsInvalidPayload() throws Exception {
        when(repository.save(eq("room"), any())).thenAnswer(invocation -> invocation.getArgument(1));
        mvc.perform(put("/api/v1/rooms/room/settings").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content(SETTINGS)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/rooms/room/settings").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content(SETTINGS.replace("Seconds\":3", "Seconds\":0")))
            .andExpect(status().isBadRequest());
        verify(repository, times(1)).save(eq("room"), any());
    }
    @Test void moderationAndAdminWritesRequireRoleAndCsrf() throws Exception {
        mvc.perform(post("/api/v1/bot/moderation").with(user("visitor").roles("USER")).with(csrf())
            .contentType("application/json").content("{\"userId\":\"target\",\"action\":\"kick\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/rooms/room/admins/user").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"role\":\"OWNER\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(moderation, admins);
    }
    @Test void validatesNewControlPayloadsAndPersistsOwner() throws Exception {
        mvc.perform(post("/api/v1/bot/moderation").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"userId\":\"target\",\"action\":\"unmute\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/rooms/room/admins/user").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"role\":\"OWNER\"}")).andExpect(status().isOk());
        verify(admins).save("room", "user", BotAdminRepository.Role.OWNER);
        verifyNoInteractions(moderation);
    }
    @Test void disablingEmotesStopsLoopsOnlyForConfiguredRoom() throws Exception {
        when(repository.save(eq("room"), any())).thenAnswer(invocation -> invocation.getArgument(1));
        mvc.perform(put("/api/v1/rooms/room/settings").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content(SETTINGS.replace("Seconds\":3}", "Seconds\":3,\"emotesEnabled\":false}"))).andExpect(status().isOk());
        verify(emotes).stopAll();
    }
    @Test void browserSessionAuthenticatesNewWritePathsWithoutBasicHeader() throws Exception {
        var result = mvc.perform(get("/api/v1/csrf").with(httpBasic("admin", "test-password-long-enough")))
            .andExpect(status().isOk()).andReturn();
        var token = new tools.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(put("/api/v1/rooms/room/admins/new-owner").session(session)
            .header(token.path("headerName").asText(), token.path("token").asText())
            .contentType("application/json").content("{\"role\":\"OWNER\"}")).andExpect(status().isOk());
        verify(admins).save("room", "new-owner", BotAdminRepository.Role.OWNER);
        mvc.perform(delete("/api/v1/rooms/room/admins/new-owner").session(session)).andExpect(status().isForbidden());
        verify(admins, never()).remove(anyString(), anyString());
    }
}
