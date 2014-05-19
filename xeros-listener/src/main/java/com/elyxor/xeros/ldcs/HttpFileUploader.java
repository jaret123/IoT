package com.elyxor.xeros.ldcs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.TimeZone;

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
		
	public int postFile(Path filePath, long createTime) {
		int httpStatus = 0;
		try {
			String contentType = Files.probeContentType(filePath);
			File file = filePath.toFile();
			String url = AppConfiguration.getServiceUrl();
			HttpPost post = new HttpPost(url);
			HttpClient client = getHttpClient(url);
			HttpEntity httpEntity = MultipartEntityBuilder.create()
			    .addBinaryBody("file", file, ContentType.create(contentType), file.getName())
			    .addTextBody("current_system_time", Long.toString(System.currentTimeMillis()))
			    .addTextBody("file_create_time", Long.toString(createTime))
			    .addTextBody("olson_timezone_id", TimeZone.getDefault().getID())
			    .addTextBody("location_id", AppConfiguration.getLocationId())
			    .build();
			logger.info(String.format("Uploading %1s to %2s", file, url));
			post.setEntity(httpEntity);
			HttpResponse response = client.execute(post);
			HttpEntity resEntity = response.getEntity();
	        String responseString = EntityUtils.toString(resEntity);	        
	        if ( response.getStatusLine().getStatusCode() != 200 ) {
	        	logger.warn(String.format("Failed to upload file; received \n %1s", responseString));	        	
	        }
	        httpStatus = response.getStatusLine().getStatusCode();
		} catch (Exception ex) {
			logger.error("Failed to post file", ex);
		}
		return httpStatus;
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
