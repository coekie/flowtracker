package demo;

import com.google.gson.Gson;
import com.google.protobuf.Method;
import com.google.protobuf.Option;
import com.google.protobuf.Syntax;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class EncodeDemo {
  static class Pojo implements Serializable {
    String myField;
  }

  public static void main(String[] args) throws IOException {
    Pojo pojo = new Pojo();
    pojo.myField = "myValue";

    System.out.println("Gson:");
    System.out.println(new Gson().toJson(pojo));
    System.out.println();

    System.out.println("Java serialization:");
    ObjectOutputStream oos = new ObjectOutputStream(System.out);
    oos.writeObject(pojo);
    oos.flush();
    System.out.println();
    System.out.println();

    System.out.println("Protobuf:");
    // we pick "Method" as an example proto here for convenience, because it's provided by the
    // protobuf library itself
    System.out.write(Method.newBuilder()
        // simple field
        .setName("name")
        // enum field
        .setSyntax(Syntax.SYNTAX_PROTO3)
        // nested message
        .addOptions(Option.newBuilder().setName("option"))
        .build().toByteArray());
    System.out.println();
  }
}
