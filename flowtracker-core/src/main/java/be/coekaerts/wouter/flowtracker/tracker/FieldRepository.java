package be.coekaerts.wouter.flowtracker.tracker;

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
