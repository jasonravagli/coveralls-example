package it.jasonravagli.gym.logic;

import java.util.List;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public interface GymView {
		
	void showCourses(List<Course> courses);
	
	void showError(String message);
	
	void showMembers(List<Member> members);

	void memberAdded(Member member);

	void memberDeleted(Member member);

	void memberUpdated(Member updatedMember);

	void courseAdded(Course course);

	void courseDeleted(Course course);

	void courseUpdated(Course updatedCourse);
	
}
