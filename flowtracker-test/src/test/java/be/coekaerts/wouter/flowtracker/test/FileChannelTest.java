package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ChannelTrackerRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileChannelTest {
  private static File file;

  @BeforeClass
  public static void createTmpFile() throws IOException {
    file = Files.createTempFile("flowtracker_FileChannelTest", "").toFile();
    try (var out = new FileOutputStream(file)) {
      out.write(new byte[]{'a', 'b', 'c'});
    }
  }

  @AfterClass
  public static void removeTmpFile() {
    assertTrue(file.delete());
  }

  @Test
  public void descriptor() throws IOException {
    try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
      TrackTestHelper.assertInterestAndDescriptor(ChannelTrackerRepository.getReadTracker(channel),
          "Read channel for " + file.getPath(), null);
    }
  }

  @Test
  public void read() throws IOException {
    try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
      ByteBuffer bb = ByteBuffer.allocate(10);
      channel.read(bb);
      // TODO assert channel tracker content and bb tracker
    }
  }

  // TODO more...
}
