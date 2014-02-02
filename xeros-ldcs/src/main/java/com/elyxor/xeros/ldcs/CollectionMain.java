package com.elyxor.xeros.ldcs;

public class CollectionMain {

	public static void main(String[] args) {
		try {
			new FileWatcher().watch();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
