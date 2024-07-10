package com.coekie.flowtracker.agent;

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

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.util.Config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Classloader for weaver + web, that loads classes from the "flowtracker-spider" dir in the jar */
class SpiderClassLoader extends ClassLoader {
  private final JarFile jar;
  private final boolean hideInternals;
  private final ProtectionDomain pd;

  SpiderClassLoader(JarFile jar, Config config) {
    super(null);
    this.jar = jar;
    this.hideInternals = config.hideInternals();
    // to prevent NPE in Vineflower's JarPluginLoader initialization, make sure our ProtectionDomain
    // has a location.
    try {
      this.pd = new ProtectionDomain(
          new CodeSource(new File(jar.getName()).toURL(), (CodeSigner[])null), null);
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    JarEntry entry = jar.getJarEntry("flowtracker-spider/" + name.replace('.', '/') + ".class");
    if (entry == null) {
      throw new ClassNotFoundException(name);
    }

    byte[] b;
    if (hideInternals) {
      // don't track reading of flowtracker class files because that's noise to the user.
      // (see also ClassLoaderHook.shouldHideFileReading for other class loaders)
      context().suspend();
    }
    try (InputStream in = jar.getInputStream(entry)) {
      b = in.readAllBytes();
    } catch (IOException e) {
      throw new Error(e);
    } finally {
      if (hideInternals) {
        context().unsuspend();
      }
    }
    return defineClass(name, b, 0, b.length, pd);
  }

  @Override
  protected URL findResource(String name) {
    String nestedName = "flowtracker-spider/" + name;
    if (jar.getEntry(nestedName) != null) {
      try {
        return new URL("jar:file:" + jar.getName() + "!/" + nestedName);
      } catch (MalformedURLException e) {
        throw new Error(e);
      }
    }
    return null;
  }

  @Override
  protected Enumeration<URL> findResources(String name) {
    URL url = findResource(name);
    if (url == null) {
      return Collections.emptyEnumeration();
    } else {
      return Collections.enumeration(Collections.singletonList(url));
    }
  }
}
