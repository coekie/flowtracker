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

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.util.Config;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ZipFileHook {
  private static boolean hideInternals;
  /**
   * Entries in this file are not tracked (well, not put in the TrackerTree), because the user
   * doesn't care about tracking the internals of flowtracker.
   */
  private static JarFile agentJarToHide;

  @Hook(target = "java.util.zip.ZipFile",
      method = "java.io.InputStream getInputStream(java.util.zip.ZipEntry)")
  @Hook(target = "org.springframework.boot.loader.jar.NestedJarFile",
      method = "java.io.InputStream getInputStream(java.util.zip.ZipEntry)")
  public static void afterGetInputStream(@Arg("RETURN") InputStream result,
      @Arg("THIS") ZipFile target, @Arg("ARG0") ZipEntry zipEntry) {
    Context context = context();
    if (context.isActive()) {
      Tracker tracker = TrackerRepository.getTracker(context, result);
      // shouldn't be null because InflaterInputStream constructor is instrumented
      if (tracker == null) {
        return;
      }
      if (hideInternals) {
        if (agentJarToHide.getName().equals(target.getName())) {
          return;
        }
      }
      if (ClassLoaderHook.shouldHideFileReading(zipEntry.getName())) {
        return;
      }
      if (tracker.getNode() == null) {
        tracker.addTo(TrackerTree.fileNode(target.getName()).pathNode(zipEntry.getName()));
      }
    }
  }

  public static void initialize(Config config, JarFile agentJar) {
    hideInternals = config.hideInternals();
    agentJarToHide = agentJar;
  }
}
