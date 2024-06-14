package com.coekie.flowtracker.weaver.flow;

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

import java.util.Objects;

/**
 * A reference to a slot containing a FlowValue in a FlowFrame. Note that as analysis proceeds,
 * the FlowValue that this points to can still change.
 */
class ValueReference {
  final FlowFrame frame;
  final boolean isLocal;
  final int index;

  private ValueReference(FlowFrame frame, boolean isLocal, int index) {
    this.frame = frame;
    this.isLocal = isLocal;
    this.index = index;
  }

  static ValueReference local(FlowFrame frame, int index) {
    return new ValueReference(frame, true, index);
  }

  static ValueReference stack(FlowFrame frame, int index) {
    return new ValueReference(frame, false, index);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValueReference)) {
      return false;
    }
    ValueReference that = (ValueReference) o;
    return frame == that.frame && isLocal == that.isLocal && index == that.index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(frame, isLocal, index);
  }
}
