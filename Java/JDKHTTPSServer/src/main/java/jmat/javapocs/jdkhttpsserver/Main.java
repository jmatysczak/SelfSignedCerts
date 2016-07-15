package jmat.javapocs.jdkhttpsserver;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class Main {

    public static void main(final String[] args) throws Exception {
        final SSLContext serverContext = createSSLContext("../stores/server.pkcs12", "server-password");
        try (final AutoCloseable server = createServer(serverContext)) {
            System.out.println("Serving...");

            final SSLContext clientContext = createSSLContext("../../certs/certs/browser.p12", "browser-password");
            HttpsURLConnection.setDefaultSSLSocketFactory(clientContext.getSocketFactory());

            final URL url = new URL("https://localhost:9000/");
            try (final InputStream in = url.openStream()) {
                int c;
                while ((c = in.read()) != -1) {
                    System.out.print((char) c);
                }
                System.out.println();
            }
        }

        System.out.println("Done.");
    }

    private static SSLContext createSSLContext(final String keyStoreFile, final String keyStorePassword) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
        final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream("../stores/trustStore"), "trust-store-password".toCharArray());

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, new SecureRandom());

        return context;
    }

    private static AutoCloseable createServer(final SSLContext context) throws Exception {
        final HttpsServer server = HttpsServer.create(new InetSocketAddress(9000), 0);

        server.setHttpsConfigurator(new HttpsConfigurator(context) {
            @Override
            public void configure(final HttpsParameters httpParameters) {
                final SSLParameters sslParameters = this.getSSLContext().getDefaultSSLParameters();
                sslParameters.setWantClientAuth(true);
                httpParameters.setSSLParameters(sslParameters);
            }
        });

        server.createContext("/", exchange -> {
            final String response = "Hello SSL World!";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        server.start();

        return () -> server.stop(1);
    }
}
