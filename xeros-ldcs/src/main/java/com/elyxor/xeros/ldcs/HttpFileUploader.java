package com.elyxor.xeros.ldcs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpFileUploader {
	
	private static Logger logger = LoggerFactory.getLogger(HttpFileUploader.class);
		
	public void postFile(Path filePath) {
		try {
			String contentType = Files.probeContentType(filePath);
			File file = filePath.toFile();
			String url = AppConfiguration.getServiceUrl();
			HttpPost post = new HttpPost(url);
			HttpClient client = getHttpClient(url);
			HttpEntity httpEntity = MultipartEntityBuilder.create()
			    .addBinaryBody("file", file, ContentType.create(contentType), file.getName())
			    .build();
			post.setEntity(httpEntity);
			HttpResponse response = client.execute(post);
			HttpEntity resEntity = response.getEntity();
	        // if ( response.getStatusLine().getStatusCode() == 200 ) {	        }	        
	        String responseString = EntityUtils.toString(resEntity);
	        
		} catch (Exception ex) {
			logger.error("Failed to post file", ex);
		}
	}

	public HttpClient getHttpClient(String url) throws Exception {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		if ( url.startsWith("https:")) {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			// set up a TrustManager that trusts everything
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
	
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} }, new SecureRandom());			
			SSLConnectionSocketFactory sslf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			PlainConnectionSocketFactory sf = PlainConnectionSocketFactory.getSocketFactory();
			clientBuilder.setSslcontext(sslContext);
			clientBuilder.setSSLSocketFactory(sslf);
			
			Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
			        .register("http", sf)
			        .register("https", sslf)
			        .build();

			HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);			
			clientBuilder.setConnectionManager(cm);			
		}
		return clientBuilder.build();
	}

}
