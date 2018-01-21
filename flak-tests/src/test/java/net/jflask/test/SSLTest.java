package net.jflask.test;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import flak.App;
import flak.AppFactory;
import flak.Flak;
import flak.annotations.Route;
import net.jflask.test.util.SimpleClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author pcdv
 */
public class SSLTest {

  private App app;

  @Route("/")
  public String test() {
    return "OK";
  }

  @After
  public void tearDown() throws Exception {
    app.stop();
  }

  @Test
  public void testIt() throws Exception {

    trustAllCertificates();

    AppFactory factory = Flak.getFactory();
    factory.getServer().setSSLContext(getSslContext());

    factory.setPort(9191);
    app = factory.createApp();
    app.scan(this);
    app.start();

    Assert.assertEquals("OK", new SimpleClient(app.getRootUrl()).get("/"));
  }

  // https://stackoverflow.com/questions/2308479/simple-java-https-server
  private SSLContext getSslContext() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");

    // initialise the keystore
    char[] password = "simulator".toCharArray();
    KeyStore ks = KeyStore.getInstance("JKS");
    InputStream fis = getClass().getResourceAsStream("/lig.keystore");
    ks.load(fis, password);

    // setup the key manager factory
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ks, password);

    // setup the trust manager factory
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(ks);

    // setup the HTTPS context and parameters
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return sslContext;
  }

  // http://stacktips.com/snippet/how-to-trust-all-certificates-for-httpurlconnection-in-android
  public void trustAllCertificates() {
    try {
      TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

          @Override
          public void checkClientTrusted(X509Certificate[] certs,
                                         String authType) {
          }

          @Override
          public void checkServerTrusted(X509Certificate[] certs,
                                         String authType) {
          }
        }
      };

      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
    }
    catch (Exception e) {
    }
  }
}
