package com.elyxor.common.db.auth;

public interface IDataStoreAuthProvider {

	public String getHost();

	public int getPort();

	public String getDBName();

	public String getUsername();

	public char[] getPassword();
}
