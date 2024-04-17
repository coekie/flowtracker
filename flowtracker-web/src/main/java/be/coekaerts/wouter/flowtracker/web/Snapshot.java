package be.coekaerts.wouter.flowtracker.web;

import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.Region;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerDetailResponse;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerPartResponse;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Creates a snapshot, a dump of the current state, as a zip file containing json files plus a copy
 * of the UI, that can be viewed in a browser just like the live interface.
 */
public class Snapshot {
  private static final Gson GSON = new Gson();
  static final String PATH_PREFIX = "snapshot/";

  private final TrackerTree.Node root;

  Snapshot(TrackerTree.Node root) {
    this.root = root;
  }

  /** Write the zip file to the output stream */
  void write(OutputStream out) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(out)) {
      writeStaticFiles(zos);
      writeTree(zos);
      writeTrackers(zos);
    }
  }

  /** Write files for {@link TreeResource} */
  private void writeTree(ZipOutputStream zos) throws IOException {
    TreeResource tree = new TreeResource(root);
    writeJson(zos, "tree/all", tree.root());
    writeJson(zos, "tree/origins", tree.origins());
    writeJson(zos, "tree/sinks", tree.sinks());
  }

  /** Write files for {@link TrackerResource} */
  private void writeTrackers(ZipOutputStream zos) throws IOException {
    TrackerResource trackerResource = new TrackerResource();
    Set<Long> written = new HashSet<>();
    writeTrackers(zos, trackerResource, written, TrackerTree.ROOT);
  }

  /** Write a tracker, and all other trackers that it refers to */
  private void writeTracker(ZipOutputStream zos, TrackerResource trackerResource, Set<Long> written,
      long trackerId) throws IOException {
    if (written.add(trackerId)) {
      TrackerDetailResponse trackerDetail = trackerResource.get(trackerId);
      writeJson(zos, "tracker/" + trackerId, trackerDetail);
      Set<Long> toWritten = new HashSet<>(); // for which trackers we wrote x_to_y already
      for (Region region : trackerDetail.regions) {
        for (TrackerPartResponse part : region.parts) {
          writeTracker(zos, trackerResource, written, part.tracker.id);
          if (toWritten.add(part.tracker.id)) {
            writeJson(zos, "tracker/" + part.tracker.id + "_to_" + trackerId,
                trackerResource.reverse(part.tracker.id, trackerId));
          }
        }
      }
    }
  }

  private void writeTrackers(ZipOutputStream zos, TrackerResource trackerResource,
      Set<Long> written, TrackerTree.Node node) throws IOException {
    for (Tracker tracker : node.trackers()) {
      writeTracker(zos, trackerResource, written, tracker.getTrackerId());
    }
    for (Node child : node.children()) {
      writeTrackers(zos, trackerResource, written, child);
    }
  }

  /** Write a single json file */
  private void writeJson(ZipOutputStream zos, String path, Object o) throws IOException {
    ZipEntry entry = new ZipEntry(PATH_PREFIX + path);
    zos.putNextEntry(entry);
    zos.write(GSON.toJson(o).getBytes());
    zos.closeEntry();
  }

  /** Write a single json file */
  private void writeFile(ZipOutputStream zos, String path, byte[] bytes) throws IOException {
    ZipEntry entry = new ZipEntry(PATH_PREFIX + path);
    zos.putNextEntry(entry);
    zos.write(bytes);
    zos.closeEntry();
  }

  /** Write the static files for the UI (html, js,...) */
  private void writeStaticFiles(ZipOutputStream zos) throws IOException {
    for (String path : findStaticResources("static", "index.html")) {
      try (InputStream in = requireNonNull(Snapshot.class.getResourceAsStream("/static/" + path))) {
        writeFile(zos, path, in.readAllBytes());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Find all resources in the classpath under `dir`. Given an example file in that dir, so that we
   * can use {@link Class#getResource(String)}.
   */
  static List<String> findStaticResources(String dir, String exampleFile) throws IOException {
    URL indexUrl = requireNonNull(Snapshot.class.getResource('/' + dir + "/" + exampleFile));
    if (indexUrl.getProtocol().equals("file")) {
      Path staticDir = Path.of(indexUrl.getPath()).getParent();
      try (Stream<Path> paths = Files.walk(staticDir)) {
        return paths
            .filter(Files::isRegularFile)
            .map(fullPath -> staticDir.relativize(fullPath).toString())
            .collect(Collectors.toList());
      }
    } else if (indexUrl.getProtocol().equals("jar")
        && indexUrl.getPath().startsWith("file:")
        && indexUrl.getPath().contains("!/")) {
      String jarPath = indexUrl.getPath().substring(5, indexUrl.getPath().indexOf("!/"));
      // note: the way we package ourselves in the flowtracker agent jar, the classpath root is not
      // the root of the jar. so for our jar, `pathInJar` is not the same as `dir`
      String pathInJar = indexUrl.getPath().substring(indexUrl.getPath().indexOf("!/") + 2,
          indexUrl.getPath().lastIndexOf('/') + 1);
      try (ZipFile zip = new ZipFile(jarPath)) {
        return zip.stream()
            .filter(entry -> !entry.isDirectory())
            .filter(entry -> entry.getName().startsWith(pathInJar))
            .map(entry -> entry.getName().substring(pathInJar.length()))
            .collect(Collectors.toList());
      }
    } else {
      throw new UnsupportedOperationException("Unexpected resources URL: " + indexUrl);
    }
  }
}
