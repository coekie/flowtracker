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

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.annotation.HookLocation;
import com.coekie.flowtracker.util.Config;

public class ClassLoaderHook {
  /** Keeps track of which class is currently being loaded */
  private static final ThreadLocal<String> loadingClass = new ThreadLocal<>();
  private static boolean hideInternals;

  @Hook(target = "jdk.internal.loader.BuiltinClassLoader",
      method = "java.lang.Class defineClass(java.lang.String, jdk.internal.loader.Resource)",
      location = HookLocation.ON_ENTER)
    @Hook(target = "java.net.URLClassLoader",
      method = "java.lang.Class defineClass(java.lang.String, jdk.internal.loader.Resource)",
      location = HookLocation.ON_ENTER)
  public static void beforeDefineClass(@Arg("ARG0") String className) {
    loadingClass.set(className);
  }

  @Hook(target = "jdk.internal.loader.BuiltinClassLoader",
      method = "java.lang.Class defineClass(java.lang.String, jdk.internal.loader.Resource)")
  @Hook(target = "java.net.URLClassLoader",
      method = "java.lang.Class defineClass(java.lang.String, jdk.internal.loader.Resource)")
  public static void afterDefineClass() {
    // we're calling .remove() on a best effort basis here: if there's an exception then it won't
    // be thrown
    loadingClass.remove();
  }

  public static void initialize(Config config) {
    hideInternals = config.hideInternals();
  }

  /**
   * Checks if the reading of the given path (from filesystem or a jar file) should be hidden.
   * We hide loading of classes by default because it adds a lot of noise and doesn't really provide
   * any value.
   */
  static boolean shouldHideFileReading(String path) {
    if (!hideInternals) {
      return false;
    }
    String currentLoadingClass = loadingClass.get();
    return currentLoadingClass != null
        && path.endsWith(currentLoadingClass.replace('.', '/') + ".class");
  }
}
