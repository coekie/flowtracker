package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.Trackers;
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
    if (Trackers.isActive()) {
      Tracker tracker = TrackerRepository.getTracker(result);
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
