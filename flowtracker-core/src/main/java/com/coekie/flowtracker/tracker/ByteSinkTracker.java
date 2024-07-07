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

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Tracker for writing to a sink of bytes, e.g. for an {@link OutputStream}.
 *
 * @see ByteOriginTracker
 * @see CharSinkTracker
 */
public class ByteSinkTracker extends DefaultTracker implements ByteContentTracker {
  private final ByteSequence content = new ByteSequence();
  private TwinSynchronization twinSync;

  @Override
  public int getLength() {
    return content.size();
  }

  public void append(byte b) {
    beforeAppend();
    content.write(b);
  }

  public void append(byte[] cbuf, int offset, int len) {
    beforeAppend();
    content.write(cbuf, offset, len);
  }

  public ByteBuffer getByteContent() {
    return content.getByteContent();
  }

  @Override
  public ByteSequence getContent() {
    return content;
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
