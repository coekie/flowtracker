package be.coekaerts.wouter.flowtracker.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Does reflection, circumventing accessibility and module access checks */
public class Reflection {
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

  public static Field getDeclaredField(Class<?> clazz, String name) {
    try {
      return clazz.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      throw new Error("Cannot find " + clazz + "." + name, e);
    }
  }

  public static Object getFieldValue(Object o, Field f) {
    try {
      long offset = (long) objectFieldOffset.invoke(unsafe, f);
      return getObject.invoke(unsafe, o, offset);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
