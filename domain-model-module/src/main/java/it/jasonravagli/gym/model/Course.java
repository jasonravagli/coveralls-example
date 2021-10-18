package it.jasonravagli.gym.model;

import java.util.Set;

public class Course extends BaseEntity {
	private String name;
	private Set<Member> subscribers;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Member> getSubscribers() {
		return subscribers;
	}

	public void setSubscribers(Set<Member> subscribers) {
		this.subscribers = subscribers;
	}

	@Override
	public String toString() {
		return name + " - subscribers=" + (subscribers == null? 0 : subscribers.size());
	}
}
