package be.coekaerts.wouter.flowtracker.agent;

import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.CoreInitializer;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import be.coekaerts.wouter.flowtracker.util.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FlowTrackAgent {
  /** Configuration passed to the agent */
  private static final Map<String, String> config = new HashMap<>();

  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      initProperties(agentArgs);
      // system property to override agent args. Useful in IntelliJ which picks up the agent from
      // the maven surefire settings but makes it impossible to change the arguments passed to it.
      initProperties(System.getProperty("flowtracker.agentArgs"));

      /*
       * Classloader used to load the weaver and the web interface.
       * Those are loaded with their dependencies in a separate class loader to avoid polluting the
       * classpath of the application.
       *
       * This also puts flowtracker-core on the bootstrap classpath. So code before this line should
       * not make any references to classes in flowtracker-core yet.
       */
      ClassLoader spiderClassLoader = initClassLoaders(inst);

      Logger.initLogging(config);

      // do not track our own initialization
      Trackers.suspendOnCurrentThread();

      spiderClassLoader
          .loadClass("be.coekaerts.wouter.flowtracker.weaver.WeaverInitializer")
          .getMethod("initialize", Instrumentation.class, Map.class)
          .invoke(null, inst, config);

      CoreInitializer.initialize(config);

      // initialization done, unsuspend tracking
      Trackers.unsuspendOnCurrentThread();

      initWeb(spiderClassLoader);
      CoreInitializer.postInitialize(config);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Update the bootstrap classpath and initialize the spiderClassLoader used to load the weaver
   * and the web interface.
   * Those are loaded with their dependencies in a separate class loader to avoid polluting the
   * classpath of the application.
   */
  private static ClassLoader initClassLoaders(Instrumentation inst) throws IOException {
    // all other flowtracker and dependencies jars
    List<URL> spiderClasspath = new ArrayList<>();

    if (config.containsKey("spiderClasspath")) {
      for (String path : getConfig("spiderClasspath").split(",")) {
        spiderClasspath.add(new File(path).toURI().toURL());
      }
    } else { // paths not explicitly configured
      // assume we're running from flowtracker-all jar with nested jars in it
      File expandDir = createExpandDir();
      JarFile jar = getThisJar();

      // make the instrumented JDK classes find the hook class
      inst.appendToBootstrapClassLoaderSearch(jar);

      for (JarEntry entry : Collections.list(jar.entries())) {
        if (entry.getName().startsWith("flowtracker/") && entry.getName().endsWith(".jar")) {
          String subJarName = entry.getName().substring("flowtracker/".length());
          File outFile = new File(expandDir, subJarName);
          copy(jar.getInputStream(entry), outFile);
          spiderClasspath.add(outFile.toURI().toURL());
        }
      }
    }

    return new URLClassLoader(spiderClasspath.toArray(new URL[0]), null);
  }

  // NICE: generic plugin system would be cleaner
  private static void initWeb(ClassLoader classLoader) throws Exception {
    Class<?> clazz;
    try {
      clazz = classLoader.loadClass("be.coekaerts.wouter.flowtracker.web.WebModule");
    } catch (ClassNotFoundException e) {
      return; // ok, module not included
    }
    clazz.newInstance();
  }

  private static void initProperties(String agentArgs) {
    if (agentArgs != null) {
      for (String arg : agentArgs.split(";")) {
        String[] keyAndValue = arg.split("=", 2);
        config.put(keyAndValue[0], keyAndValue[1]);
      }
    }
  }

  private static String getConfig(String name) {
    String result = config.get(name);
    if (result == null) {
      throw new RuntimeException("You must provide '" + name + "' as argument to the agent");
    }
    return result;
  }

  /** Returns the jar that this class is running in */
  private static JarFile getThisJar() throws IOException {
    URL url = requireNonNull(FlowTrackAgent.class.getResource("FlowTrackAgent.class"));
    String path = url.getPath();
    if (!"jar".equals(url.getProtocol()) || !path.startsWith("file:/")) {
      throw new IllegalStateException("Flowtracker not launched from jar file: " + url);
    }
    return new JarFile(path.substring("file:".length(), path.indexOf("!")));
  }

  /** Create temporary directory that we expand ourselves into */
  private static File createExpandDir() {
    Random rnd = new Random();
    for (int counter = 0; counter < 1000; counter++) {
      File tempDir = new File(System.getProperty("java.io.tmpdir"), "flowtracker-" + Math.abs(
          rnd.nextInt()));
      if (tempDir.mkdir()) {
        tempDir.deleteOnExit();
        return tempDir;
      }
    }
    throw new RuntimeException("Could not create temporary directory");
  }

  private static void copy(InputStream in, File outFile) throws IOException {
    try {
      try (FileOutputStream out = new FileOutputStream(outFile)) {
        byte[] buf = new byte[1024];
        while (true) {
          int r = in.read(buf);
          if (r == -1) {
            break;
          }
          out.write(buf, 0, r);
        }
      }
    } finally {
      in.close();
    }
  }
}
