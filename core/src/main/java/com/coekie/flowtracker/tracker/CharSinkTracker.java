package com.coekie.flowtracker.tracker;

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

/**
 * Tracker for writing to a sink of chars.
 *
 * @see CharOriginTracker
 * @see ByteSinkTracker
 */
public class CharSinkTracker extends DefaultTracker implements CharContentTracker {
  private final StringBuilder content = new StringBuilder();
  private TwinSynchronization twinSync;

  @Override public CharSequence getContent() {
    return content;
  }

  @Override public int getLength() {
    return content.length();
  }

  public void append(char c) {
    beforeAppend();
    content.append(c);
  }

  public void append(char[] cbuf, int off, int len) {
    beforeAppend();
    content.append(cbuf, off, len);
  }

  public void append(String str, int off, int len) {
    beforeAppend();
    content.append(str, off, off + len);
  }

  @Override
  public void initTwin(Tracker twin) {
    super.initTwin(twin);
    if (twin instanceof OriginTracker) {
      twinSync = ((OriginTracker) twin).twinSync();
    }
  }

  private void beforeAppend() {
    if (twinSync != null) {
      twinSync.beforeAppend(this);
    }
  }
}
