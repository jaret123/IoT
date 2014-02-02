package com.elyxor.common.error;

public class HttpErrorReason {

	public static final int MAX_RETRIES = 4;

	private int errorCode;
	private String errorReason;
	private int retry;

	public HttpErrorReason(int errorCode, String errorReason) {
		this.errorCode = errorCode;
		this.errorReason = errorReason;
		this.retry = 0;
	}

	public void incrementRetry() {
		this.retry++;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorReason() {
		return errorReason;
	}

	public void setErrorReason(String errorReason) {
		this.errorReason = errorReason;
	}

	public int getRetry() {
		return retry;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}

}
