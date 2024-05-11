package com.coekie.flowtracker.hook;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class InputStreamHook {
  private static final Field filterInputStreamIn =
      Reflection.getDeclaredField(FilterInputStream.class, "in");

  /** Returns the tracker of an input stream, ignoring wrapping FilterInputStreams */
  public static Tracker getInputStreamTracker(InputStream stream) {
    Tracker tracker = TrackerRepository.getTracker(stream);
    if (tracker != null) {
      return tracker;
    } else if (stream instanceof FilterInputStream) {
      return getInputStreamTracker(
          (InputStream) Reflection.getFieldValue(stream, filterInputStreamIn));
    } else if (stream instanceof FileInputStream) {
      try {
        return FileDescriptorTrackerRepository.getReadTracker(((FileInputStream) stream).getFD());
      } catch (IOException e) {
        return null;
      }
    } else {
      return null;
    }
  }
}
