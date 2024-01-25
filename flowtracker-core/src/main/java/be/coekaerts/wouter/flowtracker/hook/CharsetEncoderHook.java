package be.coekaerts.wouter.flowtracker.hook;

public class CharsetEncoderHook {
// not used yet, see CharsetEncoderTest
//  @Hook(target = "java.nio.charset.CharsetEncoder",
//      method = "java.nio.charset.CoderResult encode(java.nio.CharBuffer,java.nio.ByteBuffer,boolean)")
//  public static void afterEncode(int inPosBefore, int outPosBefore, CharBuffer in, ByteBuffer out) {
//    // TODO hook CharsetEncoder.encode: inspect state of buffers before and after?
//    //   oh, but at least for ascii-compatible UTF-8 this isn't necessary, flow analysis takes
//    //   care of it already.
//  }
}
