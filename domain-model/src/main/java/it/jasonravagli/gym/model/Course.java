package it.jasonravagli.gym.model;

import java.util.Objects;
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
		return name + " - subscribers=" + (subscribers == null ? 0 : subscribers.size());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(name, subscribers);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Course other = (Course) obj;
		return Objects.equals(getId(), other.getId()) && Objects.equals(name, other.name)
				&& Objects.equals(subscribers, other.subscribers);
	}
}
