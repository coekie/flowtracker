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
 * Tracker for reading from a source of chars or Strings.
 *
 * @see ByteOriginTracker
 * @see CharSinkTracker
 */
public class CharOriginTracker extends OriginTracker implements CharContentTracker {

  private final StringBuilder content = new StringBuilder();

  public void append(char c) {
    beforeAppend();
    content.append(c);
  }

  public void append(char[] cbuf, int offset, int len) {
    beforeAppend();
    content.append(cbuf, offset, len);
  }

  public void append(CharSequence charSequence) {
    beforeAppend();
    content.append(charSequence);
  }

  @Override public CharSequence getContent() {
    return content;
  }

  @Override
  public int getLength() {
    return content.length();
  }
}
