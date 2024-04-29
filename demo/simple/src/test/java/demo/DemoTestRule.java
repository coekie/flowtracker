package demo;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import be.coekaerts.wouter.flowtracker.tracker.ByteSequence;
import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot;
import com.google.common.truth.StringSubject;
import java.io.PrintStream;
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

  private String outContent() {
    return bout.toString();
  }

  private TrackerSnapshot snapshotOutput(String expectedOutput) {
    Tracker tracker = outTracker();
    String output = outContent();
    int index = output.indexOf(expectedOutput);
    assertWithMessage("Find %s in %s", expectedOutput, output).that(index).isGreaterThan(-1);
    return TrackerSnapshot.of(tracker, index, expectedOutput.length());
  }

  /**
   * Checks if the output contains the given String, and returns the Tracker that is tracked as its
   * source in the output.
   */
  private Tracker trackerForOutput(String expectedOutput) {
    TrackerSnapshot snapshot = snapshotOutput(expectedOutput);
    assertThat(snapshot.getParts()).hasSize(1);
    return snapshot.getParts().get(0).source;
  }

  void assertOutputComesFromConstantIn(String expectedOutput, Class<?> source) {
    assertOutputComesFromConstantInClassThat(expectedOutput)
        .startsWith("class " + source.getName() + "\n");
  }

  StringSubject assertOutputComesFromConstantInClassThat(String expectedOutput) {
    ClassOriginTracker tracker = (ClassOriginTracker) trackerForOutput(expectedOutput);
    return assertThat(tracker.getContent().toString());
  }

  void assertOutputNotTracked(String expectedOutput) {
    assertThat(trackerForOutput(expectedOutput)).isNull();
  }
}
