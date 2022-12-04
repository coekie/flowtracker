package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Test instrumentation of {@link FileInputStream} */
public class FileInputStreamTest extends AbstractInputStreamTest {
  private static File file;

  @BeforeClass public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_InputStreamHookTest", "").toFile();
    try (var out = new FileOutputStream(file)) {
      out.write(new byte[]{'a', 'b', 'c'});
    }
  }

  @AfterClass public static void removeTmpFile() {
    assertTrue(file.delete());
  }

  @Override
  InputStream createInputStream(byte[] bytes) throws IOException {
    try (var out = new FileOutputStream(file)) {
      out.write(bytes);
    }
    return new FileInputStream(file);
  }

  @Test public void descriptor() throws IOException {
    try (var fisFromFile = new FileInputStream(file)) {
      TrackTestHelper.assertDescriptor(fisFromFile, "FileInputStream for " + file.getPath(), null);
    }

    try (var fisFromName = new FileInputStream(file.getPath())) {
      TrackTestHelper.assertDescriptor(fisFromName, "FileInputStream for " + file.getPath(), null);
    }
  }
}