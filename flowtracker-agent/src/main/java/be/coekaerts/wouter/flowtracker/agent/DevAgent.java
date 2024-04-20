package be.coekaerts.wouter.flowtracker.agent;

import be.coekaerts.wouter.flowtracker.util.Config;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/** Agent using by unit tests and for development */
public class DevAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      // for DevAgent the manifest already puts flowtracker-core on the bootstrap classpath,
      // so unlike FlowTrackAgent, we don't need to do appendToBootstrapClassLoaderSearch here.
      JarFile agentJar = FlowTrackAgent.getAgentJar();
      new Phase2().premain2(agentArgs, inst, agentJar);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  static class Phase2 extends FlowTrackAgent.Phase2 {
    @Override
    public ClassLoader createSpiderClassLoader(Instrumentation inst, JarFile agentJar,
        Config config) throws IOException {
      // agentJar is flowtracker-agent-dev/target/flowtracker-agent-dev-*-SNAPSHOT.jar
      File root = new File(agentJar.getName()).getParentFile().getParentFile().getParentFile();

      List<URL> spiderClasspath = new ArrayList<>();
      String[] modules = config.getBoolean("webmodule", true)
          ? new String[]{"flowtracker-weaver", "flowtracker-web"}
          : new String[]{"flowtracker-weaver"};
      for (String module : modules) {
        File classesPath = new File(root, module + "/target/classes");
        if (!classesPath.exists()) {
          throw new Error("Error building classpath: " + classesPath + " not found");
        }
        spiderClasspath.add(classesPath.toURI().toURL());
      }
      // we load our own classes from target/classes, but our dependencies are bundled in the agent
      // jar, handled by SpiderClassLoader
      return new URLClassLoader(spiderClasspath.toArray(new URL[0]),
          new SpiderClassLoader(agentJar, config));
    }
  }
}
