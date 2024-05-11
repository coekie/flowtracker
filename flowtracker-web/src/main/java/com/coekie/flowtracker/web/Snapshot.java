package com.coekie.flowtracker.web;

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

import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerTree.Node;
import com.coekie.flowtracker.web.SettingsResource.Settings;
import com.coekie.flowtracker.web.TrackerResource.Region;
import com.coekie.flowtracker.web.TrackerResource.TrackerDetailResponse;
import com.coekie.flowtracker.web.TrackerResource.TrackerPartResponse;
import com.coekie.flowtracker.web.TreeResource.NodeRequestParams;
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
import java.util.function.Predicate;
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
  static final Gson GSON = new Gson();
  static final String PATH_PREFIX = "snapshot/";

  /**
   * Root node, to determine what trackers to include in the snapshot. {@link TrackerTree#ROOT} in
   * real snapshots, but can be something else in tests.
   */
  private final TrackerTree.Node root;

  /** A minimized snapshot only includes origins if they are referenced from a sink. In other words,
   * it leaves out data that was read, but then not used in the output (at least not in a way that
   * we track)
   */
  private final boolean minimized;

  private final TrackerResource trackerResource = new TrackerResource();

  /** Ids of trackers that we already wrote in the snapshot */
  private final Set<Long> includedTrackers = new HashSet<>();

  Snapshot(TrackerTree.Node root, boolean minimized) {
    this.root = root;
    this.minimized = minimized;
  }

  /** Write the zip file to the output stream */
  void write(OutputStream out) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(out)) {
      writeStaticFiles(zos);
      writeSettings(zos);

      // the order here matters because Snapshot.includedTrackers is mutable: it is populated by
      // writeTrackers and depended on by writeTree.
      writeTrackers(zos, TrackerTree.ROOT, false);
      writeTree(zos);
    }
  }

  /** Write files for {@link TreeResource} */
  private void writeTree(ZipOutputStream zos) throws IOException {
    TreeResource tree = new TreeResource(root);
    // only include trackers in the tree if we wrote the tracker; that excludes trackers that have
    // been "minimized" away.
    Predicate<Tracker> filter = tracker -> includedTrackers.contains(tracker.getTrackerId());

    writeJson(zos, "tree/all", tree.tree(NodeRequestParams.ALL.and(filter)));
    writeJson(zos, "tree/origins", tree.tree(NodeRequestParams.ORIGINS.and(filter)));
    writeJson(zos, "tree/sinks", tree.tree(NodeRequestParams.SINKS.and(filter)));
  }

  private void writeTrackers(ZipOutputStream zos, TrackerTree.Node node, boolean minimizedNode)
      throws IOException {
    // only apply minimizing to files and classes
    if (node == TrackerTree.FILES || node == TrackerTree.CLASS) {
      minimizedNode = minimized;
    }
    for (Tracker tracker : node.trackers()) {
      // if condition: when minimized then don't include origin trackers just because they are in
      // the tree. So they are only included if they are referenced from a sink.
      if (!(minimizedNode && TrackerResource.isOrigin(tracker))) {
        InterestRepository.register(tracker);
        writeTracker(zos, tracker.getTrackerId());
      }
    }
    for (Node child : node.children()) {
      writeTrackers(zos, child, minimizedNode);
    }
  }

  /** Write a tracker, and all other trackers that it refers to */
  private void writeTracker(ZipOutputStream zos, long trackerId) throws IOException {
    if (includedTrackers.add(trackerId)) {
      TrackerDetailResponse trackerDetail = trackerResource.get(trackerId);
      writeJson(zos, "tracker/" + trackerId, trackerDetail);
      Set<Long> toWritten = new HashSet<>(); // for which trackers we wrote x_to_y already
      for (Region region : trackerDetail.regions) {
        for (TrackerPartResponse part : region.parts) {
          writeTracker(zos, part.tracker.id);
          if (toWritten.add(part.tracker.id)) {
            writeJson(zos, "tracker/" + part.tracker.id + "_to_" + trackerId,
                trackerResource.reverse(part.tracker.id, trackerId));
          }
        }
      }
    }
  }

  private void writeSettings(ZipOutputStream zos) throws IOException {
    Settings settings = new SettingsResource().get();
    settings.snapshot = true;
    writeJson(zos, "settings", settings);
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
