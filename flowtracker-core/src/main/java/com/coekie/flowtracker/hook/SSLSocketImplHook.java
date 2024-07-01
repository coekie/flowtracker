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

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.tracker.FileDescriptorTrackerRepository;
import com.coekie.flowtracker.tracker.TrackerRepository;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;

public class SSLSocketImplHook {
  /** Last part of path for trackers on SSL sockets */
  public static final String SSL = "SSL";

  @Hook(target = "sun.security.ssl.SSLSocketImpl",
      method = "void doneConnect()")
  public static void afterConnect(@Arg("THIS") SSLSocket sslSocket) {
    Context context = context();
    if (context.isActive()) {
      InputStream in;
      OutputStream out;
      try {
        in = sslSocket.getInputStream();
        out = sslSocket.getOutputStream();
      } catch (IOException e) {
        return;
      }

      // TODO we may have to look for sslSocket's BaseSSLSocketImpl.self here, to handle SSLSockets
      //  layered over a preexisting socket
      FileDescriptor fd = SocketImplHook.getSocketFd(sslSocket);

      ByteOriginTracker readTracker = new ByteOriginTracker();
      ByteOriginTracker delegateReadTracker =
          FileDescriptorTrackerRepository.getReadTracker(context, fd);
      if (delegateReadTracker != null) {
        readTracker.addTo(delegateReadTracker.getNode().node("SSL"));
      }
      TrackerRepository.setTracker(context, in, readTracker);

      ByteSinkTracker writeTracker = new ByteSinkTracker();
      ByteSinkTracker delegateWriteTracker =
          FileDescriptorTrackerRepository.getWriteTracker(context, fd);
      if (delegateWriteTracker != null) {
        writeTracker.addTo(delegateWriteTracker.getNode().node("SSL"));
      }
      TrackerRepository.setTracker(context, out, writeTracker);

      readTracker.twin = writeTracker;
      writeTracker.twin = readTracker;
    }
  }
}
