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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 * Enables access to private fields in JDK classes, circumventing accessibility and module access
 * checks.
 */
public class Reflection {
  /** Trusted lookup, has full access */
  private static final Lookup lookup = initLookup();

  public static Class<?> clazz(String className) {
    try {
      return Class.forName(className, false, null);
    } catch (ClassNotFoundException e) {
      throw new Error("Cannot find " + className, e);
    }
  }

  /** Returns a MethodHandle that reads the requested field */
  public static MethodHandle getter(Class<?> owner, String name, Class<?> fieldType) {
    try {
      return lookup.findGetter(owner, name, fieldType);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new Error(e);
    }
  }

  /** Returns a MethodHandle that sets the requested field */
  public static MethodHandle setter(Class<?> owner, String name, Class<?> fieldType) {
    try {
      return lookup.findSetter(owner, name, fieldType);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new Error(e);
    }
  }

  public static VarHandle varHandle(Class<?> owner, String name, Class<?> fieldType) {
    try {
      return lookup.findVarHandle(owner, name, fieldType);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new Error(e);
    }
  }

  /**
   * Looks up the method handle to access the field and gets its value.
   * <p>
   * This is slow because it looks up the handle every time, instead of caching it. The advantage is
   * that this is less verbose. That is fine for code paths that are rarely executed, and tests.
   */
  public static <T> T getSlow(Class<?> ownerClass, String name, Class<T> fieldType,
      Object o) {
    try {
      return fieldType.cast(getter(ownerClass, name, fieldType).invoke(o));
    } catch (Throwable e) {
      throw new Error(e);
    }
  }

  /**
   * Steals the Lookup with full access.
   * This implementation uses Unsafe, which will likely have to change in future JDK versions.
   */
  private static Lookup initLookup() {
    try {
      // we're running in the bootstrap classloader, so we don't need special tricks to use Unsafe.
      Unsafe unsafe = Unsafe.getUnsafe();
      Field field = Lookup.class.getDeclaredField("IMPL_LOOKUP");
      return (Lookup) unsafe.getObject(
          unsafe.staticFieldBase(field),
          unsafe.staticFieldOffset(field));
    } catch (Throwable t) {
      throw new Error(t);
    }
  }
}
