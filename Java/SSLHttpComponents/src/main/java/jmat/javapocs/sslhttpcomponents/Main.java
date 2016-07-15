package jmat.javapocs.sslhttpcomponents;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Future;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

public class Main {

    public static void main(final String[] args) throws Exception {
        final SSLContext serverContext = createSSLContext("../stores/server.pkcs12", "server-password");
        try (final AutoCloseable server = createServer(serverContext)) {
            System.out.println("Serving...");

            final SSLContext clientContext = createSSLContext("../../certs/certs/browser.p12", "browser-password");
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(clientContext, new NoopHostnameVerifier());

            final HttpGet httpGet = new HttpGet("https://localhost:9000/");

            try (final CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(factory)
                .build()) {

                System.out.println("Sync:");
                try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    System.out.println("\t" + response.getStatusLine());
                    final String data = EntityUtils.toString(response.getEntity());
                    System.out.println("\t" + data);
                }
            }
            System.out.println();

            try (final CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.custom()
                .setSSLContext(clientContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build()) {
                System.out.println("Async:");
                httpAsyncClient.start();
                final Future<HttpResponse> future = httpAsyncClient.execute(httpGet, new FutureCallback<HttpResponse>() {

                    @Override
                    public void completed(final HttpResponse response) {
                        try {
                            System.out.println("\tAsync http request completed.");
                            System.out.println("\t" + response.getStatusLine());
                            final String data = EntityUtils.toString(response.getEntity());
                            System.out.println("\t" + data);
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void failed(final Exception ex) {
                        System.out.println("\tAsync http request failed.");
                        ex.printStackTrace();
                    }

                    @Override
                    public void cancelled() {
                        System.out.println("\tAsync http request cancelled.");
                    }
                });
                System.out.println("\tWaiting for async request...");
                future.get();
            }
            System.out.println();
        }

        System.out.println("Done");
    }

    private static SSLContext createSSLContext(final String keyStoreFile, final String keyStorePassword) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (final FileInputStream keyStoreStream = new FileInputStream(keyStoreFile)) {
            keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
        }

        final SSLContext context = new SSLContextBuilder()
            .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
            .loadTrustMaterial(new File("../stores/trustStore"), "trust-store-password".toCharArray())
            .build();

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
