package be.coekaerts.wouter.flowtracker.hook;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 * Does reflection, circumventing accessibility and module access checks.
 *
 * <p>This is currently a simple wrapper around Unsafe, mostly kept to make it easy to change
 * (again) how we access fields if JDK changes require it.
 */
public class Reflection {
  private static final Unsafe unsafe = Unsafe.getUnsafe();

  public static Field getDeclaredField(String className, String name) {
    try {
      return getDeclaredField(Class.forName(className, false, null), name);
    } catch (ClassNotFoundException e) {
      throw new Error("Cannot find " + className, e);
    }
  }

  public static Field getDeclaredField(Class<?> clazz, String name) {
    try {
      return clazz.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      throw new Error("Cannot find " + clazz + "." + name, e);
    }
  }

  // TODO optimize: instead of getting field offset every time, have something like FieldAccessor
  //  that is stored in a static field in the class that calls this (instead of the Field instance)
  public static Object getFieldValue(Object o, Field f) {
    long offset = unsafe.objectFieldOffset(f);
    return unsafe.getObject(o, offset);
  }

  public static int getInt(Object o, Field f) {
    long offset = unsafe.objectFieldOffset(f);
    return unsafe.getInt(o, offset);
  }
}
