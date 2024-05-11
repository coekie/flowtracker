package com.coekie.flowtracker.hook;

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
