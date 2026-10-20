
package com.example.url_shortener;

import com.example.url_shortener.entity.Url;
import com.example.url_shortener.repository.UrlRepository;
import com.example.url_shortener.service.UrlService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    @Test
    void createShortUrlShouldSaveOriginalUrl() {

        when(urlRepository.existsByShortCode(any(String.class)))
                .thenReturn(false);

        when(urlRepository.save(any(Url.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String originalUrl = "https://example.com";

        urlService.createShortUrl(originalUrl);

        verify(urlRepository, atLeastOnce())
                .existsByShortCode(any(String.class));

        verify(urlRepository).save(argThat(url ->
                url.getOriginalUrl().equals(originalUrl)
                        && url.getShortCode() != null
                        && url.getShortCode().length() == 6
        ));
    }
}