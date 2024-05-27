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

import com.coekie.flowtracker.tracker.ClassOriginTracker;

@SuppressWarnings("unused")
public class ClassHook {
  /** Hook for {@link Class#getName()}, used in `ClassNameCall` */
  public static String getName(Class<?> clazz) {
    String name = clazz.getName();
    return StringHook.constantString(clazz.getName(), ClassOriginTracker.get(clazz),
        6 /* after "class " */);
  }
}
