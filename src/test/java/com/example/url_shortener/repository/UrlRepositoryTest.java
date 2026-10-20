package com.example.url_shortener.repository;

import com.example.url_shortener.entity.Url;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UrlRepositoryTest {

    @Autowired
    private UrlRepository urlRepository;

    @Test
    void findByShortCode_existingCode_returnsMatchingUrl() {
        Url savedUrl = urlRepository.saveAndFlush(createUrl("Ab12xY"));

        Optional<Url> result = urlRepository.findByShortCode("Ab12xY");

        assertTrue(result.isPresent());
        assertEquals(savedUrl.getId(), result.get().getId());
        assertEquals("https://example.com/Ab12xY", result.get().getOriginalUrl());
    }

    @Test
    void findByShortCode_unknownCode_returnsEmpty() {
        assertTrue(urlRepository.findByShortCode("missing").isEmpty());
    }

    @Test
    void existsByShortCode_existingCode_returnsTrue() {
        urlRepository.saveAndFlush(createUrl("Ab12xY"));

        assertTrue(urlRepository.existsByShortCode("Ab12xY"));
        assertFalse(urlRepository.existsByShortCode("missing"));
    }

    @Test
    void save_duplicateShortCode_violatesUniqueConstraint() {
        urlRepository.saveAndFlush(createUrl("Ab12xY"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> urlRepository.saveAndFlush(createUrl("Ab12xY")));
    }

    private Url createUrl(String shortCode) {
        Url url = new Url();
        url.setShortCode(shortCode);
        url.setOriginalUrl("https://example.com/" + shortCode);
        url.setCreatedAt(LocalDateTime.of(2026, 10, 20, 12, 0));
        url.setClickCount(0);
        return url;
    }
}
