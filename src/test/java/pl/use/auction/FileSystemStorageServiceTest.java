package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pl.use.auction.service.FileSystemStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemStorageServiceTest {

    private final FileSystemStorageService fileSystemStorageService = new FileSystemStorageService();

    @Test
    void saveImageShouldStoreFileSuccessfully() throws IOException {
        String originalFilename = "testImage.jpg";
        MultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                "image/jpeg",
                "Test image content".getBytes()
        );
        Path uploadDir = Paths.get("src/test/resources/static/auctionImages/");
        Path expectedFilePath = uploadDir.resolve(originalFilename);

        Files.createDirectories(uploadDir);

        FileSystemStorageService fileSystemStorageService = new FileSystemStorageService();

        String savedImagePath = fileSystemStorageService.save(file, uploadDir.toString());

        assertEquals("auctionImages/" + originalFilename, savedImagePath);
        assertTrue(Files.exists(expectedFilePath));

        Files.deleteIfExists(expectedFilePath);
    }
}