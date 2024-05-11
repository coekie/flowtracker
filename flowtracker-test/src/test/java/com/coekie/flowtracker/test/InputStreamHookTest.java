package com.coekie.flowtracker.test;

import com.coekie.flowtracker.hook.InputStreamHook;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.google.common.truth.Truth;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import org.junit.Test;

public class InputStreamHookTest {
  @Test public void getInputStreamTracker_filterInputSteam() {
    ByteArrayInputStream source = new ByteArrayInputStream(new byte[0]);
    Tracker sourceTracker = TrackerRepository.createTracker(source);

    BufferedInputStream filterStream = new BufferedInputStream(source);
    Truth.assertThat(InputStreamHook.getInputStreamTracker(filterStream)).isSameInstanceAs(sourceTracker);
  }
}
