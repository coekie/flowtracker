package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.hook.InputStreamHook;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import org.junit.Test;

public class InputStreamHookTest {
  @Test public void getInputStreamTracker_filterInputSteam() {
    ByteArrayInputStream source = new ByteArrayInputStream(new byte[0]);
    Tracker sourceTracker = TrackerRepository.createTracker(source);

    BufferedInputStream filterStream = new BufferedInputStream(source);
    assertThat(InputStreamHook.getInputStreamTracker(filterStream)).isSameInstanceAs(sourceTracker);
  }
}
