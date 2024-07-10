package demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializationDemo {
  static class Pojo implements Serializable {
    String myField;

    @Override
    public String toString() {
      return "Pojo(" + myField + ')';
    }
  }

  public static void main(String... args) throws Exception {
    Pojo pojo = new Pojo();
    pojo.myField = "myValue";

    byte[] serialized = serialize(pojo);
    System.out.print("Serialized: ");
    System.out.write(serialized);
    System.out.println();

    System.out.print("Deserialized: ");
    System.out.println(deserialize(serialized));
  }

  static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(obj);
    }
    return bos.toByteArray();
  }

  static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    try (ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return in.readObject();
    }
  }
}
