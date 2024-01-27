package pl.use.auction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import pl.use.auction.controller.FileController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private Resource mockResource;
    private FileController fileController;
    private final Path rootLocation = Paths.get("src/test/resources/static/auctionImages");
    private final String testFilename = "testImage.png";
    private final Path testFilePath = rootLocation.resolve(testFilename);

    @BeforeEach
    void setUp() throws Exception {
        Path testRootLocation = Paths.get("src/test/resources/static/auctionImages");
        fileController = new FileController(testRootLocation);
        Files.createDirectories(testRootLocation);
        if (!Files.exists(testFilePath)) {
            Files.copy(Paths.get("src/test/resources/" + testFilename), testFilePath);
        }
    }

    @Test
    void serveFile_WhenFileExists() throws Exception {
        ResponseEntity<Resource> response = fileController.serveFile(testFilename);

        assertEquals(200, response.getStatusCode().value());

        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertEquals("inline; filename=\"" + testFilename + "\"", contentDisposition);
    }

    @Test
    void serveFile_WhenFileDoesNotExist() {
        ResponseEntity<Resource> response = fileController.serveFile("nonExistentFile.jpg");

        assertEquals(404, response.getStatusCode().value());
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(testFilePath);
    }
}
