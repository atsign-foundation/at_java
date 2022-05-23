package org.atsign.common;

import org.atsign.common.Keys.AtKey;

public class AtEntry<K extends AtKey, V> {
	
	private K key;
	private V value;
	
	public AtEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}
}
