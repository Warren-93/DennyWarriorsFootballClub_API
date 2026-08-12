package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.InvalidMediaException;
import mark.warren93.dev.DennyWarriorsAPI.model.MediaAsset;
import mark.warren93.dev.DennyWarriorsAPI.repository.MediaAssetRepository;
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
    private MediaAssetRepository mediaAssetRepository;

    private MediaService mediaService;

    private MediaService service() {
        return new MediaService(mediaAssetRepository);
    }

    @Test
    void acceptsAllowedImageType() {
        mediaService = service();
        when(mediaAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        MediaAsset saved = mediaService.upload(file);

        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getFilename()).isEqualTo("photo.png");
        assertThat(saved.getSize()).isEqualTo(3);
        assertThat(saved.getData()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsDisallowedContentType() {
        mediaService = service();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> mediaService.upload(file))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void rejectsOversizedFile() {
        mediaService = service();
        byte[] tooLarge = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", tooLarge);

        assertThatThrownBy(() -> mediaService.upload(file))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void rejectsEmptyFile() {
        mediaService = service();
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> mediaService.upload(file))
                .isInstanceOf(InvalidMediaException.class);
    }
}
