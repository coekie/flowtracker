package be.coekaerts.wouter.flowtracker.agent;

import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Classloader for weaver + web, that loads classes from the "flowtracker-spider" dir in the jar */
class SpiderClassLoader extends ClassLoader {
  private final JarFile jar;
  private final boolean hideInternals;

  SpiderClassLoader(JarFile jar, Map<String, String> config) {
    super(null);
    this.jar = jar;
    this.hideInternals = !"false".equals(config.get("hideInternals"));
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    JarEntry entry = jar.getJarEntry("flowtracker-spider/" + name.replace('.', '/') + ".class");
    if (entry == null) {
      throw new ClassNotFoundException(name);
    }

    byte[] b;
    if (hideInternals) {
      // don't track reading of flowtracker class files because that's noise to the user
      Trackers.suspendOnCurrentThread();
    }
    try (InputStream in = jar.getInputStream(entry)) {
      b = in.readAllBytes();
    } catch (IOException e) {
      throw new Error(e);
    } finally {
      if (hideInternals) {
        // don't track reading of flowtracker class files because that's noise to the user
        Trackers.unsuspendOnCurrentThread();
      }
    }
    return defineClass(name, b, 0, b.length);
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
