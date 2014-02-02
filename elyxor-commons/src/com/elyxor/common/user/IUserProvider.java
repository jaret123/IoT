package com.elyxor.common.user;

import java.util.List;

import com.elyxor.common.document.User;

public interface IUserProvider {

	public List<User> getUsers();
	public List<User> getUsers(int limit, int offset);
	public User getByTwitterName(String twitterName);
	public void add(String firstName, String lastName, String twitterName);
    public void remove(String twitterName);
}
