package com.coekie.flowtracker.test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** Helper class for testing SSL sockets. Contains a connected server & client socket pair. */
class SSLSocketTester implements Closeable {
  final Socket server;
  final Socket client;

  private SSLSocketTester(Socket server, Socket client) {
    this.server = server;
    this.client = client;
  }

  static SSLSocketTester createDirect() {
    try {
      try (ServerSocket listenSocket = serverSSLContext().getServerSocketFactory()
          .createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
        SSLSocket client = (SSLSocket) clientSSLContext().getSocketFactory()
            .createSocket(listenSocket.getInetAddress(),
                listenSocket.getLocalPort());
        SSLSocket server = (SSLSocket) listenSocket.accept();

        Future<?> clientHandshake = handshake(client);
        Future<?> serverHandshake = handshake(server);

        clientHandshake.get();
        serverHandshake.get();

        return new SSLSocketTester(server, client);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create an SSLSocket "layered" on top of a plain Socket connection.
   * Unlike in {@link #createDirect()}, here we have `BaseSSLSocketImpl.self` not equal to
   * itself; the underlying socket and the SSL socket are two different objects.
   */
  static SSLSocketTester createLayered() {
    SocketTester plainSocketTester = SocketTester.createConnected();
    try {
      SSLSocket client = (SSLSocket) clientSSLContext().getSocketFactory()
          .createSocket(plainSocketTester.client, "localhost", plainSocketTester.client.getPort(),
              true);
      SSLSocket server = (SSLSocket) serverSSLContext().getSocketFactory()
          .createSocket(plainSocketTester.server, null, false);

      Future<?> clientHandshake = handshake(client);
      Future<?> serverHandshake = handshake(server);

      clientHandshake.get();
      serverHandshake.get();

      return new SSLSocketTester(server, client);
    } catch (Exception e) {
      try {
        plainSocketTester.close();
      } catch (IOException ignore) {
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    server.close();
    client.close();
  }

  /** Create an SSLContext for the server, with our certificate */
  private static SSLContext serverSSLContext() throws Exception {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(SSLSocketTest.class.getResourceAsStream("keystore"), "flowtracker".toCharArray());
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, "flowtracker".toCharArray());

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, null);
    return sslContext;
  }

  /** Create an SSLContext for the client, accepting our certificate */
  private static SSLContext clientSSLContext() throws Exception {
    // fake TrustManager that trusts everything. we really do not care.
    X509TrustManager trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[]{trustManager}, null);
    return sslContext;
  }

  private static Future<?> handshake(SSLSocket socket) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            socket.startHandshake();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }
}
