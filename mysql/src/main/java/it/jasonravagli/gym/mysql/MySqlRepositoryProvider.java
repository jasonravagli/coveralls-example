package it.jasonravagli.gym.mysql;

import it.jasonravagli.gym.logic.CourseRepository;
import it.jasonravagli.gym.logic.MemberRepository;
import it.jasonravagli.gym.logic.RepositoryProvider;

public class MySqlRepositoryProvider implements RepositoryProvider {

	private MySqlMemberRepository memberRepository;
	private MySqlCourseRepository courseRepository;
	
	public MySqlRepositoryProvider(MySqlMemberRepository memberRepository, MySqlCourseRepository courseRepository) {
		this.memberRepository = memberRepository;
		this.courseRepository = courseRepository;
	}

	@Override
	public MemberRepository getMemberRepository() {
		return memberRepository;
	}

	@Override
	public CourseRepository getCourseRepository() {
		return courseRepository;
	}

}
