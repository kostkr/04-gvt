package uj.wmii.pwj.gvt;

import org.junit.jupiter.api.*;
import org.mockito.Mock;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GvtTest {

    private ByteArrayOutputStream out;

    @Mock
    private ExitHandler eh = mock(ExitHandler.class);

    @BeforeEach
    void prepareOutput() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out = new ByteArrayOutputStream(512);
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    private void executeRuntime(String command, String failMessage) {
        try {
            Runtime.getRuntime().exec(command);
            Thread.sleep(100);
        } catch (IOException | InterruptedException e) {
            fail(failMessage, e);
        }
    }

    private static void safeDelete(Path... paths) {
        for (var p: paths) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @AfterAll
    static void cleanUp() {
        Path gvtPath = Path.of(".gvt");
        try {
            Files.walk(gvtPath)
                .sorted(Comparator.reverseOrder())
                .forEach(GvtTest::safeDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
        safeDelete(Path.of("a.txt"));
        safeDelete(Path.of("b.txt"));
        safeDelete(Path.of("c.txt"));
        safeDelete(Path.of("d.txt"));
    }

    @Test
    @Order(1)
    public void invokeEmptyCommand() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal();
        verify(eh, times(1)).exit(1, "Please specify command.");
    }

    @Test
    @Order(2)
    public void addFileToNotInitializedRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("add", "a.txt");
        verify(eh, times(1)).exit(-2, "Current directory is not initialized. Please use init command to initialize.");
    }

    @Test
    @Order(3)
    public void detachFileFromNotInitializedRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("detach", "a.txt");
        verify(eh, times(1)).exit(-2, "Current directory is not initialized. Please use init command to initialize.");
    }

    @Test
    @Order(4)
    public void checkoutVersionNotInitializedRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("checkout", "1");
        verify(eh, times(1)).exit(-2, "Current directory is not initialized. Please use init command to initialize.");
    }

    @Test
    @Order(5)
    public void commitFileToNotInitializedRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("commit", "a.txt");
        verify(eh, times(1)).exit(-2, "Current directory is not initialized. Please use init command to initialize.");
    }

    @Test
    @Order(6)
    public void historyOfNotInitializedRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("history");
        verify(eh, times(1)).exit(-2, "Current directory is not initialized. Please use init command to initialize.");
    }

    @Test
    @Order(7)
    public void versionOfNotInitializedRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(-2, "Current directory is not initialized. Please use init command to initialize.");
    }

    @Test
    @Order(8)
    public void initializeRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("init");
        verify(eh, times(1)).exit(0, "Current directory initialized successfully.");
    }

    @Test
    @Order(9)
    public void checkVersion0() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 0\nGVT initialized.");
    }

    @Test
    @Order(10)
    public void addNotExistingFileToInitializedRepo() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("add", "a.txt");
        verify(eh, times(1)).exit(21, "File not found. File: a.txt");
    }


    @Test
    @Order(11)
    public void addFileToRepo() {
        executeRuntime("touch a.txt", "File a.txt cannot be created");
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("add", "a.txt");
        verify(eh, times(1)).exit(0, "File added successfully. File: a.txt");
    }

    @Test
    @Order(12)
    public void checkVersion1() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 1\nFile added successfully. File: a.txt");
    }

    @Test
    @Order(13)
    public void addSecondFileToRepo() {
        executeRuntime("touch b.txt", "File b.txt cannot be created");
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("add", "b.txt");
        verify(eh, times(1)).exit(0, "File added successfully. File: b.txt");
    }

    @Test
    @Order(14)
    public void checkVersion2() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 2\nFile added successfully. File: b.txt");
    }

    @Test
    @Order(15)
    public void tryAddAlreadyAddedFile() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("add", "a.txt");
        verify(eh, times(1)).exit(0, "File already added. File: a.txt");
    }

    @Test
    @Order(16)
    public void checkVersion2Again() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 2\nFile added successfully. File: b.txt");
    }

    @Test
    @Order(17)
    public void addFileWithCustomMessage() {
        executeRuntime("touch c.txt", "File c.txt cannot be created");
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("add", "c.txt", "-m", "Adding C FILE");
        verify(eh, times(1)).exit(0, "File added successfully. File: c.txt");
    }

    @Test
    @Order(18)
    public void checkVersion3() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 3\nAdding C FILE");
    }

    @Test
    @Order(19)
    public void detachNoFileName() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("detach");
        verify(eh, times(1)).exit(30, "Please specify file to detach.");
    }

    @Test
    @Order(20)
    public void detachNotExistingFile() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("detach", "x");
        verify(eh, times(1)).exit(0, "File is not added to gvt. File: x");
    }


    @Test
    @Order(21)
    public void detachExistingFile() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("detach", "b.txt");
        verify(eh, times(1)).exit(0, "File detached successfully. File: b.txt");
    }

    @Test
    @Order(22)
    public void detachJustDetachedFile() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("detach", "b.txt");
        verify(eh, times(1)).exit(0, "File is not added to gvt. File: b.txt");
    }

    @Test
    @Order(23)
    public void checkVersion4() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 4\nFile detached successfully. File: b.txt");
    }


    @Test
    @Order(24)
    public void addAgainDetachedExistingFile() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("add", "b.txt");
        verify(eh, times(1)).exit(0, "File added successfully. File: b.txt");
    }

    @Test
    @Order(25)
    public void checkVersion5() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 5\nFile added successfully. File: b.txt");
    }

    @Test
    @Order(26)
    public void tryCommitNoFile() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("commit");
        verify(eh, times(1)).exit(50, "Please specify file to commit.");
    }


    @Test
    @Order(27)
    public void tryCommitNotExistingFile() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("commit", "d.txt");
        verify(eh, times(1)).exit(51, "File not found. File: d.txt");
    }


    @Test
    @Order(28)
    public void tryCommitNotAddedFile() {
        executeRuntime("touch d.txt", "File d.txt cannot be created");
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("commit", "d.txt");
        verify(eh, times(1)).exit(0, "File is not added to gvt. File: d.txt");
    }

    @Test
    @Order(29)
    public void modifyAndCommitFile() {
        try {
            Files.writeString(Path.of("b.txt"), "Ala ma kota");
        } catch (IOException e) {
            fail("Cannot modify file b.txt", e);
        }
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("commit", "b.txt");
        verify(eh, times(1)).exit(0, "File committed successfully. File: b.txt");
    }

    @Test
    @Order(30)
    public void checkVersion6() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 6\nFile committed successfully. File: b.txt");
    }

    @Test
    @Order(31)
    public void modifyAndCommitFileWithCustomMessage() {
        try {
            Files.writeString(Path.of("b.txt"), "Ala ma kota\nPonownie!");
        } catch (IOException e) {
            fail("Cannot modify file b.txt", e);
        }
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("commit", "b.txt", "-m", "Again modified b.txt\nWith two lines!\nOr even three!");
        verify(eh, times(1)).exit(0, "File committed successfully. File: b.txt");
    }

    @Test
    @Order(32)
    public void checkVersion7() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("version");
        verify(eh, times(1)).exit(0, "Version: 7\nAgain modified b.txt\nWith two lines!\nOr even three!");
    }

    @Test
    @Order(33)
    public void historyLast2Version() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("history", "-last", "2");
        verify(eh, times(1)).exit(0,
    """
             7: Again modified b.txt
             6: File committed successfully. File: b.txt
             """);
    }

    @Test
    @Order(34)
    public void historyAll() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("history");
        verify(eh, times(1)).exit(0,
            """
            7: Again modified b.txt
            6: File committed successfully. File: b.txt
            5: File added successfully. File: b.txt
            4: File detached successfully. File: b.txt
            3: Adding C FILE
            2: File added successfully. File: b.txt
            1: File added successfully. File: a.txt
            0: GVT initialized.
            """);
    }


    @Test
    @Order(35)
    public void checkoutInvalidVersion() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("checkout", "20");
        verify(eh, times(1)).exit(60, "Invalid version number: 20");
    }

    @Test
    @Order(36)
    public void checkoutVersion2() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("checkout", "2");
        verify(eh, times(1)).exit(0, "Checkout successful for version: 2");
        try {
            assertThat(Files.readString(Path.of("b.txt"))).isBlank();
        } catch (IOException e) {
            fail("Cannot read file b.txt", e);
        }
    }

    @Test
    @Order(37)
    public void checkoutVersion7() {
        Gvt gvt = new Gvt(eh);
        gvt.mainInternal("checkout", "7");
        verify(eh, times(1)).exit(0, "Checkout successful for version: 7");
        try {
            assertThat(Files.readString(Path.of("b.txt"))).isEqualTo("Ala ma kota\nPonownie!");
        } catch (IOException e) {
            fail("Cannot read file b.txt", e);
        }
    }

}
