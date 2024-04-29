package demo;

import org.yaml.snakeyaml.Yaml;

public class SnakeYamlDemo {
  public static class Pojo {
    public String myField;

    @Override
    public String toString() {
      return "Pojo(" + myField + ')';
    }
  }

  public static void main(String... args) {
    Yaml yaml = new Yaml();

    // parse yaml
    System.out.println(yaml.loadAs("myField: hello", Pojo.class));

    // dump yaml
    Pojo pojo = new Pojo();
    pojo.myField = "toDump";
    System.out.println(yaml.dump(pojo));
  }
}
