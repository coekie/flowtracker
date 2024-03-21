package demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SimpleDemo {
  public static void main(String[] args) throws IOException {
    System.out.println("Starting");
    try (BufferedReader reader = new BufferedReader(new FileReader("/etc/issue"))) {
      System.out.println(reader.readLine());
    }
    Files.copy(Paths.get("/etc/issue"), System.out);
    System.out.write("test\n".getBytes());
    System.out.println('a');
    System.out.println("Done");
  }
}