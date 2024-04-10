package be.coekaerts.wouter.flowtracker.web;

import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    }
  }

  /** Write files for {@link TreeResource} */
  private void writeTree(ZipOutputStream zos) throws IOException {
    TreeResource tree = new TreeResource(root);
    writeJson(zos, "tree/all", tree.root());
    writeJson(zos, "tree/origins", tree.origins());
    writeJson(zos, "tree/sinks", tree.sinks());
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
