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

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerUpdater;
import sun.misc.Unsafe;

@SuppressWarnings("unused") // used by UnsafeStore
public class UnsafeHook {
  // sorry-not-sorry, not keeping this private like we're supposed to.
  // (that would be a concern if this was a public library).
  public static final Unsafe unsafe = Unsafe.getUnsafe();

  private static final long BYTE_ARRAY_BASE_OFFSET = unsafe.arrayBaseOffset(byte[].class);

  public static void putByte(Unsafe unsafe, Object target, long offset, byte value,
      TrackerPoint source) {
    unsafe.putByte(target, offset, value);
    if (target instanceof byte[]) {
      int index = (int) (offset - BYTE_ARRAY_BASE_OFFSET);
      TrackerUpdater.setSourceTrackerPoint(context(), target, index, 1, source);
    }
  }
}
