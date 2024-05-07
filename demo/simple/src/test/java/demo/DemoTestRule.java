package demo;

import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.ByteSequence;
import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot;
import com.google.common.primitives.Bytes;
import com.google.common.truth.StringSubject;
import java.io.PrintStream;
import java.util.Arrays;
import org.junit.rules.ExternalResource;

public class DemoTestRule extends ExternalResource {
  private final ByteSequence bout = new ByteSequence();
  private PrintStream originalOut;

  @Override
  protected void before() {
    originalOut = System.out;
    System.setOut(new PrintStream(bout));
  }

  @Override
  protected void after() {
    System.setOut(originalOut);
  }

  private Tracker outTracker() {
    return TrackerRepository.getTracker(bout.getByteContent().array());
  }

  private TrackerSnapshot snapshotOutput(byte[] prefix, byte[] expectedOutput) {
    Tracker tracker = outTracker();
    byte[] output = bout.toByteArray();

    int startIndex;
    if (prefix == null) {
      startIndex = 0;
    } else {
      startIndex = indexOf(output, prefix) + prefix.length;
    }

    int index =
        startIndex + indexOf(Arrays.copyOfRange(output, startIndex, output.length), expectedOutput);
    return TrackerSnapshot.of(tracker, index, expectedOutput.length).simplify();
  }

  private int indexOf(byte[] output, byte[] expected) {
    int index = Bytes.indexOf(output, expected);
    if (index == -1) {
      throw new AssertionError("Cannot find '" + new String(expected) + "' in '"
          + new String(output) + "'");
    }
    return index;
  }

  /**
   * Checks if the output contains the given String, and returns the TrackedSubject for that part
   * of the output.
   */
  TrackedSubject assertThatOutput(String expectedOutput) {
    return assertThatOutput(expectedOutput.getBytes());
  }

  /**
   * Checks if the output contains `expectedOutput`, after where it contains `prefix`, and returns
   * the TrackedSubject for that part of the output.
   */
  TrackedSubject assertThatOutput(String prefix, String expectedOutput) {
    return assertThatOutput(prefix.getBytes(), expectedOutput.getBytes());
  }

  TrackedSubject assertThatOutput(byte[] expectedOutput) {
    return assertThatOutput(null, expectedOutput);
  }

  TrackedSubject assertThatOutput(byte[] prefix, byte[] expectedOutput) {
    return new TrackedSubject(snapshotOutput(prefix, expectedOutput));
  }

  // like a Truth Subject, but too lazy to actually be one.
  static class TrackedSubject {
    private final TrackerSnapshot snapshot;

    private TrackedSubject(TrackerSnapshot snapshot) {
      this.snapshot = snapshot;
    }

    private Tracker tracker() {
      assertThat(snapshot.getParts()).hasSize(1);
      return snapshot.getParts().get(0).source;
    }

    void isNotTracked() {
      assertThat(tracker()).isNull();
    }

    StringSubject comesFromConstantInClassThat() {
      ClassOriginTracker tracker = (ClassOriginTracker) tracker();
      return assertThat(tracker.getContent().toString());
    }

    void comesFromConstantInClass(Class<?> source) {
      comesFromConstantInClassThat()
          .startsWith("class " + source.getName() + "\n");
    }
  }
}
