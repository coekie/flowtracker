package be.coekaerts.wouter.flowtracker.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Does reflection, circumventing accessibility and module access checks */
class Reflection {
  private static final Object unsafe;
  private static final Method objectFieldOffset;
  private static final Method getObject;

  static {
    try {
      Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      unsafe = theUnsafe.get(null);
      objectFieldOffset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
      getObject = unsafeClass.getDeclaredMethod("getObject", Object.class, long.class);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  static Object getField(Object o, Field f) {
    try {
      long offset = (long) objectFieldOffset.invoke(unsafe, f);
      return getObject.invoke(unsafe, o, offset);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
