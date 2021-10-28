package it.jasonravagli.gym.logic;

public interface RepositoryProvider {

	public MemberRepository getMemberRepository();

	public CourseRepository getCourseRepository();

}
