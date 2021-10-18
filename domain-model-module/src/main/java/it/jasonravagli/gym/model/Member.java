package it.jasonravagli.gym.model;

import java.time.LocalDate;

public class Member extends BaseEntity {
	private String name;
	private String surname;
	private LocalDate dateOfBirth;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	@Override
	public String toString() {
		return surname + " " + name + " - " + dateOfBirth;
	}
}
