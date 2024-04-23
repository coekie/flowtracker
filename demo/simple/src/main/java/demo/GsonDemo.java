package demo;

import com.google.gson.Gson;

public class GsonDemo {
  static class Pojo {
    String myField;

    @Override
    public String toString() {
      return "Pojo(" + myField + ')';
    }
  }

  public static void main(String... args) {
    Pojo pojo = new Pojo();
    pojo.myField = "toJson";
    System.out.println(new Gson().toJson(pojo));

    System.out.println(new Gson().fromJson("{\"myField\": \"fromJson\"}", Pojo.class));
  }
}
