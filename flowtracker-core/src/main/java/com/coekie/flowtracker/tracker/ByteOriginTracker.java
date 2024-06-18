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

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Tracker for reading from a source of bytes, e.g. for an {@link InputStream}s.
 *
 * @see CharOriginTracker
 * @see ByteSinkTracker
 */
public class ByteOriginTracker extends OriginTracker implements ByteContentTracker {
  private final ByteSequence content = new ByteSequence();

  public void append(byte b) {
    content.write(b);
  }

  public void append(byte[] cbuf, int offset, int len) {
    content.write(cbuf, offset, len);
  }

  public ByteBuffer getByteContent() {
    return content.getByteContent();
  }

  @Override
  public int getLength() {
    return content.size();
  }

  @Override
  public ByteSequence getContent() {
    return content;
  }
}
