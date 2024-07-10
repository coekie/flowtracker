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

import com.coekie.flowtracker.util.Config;
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
      // so unlike FlowTrackerAgent, we don't need to do appendToBootstrapClassLoaderSearch here.
      JarFile agentJar = FlowTrackerAgent.getAgentJar();
      new Phase2().premain2(agentArgs, inst, agentJar);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  static class Phase2 extends FlowTrackerAgent.Phase2 {
    @Override
    public ClassLoader createSpiderClassLoader(Instrumentation inst, JarFile agentJar,
        Config config) throws IOException {
      // agentJar is agent/agent-dev/target/agent-dev-*-SNAPSHOT.jar
      File root = new File(agentJar.getName()).getParentFile().getParentFile().getParentFile()
          .getParentFile();

      List<URL> spiderClasspath = new ArrayList<>();
      String[] modules = config.getBoolean("webmodule", true)
          ? new String[]{"flowtracker-weaver", "web"}
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
