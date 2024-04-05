package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.annotation.Arg;
import be.coekaerts.wouter.flowtracker.annotation.Hook;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;
import be.coekaerts.wouter.flowtracker.util.Config;
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
