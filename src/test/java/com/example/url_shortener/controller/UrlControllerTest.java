package com.example.url_shortener.controller;

import com.example.url_shortener.dto.CreateUrlResponse;
import com.example.url_shortener.dto.UrlPageResponse;
import com.example.url_shortener.dto.UrlStatsResponse;
import com.example.url_shortener.exception.GlobalExceptionHandler;
import com.example.url_shortener.exception.ShortUrlNotFoundException;
import com.example.url_shortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@Import(GlobalExceptionHandler.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @Test
    void createShortUrl_validRequest_returnsResponseAndDelegates() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 20, 12, 0);
        when(urlService.createShortUrl("https://example.com"))
                .thenReturn(new CreateUrlResponse("Ab12xY", "http://localhost:8080/Ab12xY",
                        "https://example.com", createdAt, 0));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("Ab12xY"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/Ab12xY"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.clickCount").value(0));

        verify(urlService).createShortUrl("https://example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"originalUrl\":\"\"}",
            "{\"originalUrl\":\"   \"}",
            "{\"originalUrl\":\"ftp://example.com\"}"
    })
    void createShortUrl_invalidOriginalUrl_returnsBadRequest(String requestBody) throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.originalUrl").exists());

        verifyNoInteractions(urlService);
    }

    @Test
    void createShortUrl_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlService);
    }

    @Test
    void redirect_existingShortCode_returnsFoundWithLocation() throws Exception {
        when(urlService.getOriginalUrl("Ab12xY")).thenReturn("https://example.com");

        mockMvc.perform(get("/api/v1/urls/Ab12xY"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));

        verify(urlService).getOriginalUrl("Ab12xY");
    }

    @Test
    void redirect_unknownShortCode_returnsNotFoundMessage() throws Exception {
        when(urlService.getOriginalUrl("missing"))
                .thenThrow(new ShortUrlNotFoundException("Short URL not found"));

        mockMvc.perform(get("/api/v1/urls/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Short URL not found"));
    }

    @Test
    void createUrlStats_existingShortCode_returnsStatistics() throws Exception {
        when(urlService.createUrlStats("Ab12xY"))
                .thenReturn(new UrlStatsResponse("Ab12xY", "https://example.com",
                        LocalDateTime.of(2026, 10, 20, 12, 0), 2));

        mockMvc.perform(get("/api/v1/urls/Ab12xY/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("Ab12xY"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.clickCount").value(2));

        verify(urlService).createUrlStats("Ab12xY");
    }

    @Test
    void getAllUrls_defaultParameters_delegatesDefaultValuesAndReturnsPage() throws Exception {
        when(urlService.getAllUrls(0, 10, "createdAt", "desc"))
                .thenReturn(new UrlPageResponse(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/api/v1/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(urlService).getAllUrls(0, 10, "createdAt", "desc");
    }

    @Test
    void getAllUrls_maximumPageSizeAndUppercaseDirection_areAccepted() throws Exception {
        when(urlService.getAllUrls(0, 101, "clickCount", "ASC"))
                .thenReturn(new UrlPageResponse(List.of(), 0, 101, 0, 0));

        mockMvc.perform(get("/api/v1/urls?page=0&size=101&sortBy=clickCount&direction=ASC"))
                .andExpect(status().isOk());

        verify(urlService).getAllUrls(0, 101, "clickCount", "ASC");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "?page=-1",
            "?size=0",
            "?size=102",
            "?sortBy=originalUrl",
            "?direction=sideways"
    })
    void getAllUrls_invalidQueryParameters_returnsBadRequest(String query) throws Exception {
        mockMvc.perform(get("/api/v1/urls" + query))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlService);
    }
}
