package be.coekaerts.wouter.flowtracker.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Classloader for weaver + web, that loads classes from the "flowtracker-spider" dir in the jar */
class SpiderClassLoader extends ClassLoader {
  private final JarFile jar;

  SpiderClassLoader(JarFile jar) {
    super(null);
    this.jar = jar;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    JarEntry entry = jar.getJarEntry("flowtracker-spider/" + name.replace('.', '/') + ".class");
    if (entry == null) {
      throw new ClassNotFoundException(name);
    }

    try (InputStream in = jar.getInputStream(entry)) {
      byte[] b = in.readAllBytes();
      return defineClass(name, b, 0, b.length);
    } catch (IOException e) {
      throw new Error(e);
    }
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
