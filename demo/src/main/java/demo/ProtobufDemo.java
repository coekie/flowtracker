package demo;

import com.google.protobuf.Method;
import com.google.protobuf.Option;
import com.google.protobuf.Syntax;
import java.io.IOException;

public class ProtobufDemo {
  public static void main(String... args) throws IOException {
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
  }
}
