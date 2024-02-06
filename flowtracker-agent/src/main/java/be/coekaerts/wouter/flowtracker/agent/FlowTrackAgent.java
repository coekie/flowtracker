package be.coekaerts.wouter.flowtracker.agent;

import be.coekaerts.wouter.flowtracker.CoreInitializer;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import be.coekaerts.wouter.flowtracker.util.Logger;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class FlowTrackAgent {
  /** Configuration passed to the agent */
  private static final Map<String, String> config = new HashMap<>();

  public static void premain(String agentArgs, Instrumentation inst) {
    new FlowTrackAgent().doPremain(agentArgs, inst);
  }

  void doPremain(String agentArgs, Instrumentation inst) {
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
  ClassLoader initClassLoaders(Instrumentation inst) throws IOException {
    JarFile jar = getAgentJar();

    // make the instrumented JDK classes find the hook class
    inst.appendToBootstrapClassLoaderSearch(jar);

    return new SpiderClassLoader(jar);
  }

  // NICE: generic plugin system would be cleaner
  private static void initWeb(ClassLoader classLoader) throws Exception {
    // don't start webserver when "noweb" is set, e.g. for unit tests
    if ("true".equals(config.get("noweb"))) {
      return;
    }

    Class<?> clazz;
    try {
      clazz = classLoader.loadClass("be.coekaerts.wouter.flowtracker.web.WebModule");
    } catch (ClassNotFoundException e) {
      throw new Error(e);
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

  /** Returns the jar that this class is running in */
  static JarFile getAgentJar() throws IOException {
    URL url = Thread.currentThread().getContextClassLoader().getResource("flowtracker-spider");
    if (url == null) {
      throw new IllegalStateException("Failed to find our own jar");
    }
    String path = url.getPath();
    if (!"jar".equals(url.getProtocol()) || !path.startsWith("file:/")) {
      throw new IllegalStateException("Flowtracker not launched from jar file: " + url);
    }
    return new JarFile(path.substring("file:".length(), path.indexOf("!")));
  }
}
