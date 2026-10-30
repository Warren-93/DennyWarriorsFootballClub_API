package com.clubsite.api.media;

import com.clubsite.api.TestProperties;
import com.clubsite.api.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaAssetRepository repository;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(repository, TestProperties.defaults());
    }

    @Test
    void acceptsAllowedImageType() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MediaAsset saved = mediaService.upload(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3}));

        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getFilename()).isEqualTo("photo.png");
        assertThat(saved.getSize()).isEqualTo(3);
        assertThat(saved.getData()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsDisallowedContentType() {
        var file = new MockMultipartFile("file", "page.html", "text/html", new byte[]{1});

        assertThatThrownBy(() -> mediaService.upload(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void rejectsOversizedFile() {
        var file = new MockMultipartFile("file", "big.png", "image/png", new byte[6 * 1024 * 1024]);

        assertThatThrownBy(() -> mediaService.upload(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void rejectsEmptyFile() {
        var file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> mediaService.upload(file)).isInstanceOf(ApiException.class);
    }
}
