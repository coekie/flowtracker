package be.coekaerts.wouter.flowtracker.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FileInputStreamHookTest {
  private static File file;

  @BeforeClass public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_InputStreamHookTest", "txt").toFile();
  }

  @AfterClass public static void removeTmpFile() {
    assertTrue(file.delete());
  }

  @Test public void descriptor() throws IOException {
    FileInputStream fisFromFile = new FileInputStream(file);
    TrackTestHelper.assertDescriptor(fisFromFile, "FileInputStream for " + file.getPath(), null);

    FileInputStream fisFromName = new FileInputStream(file.getPath());
    TrackTestHelper.assertDescriptor(fisFromName, "FileInputStream for " + file.getPath(), null);
  }
}
