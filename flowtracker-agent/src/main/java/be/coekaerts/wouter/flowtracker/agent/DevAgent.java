package be.coekaerts.wouter.flowtracker.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/** Agent using by unit tests and for development */
public class DevAgent extends FlowTrackAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
    new DevAgent().doPremain(agentArgs, inst);
  }

  @Override
  ClassLoader initClassLoaders(Instrumentation inst, JarFile agentJar) throws IOException {
    // for DevAgent the manifest already puts flowtracker-core on the bootstrap classpath,
    // so unlike our superclass, we don't need to do appendToBootstrapClassLoaderSearch here.

    // agentJar is flowtracker-agent-dev/target/flowtracker-agent-dev-*-SNAPSHOT.jar
    File root = new File(agentJar.getName()).getParentFile().getParentFile().getParentFile();

    List<URL> spiderClasspath = new ArrayList<>();
    for (String module : new String[]{"flowtracker-weaver", "flowtracker-web"}) {
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
