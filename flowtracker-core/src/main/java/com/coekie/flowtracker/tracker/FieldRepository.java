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

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Keeps track of the source of values of fields */
public class FieldRepository {
  private static final Map<Object, Map<String, TrackerPoint>> objectToFieldMap =
      Collections.synchronizedMap(new IdentityHashMap<>());

  public static TrackerPoint getPoint(Object target, String fieldId) {
    Map<String, TrackerPoint> fieldMap = objectToFieldMap.get(target);
    return fieldMap == null ? null : fieldMap.get(fieldId);
  }

  @SuppressWarnings("unused") // invoked from FieldStore
  public static void setPoint(Object target, String fieldId, TrackerPoint point) {
    if (target != null) {
      Map<String, TrackerPoint> fieldMap =
          objectToFieldMap.computeIfAbsent(target, k -> new HashMap<>());
      fieldMap.put(fieldId, point);
    }
  }

  public static String fieldId(String owner, String fieldName) {
    return owner + " " + fieldName;
  }
}