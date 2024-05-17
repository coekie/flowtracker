package demo;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ByteContentTracker;
import com.coekie.flowtracker.tracker.ByteSequence;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import com.coekie.flowtracker.tracker.TrackerTree.Node;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;
import com.google.common.truth.StringSubject;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.rules.ExternalResource;

public class DemoTestRule extends ExternalResource {
  private final ByteSequence bout = new ByteSequence();
  private PrintStream originalOut;
  private final long trackerIdAtStart = Tracker.nextId();

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

  /** Find a new Tracker that was created since this test started */
  private Tracker findNewTracker(Node parent, Predicate<Tracker> predicate) {
    List<Tracker> found = trackers(parent)
        .filter(t -> t.getTrackerId() > trackerIdAtStart)
        .filter(predicate)
        .collect(Collectors.toList());
    return Iterables.getOnlyElement(found);
  }

  /** Find a new ByteSinkTracker that was created since this test started */
  TrackerSubject newSink(Node parent) {
    Tracker tracker = findNewTracker(parent, t -> t instanceof ByteSinkTracker);
    return new TrackerSubject(tracker);
  }

  /** Returns all trackers nested (recursive) under the given node */
  private static Stream<Tracker> trackers(Node node) {
    return Stream.concat(
        node.trackers().stream(),
        node.children().stream().flatMap(DemoTestRule::trackers));
  }

  /** Returns a TrackerSubject for what the test wrote to System.out */
  TrackerSubject out() {
    return new TrackerSubject(outTracker(), bout);
  }

  // like a Truth Subject, but too lazy to actually be one.
  static class TrackerSubject {
    private final Tracker tracker;
    private final ByteSequence content;

    private TrackerSubject(Tracker tracker, ByteSequence content) {
      this.tracker = tracker;
      this.content = content;
    }

    private TrackerSubject(Tracker tracker) {
      this.tracker = tracker;
      this.content = ((ByteContentTracker) tracker).getContent();
    }

    private TrackerSnapshot snapshotOutput(byte[] prefix, byte[] expectedOutput) {
      byte[] output = content.toByteArray();

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
    TrackedSubject assertThatPart(String expectedOutput) {
      return assertThatPart(expectedOutput.getBytes());
    }

    /**
     * Checks if the output contains `expectedOutput`, after where it contains `prefix`, and returns
     * the TrackedSubject for that part of the output.
     */
    TrackedSubject assertThatPart(String prefix, String expectedOutput) {
      return assertThatPart(prefix.getBytes(), expectedOutput.getBytes());
    }

    TrackedSubject assertThatPart(byte[] expectedOutput) {
      return assertThatPart(null, expectedOutput);
    }

    TrackedSubject assertThatPart(byte[] prefix, byte[] expectedOutput) {
      return new TrackedSubject(snapshotOutput(prefix, expectedOutput));
    }
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
