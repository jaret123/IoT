package com.elyxor.common.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.elyxor.common.error.HttpErrorReason;

/**
 * @author Kemal Yurderi
 *
 *  This class is designed to cache the responses from web requests when processing
 *   lots of https, to avoid re-requesting the same failed url over and over again.
 */
public class HttpResponseManager {

	private static final Logger log = Logger.getLogger(HttpResponseManager.class);

	private static HttpResponseManager mInstance;

	private static Map<String, String> resolvedURLCache = new HashMap<String, String>();

	private static Map<String, HttpErrorReason> failedURLs = new HashMap<String, HttpErrorReason>();
	private static Map<String, HttpErrorReason> retryURLs = new HashMap<String, HttpErrorReason>();

	public static final Map<Integer, String> errorCodesMap = Collections.unmodifiableMap(new HashMap<Integer, String>() {

		/**
         * 
         */
		private static final long serialVersionUID = 1238612946L;

		{
			put(400, "Bad Request");
			put(401, "Unauthorized");
			put(402, "Payment Required");
			put(403, "Forbidden");
			put(405, "Method Not Allowed");
			put(406, "Not Acceptable");
			put(407, "Proxy Authentication Required");
			put(409, "Conflict");
			put(411, "Length Required");
			put(412, "Precondition Failed");
			put(413, "Request Entity Too Large");
			put(414, "Request-URI Too Large");
			put(415, "Unsupported Media Type");
			put(416, "Requested range not satisfiable");
			put(417, "Expectation Failed");

		}
	});

	public static final Map<Integer, String> retryCodesMap = Collections.unmodifiableMap(new HashMap<Integer, String>() {

		/**
         * 
         */
		private static final long serialVersionUID = 1238612946L;

		{
			put(404, "Not Found");
			put(408, "Request Time-out");
			put(410, "Gone");
			put(500, "Internal Server Error");
			put(501, "Not Implemented");
			put(502, "Bad Gateway");
			put(503, "Service Unavailable");
			put(504, "Gateway Time-out");
		}
	});

	public static HttpResponseManager sharedHttpResponseManager() {
		if (mInstance == null) {
			mInstance = new HttpResponseManager();
		}
		return mInstance;
	}

	private HttpResponseManager() {

	}

	/**
	 * This method returns the resolved URL by using Http connection and checking the response
	 * 
	 * @param encodedURL
	 * @return resolvedURL
	 */
	public String getResolvedURL(String encodedURL) {

		String origURL = encodedURL;

		if (encodedURL == null) {
			log.debug("Cannot resolve URL for null encodedURL");
			return "";
		}

		try {
			// If the URL is in cache, then return the resolved one
			if (resolvedURLCache.containsKey(origURL)) {
				return resolvedURLCache.get(origURL);
			}

			// If the URL is in the "black list" then do not try to resolve it
			if (failedURLs.containsKey(origURL)) {
				return "";
			}
			
			if (retryURLs.containsKey(origURL)) {
				if (retryURLs.get(origURL).getRetry() >= HttpErrorReason.MAX_RETRIES) {
					log.warn(origURL + " has exceeded the max number of retries (" + HttpErrorReason.MAX_RETRIES
					        + ")- will not proceed with trying to resolve this url!");
					return "";
				}
			}

			URL url = new URL(encodedURL);
			//using proxy may increase latency
			HttpURLConnection hConn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
			// force no follow
			hConn.setInstanceFollowRedirects(false);
			// we don't really care about what the content actually is       
			hConn.setRequestMethod("GET");
			// lets set it to 10sec
			hConn.setConnectTimeout(10000);
			hConn.setReadTimeout(10000);
			hConn.connect();
			int responseCode = hConn.getResponseCode();

			if (retryCodesMap.containsKey(responseCode)) {
				if (!retryURLs.containsKey(origURL)) {
					log.info("Adding to retry list due to http response: " + responseCode + " - " + origURL);
					retryURLs.put(origURL, new HttpErrorReason(responseCode, retryCodesMap.get(responseCode)));
				} else if (retryURLs.get(origURL).getRetry() < HttpErrorReason.MAX_RETRIES) {
					retryURLs.get(origURL).incrementRetry();
				}

				hConn.getInputStream().close();
				return "";
			}
			if (errorCodesMap.containsKey(responseCode)) {
				if (!failedURLs.containsKey(origURL)) {
					log.info("Adding to failure list due to http response: " + responseCode + " - " + origURL);
					failedURLs.put(origURL, new HttpErrorReason(responseCode, errorCodesMap.get(responseCode)));
				}
				hConn.getInputStream().close();
				return "";

			}
			hConn.getInputStream().close();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				resolvedURLCache.put(origURL, encodedURL);
				return encodedURL;
			}

			String loc = hConn.getHeaderField("Location");
			if (loc != null) {

				String resolvedURL = "";
				switch (responseCode) {
					case HttpURLConnection.HTTP_MOVED_PERM: {
						resolvedURL = loc.replaceAll(" ", "+");
						resolvedURLCache.put(origURL, resolvedURL);
						break; 
					}
					case HttpURLConnection.HTTP_MOVED_TEMP: 
					case HttpURLConnection.HTTP_SEE_OTHER: {
						resolvedURL = loc.replaceAll(" ", "+");
					}
				}
				
				return resolvedURL;
			}
		} catch (MalformedURLException e) {
			log.error("HttpResponseManager - MalformedURLException: " + e.getLocalizedMessage());
		} catch (IOException e) {
			if (!failedURLs.containsKey(origURL)) {
				log.warn("Adding to retry list due to IO Excecption: " + e.getLocalizedMessage() + " - " + origURL);
				retryURLs.put(origURL, new HttpErrorReason(503, errorCodesMap.get(503)));
			}
		}

		return "";
	}

	public Map<String, String> getResolvedURLCache() {
		return resolvedURLCache;
	}

	public Map<String, HttpErrorReason> getFailedURLs() {
		return failedURLs;
	}

	public Map<String, HttpErrorReason> getRetryURLs() {
		return retryURLs;
	}

	public static Map<Integer, String> getErrorcodesmap() {
		return errorCodesMap;
	}

	public static Map<Integer, String> getRetrycodesmap() {
		return retryCodesMap;
	}
}
