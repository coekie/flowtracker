package be.coekaerts.wouter.flowtracker.agent;

import be.coekaerts.wouter.flowtracker.CoreInitializer;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import be.coekaerts.wouter.flowtracker.util.Config;
import be.coekaerts.wouter.flowtracker.util.Logger;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.jar.JarFile;

public class FlowTrackAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      JarFile agentJar = getAgentJar();
      // Put flowtracker-core on the bootstrap classpath, to make instrumented JDK classes find the
      // hook classes.
      inst.appendToBootstrapClassLoaderSearch(agentJar);
      new Phase2().premain2(agentArgs, inst, agentJar);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
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

  /**
   * Second phase of starting of the agent. This is in a separate class, so that FlowTrackAgent
   * itself doesn't have references to classes from flowtracker-core that need to be put on the
   * bootstrap classpath _before_ they get loaded.
   */
  public static class Phase2 {
    public void premain2(String agentArgs, Instrumentation inst, JarFile agentJar)
        throws Exception {
      Config config = Config.initialize(agentArgs);
      ClassLoader spiderClassLoader = createSpiderClassLoader(inst, agentJar, config);

      Logger.initLogging(config);

      // do not track our own initialization
      Trackers.suspendOnCurrentThread();

      spiderClassLoader
          .loadClass("be.coekaerts.wouter.flowtracker.weaver.WeaverInitializer")
          .getMethod("initialize", Instrumentation.class, Config.class)
          .invoke(null, inst, config);

      CoreInitializer.initialize(config, agentJar);

      // initialization done, unsuspend tracking
      Trackers.unsuspendOnCurrentThread();

      initWeb(spiderClassLoader, config);
      CoreInitializer.postInitialize(config);
    }

    /**
     * Initialize the spiderClassLoader used to load the weaver and the web interface.
     * Those are loaded with their dependencies in a separate class loader to avoid polluting the
     * classpath of the application.
     */
    public ClassLoader createSpiderClassLoader(Instrumentation inst, JarFile agentJar,
        Config config) throws IOException {
      return new SpiderClassLoader(agentJar, config);
    }

    // NICE: generic plugin system would be cleaner
    private static void initWeb(ClassLoader classLoader, Config config) throws Exception {
      // don't start webserver when "noweb" is set, e.g. for unit tests
      if (config.getBoolean("noweb", false)) {
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
  }
}
