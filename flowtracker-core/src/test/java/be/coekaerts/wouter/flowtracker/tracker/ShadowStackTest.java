package be.coekaerts.wouter.flowtracker.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import be.coekaerts.wouter.flowtracker.tracker.ShadowStack.Frame;
import org.junit.Test;

public class ShadowStackTest {
  @Test
  public void testReturnValue() {
    Tracker tracker = new CharOriginTracker();

    Frame callingReadFrame = ShadowStack.calling("read");
    // inside the called read() method:
    {
      Frame readFrame = ShadowStack.start("read");
      assertSame(callingReadFrame, readFrame);
      // assert readFrame == callingReadFrame
      Frame.returning(readFrame, tracker, 2);
    }

    // then in the calling method we can access callingReadFrame.returnedTrackerPoint
    assertEquals(tracker, callingReadFrame.returnTracker);
    assertEquals(2, callingReadFrame.returnIndex);
  }

  @Test
  public void testStartWithoutCalling() {
    assertNull(ShadowStack.start("read"));
  }
}
