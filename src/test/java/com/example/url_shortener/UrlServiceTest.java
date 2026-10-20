package com.example.url_shortener;

import com.example.url_shortener.dto.CreateUrlResponse;
import com.example.url_shortener.dto.UrlPageResponse;
import com.example.url_shortener.dto.UrlStatsResponse;
import com.example.url_shortener.entity.Url;
import com.example.url_shortener.exception.ShortUrlNotFoundException;
import com.example.url_shortener.repository.UrlRepository;
import com.example.url_shortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    @Test
    void createShortUrl_availableCode_savesInitializedUrlAndReturnsResponse() {
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String originalUrl = "https://example.com/path";

        CreateUrlResponse response = urlService.createShortUrl(originalUrl);

        ArgumentCaptor<Url> urlCaptor = ArgumentCaptor.forClass(Url.class);
        verify(urlRepository).save(urlCaptor.capture());
        Url savedUrl = urlCaptor.getValue();
        assertTrue(savedUrl.getShortCode().matches("[A-Za-z0-9]{6}"));
        assertEquals(originalUrl, savedUrl.getOriginalUrl());
        assertEquals(0, savedUrl.getClickCount());
        assertNotNull(savedUrl.getCreatedAt());

        assertEquals(savedUrl.getShortCode(), response.getShortCode());
        assertEquals("http://localhost:8080/" + savedUrl.getShortCode(), response.getShortUrl());
        assertEquals(originalUrl, response.getOriginalUrl());
        assertEquals(savedUrl.getCreatedAt(), response.getCreatedAt());
        assertEquals(0, response.getClickCount());
        verify(urlRepository).existsByShortCode(savedUrl.getShortCode());
    }

    @Test
    void createShortUrl_codeCollision_retriesUntilAvailable() {
        when(urlRepository.existsByShortCode(anyString())).thenReturn(true, false);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUrlResponse response = urlService.createShortUrl("https://example.com");

        verify(urlRepository, times(2)).existsByShortCode(anyString());
        verify(urlRepository).save(argThat(url -> url.getShortCode().equals(response.getShortCode())));
    }

    @Test
    void createShortUrl_repositorySaveFails_propagatesException() {
        IllegalStateException repositoryFailure = new IllegalStateException("Database unavailable");
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenThrow(repositoryFailure);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> urlService.createShortUrl("https://example.com"));

        assertSame(repositoryFailure, thrown);
    }

    @Test
    void getOriginalUrl_existingCode_incrementsClicksAndReturnsOriginalUrl() {
        Url url = createUrl("Ab12xY", "https://example.com", 4);
        when(urlRepository.findByShortCode("Ab12xY")).thenReturn(Optional.of(url));

        String originalUrl = urlService.getOriginalUrl("Ab12xY");

        assertEquals("https://example.com", originalUrl);
        assertEquals(5, url.getClickCount());
        verify(urlRepository).save(url);
    }

    @Test
    void getOriginalUrl_unknownCode_throwsNotFoundWithoutSaving() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        ShortUrlNotFoundException exception = assertThrows(
                ShortUrlNotFoundException.class,
                () -> urlService.getOriginalUrl("missing"));

        assertEquals("Short URL not found", exception.getMessage());
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void createUrlStats_existingCode_returnsUrlStatistics() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 20, 12, 0);
        Url url = createUrl("Ab12xY", "https://example.com", 7);
        url.setCreatedAt(createdAt);
        when(urlRepository.findByShortCode("Ab12xY")).thenReturn(Optional.of(url));

        UrlStatsResponse response = urlService.createUrlStats("Ab12xY");

        assertEquals("Ab12xY", response.getShortCode());
        assertEquals("https://example.com", response.getOriginalUrl());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(7, response.getClickCount());
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void createUrlStats_unknownCode_throwsNotFound() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class, () -> urlService.createUrlStats("missing"));
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void getAllUrls_pageAndSortArguments_returnsMappedPage() {
        Url url = createUrl("Ab12xY", "https://example.com", 3);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 20, 12, 0);
        url.setCreatedAt(createdAt);
        Pageable expectedPageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "clickCount"));
        when(urlRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(url), expectedPageable, 11));

        UrlPageResponse response = urlService.getAllUrls(2, 5, "clickCount", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(urlRepository).findAll(pageableCaptor.capture());
        assertEquals(expectedPageable, pageableCaptor.getValue());
        assertEquals(2, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(11, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(1, response.getContents().size());
        UrlStatsResponse mappedUrl = response.getContents().get(0);
        assertEquals("Ab12xY", mappedUrl.getShortCode());
        assertEquals("https://example.com", mappedUrl.getOriginalUrl());
        assertEquals(createdAt, mappedUrl.getCreatedAt());
        assertEquals(3, mappedUrl.getClickCount());
    }

    @Test
    void getAllUrls_emptyRepositoryPage_returnsEmptyContentsAndPageMetadata() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(urlRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        UrlPageResponse response = urlService.getAllUrls(0, 10, "createdAt", "desc");

        assertTrue(response.getContents().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
    }

    private Url createUrl(String shortCode, String originalUrl, int clickCount) {
        Url url = new Url();
        url.setShortCode(shortCode);
        url.setOriginalUrl(originalUrl);
        url.setCreatedAt(LocalDateTime.of(2026, 10, 19, 9, 30));
        url.setClickCount(clickCount);
        return url;
    }
}
