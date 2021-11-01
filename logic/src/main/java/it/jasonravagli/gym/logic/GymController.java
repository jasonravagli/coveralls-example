package it.jasonravagli.gym.logic;

import java.util.List;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public class GymController {

	private static final String NOT_EXISTING_COURSE_MSG_TEMPLATE = "Course with id %s does not exist";
	private static final String EXISTING_COURSE_MSG_TEMPLATE = "A course with id %s already exists";
	private static final String NOT_EXISTING_MEMBER_MSG_TEMPLATE = "Member with id %s does not exist";
	private static final String EXISTING_MEMBER_MSG_TEMPLATE = "A member with id %s already exists";

	private GymView gymView;
	private TransactionManager transactionManager;
	
	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	public void setView(GymView gymView) {
		this.gymView = gymView;
	}

	public void allMembers() {
		try {
			List<Member> members = transactionManager
					.doInTransaction(repositoryProvider -> repositoryProvider.getMemberRepository().findAll());
			gymView.showMembers(members);
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void allCourses() {
		try {
			List<Course> courses = transactionManager
					.doInTransaction(repositoryProvider -> repositoryProvider.getCourseRepository().findAll());
			gymView.showCourses(courses);
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void addMember(Member member) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				MemberRepository memberRepository = repositoryProvider.getMemberRepository();
				if (memberRepository.findById(member.getId()) != null) {
					gymView.showError(String.format(EXISTING_MEMBER_MSG_TEMPLATE, member.getId()));
					return null;
				}

				memberRepository.save(member);
				gymView.memberAdded(member);
				return null;
			});
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void deleteMember(Member member) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				MemberRepository memberRepository = repositoryProvider.getMemberRepository();
				if (memberRepository.findById(member.getId()) == null) {
					gymView.showError(String.format(NOT_EXISTING_MEMBER_MSG_TEMPLATE, member.getId()));
					return null;
				}
				repositoryProvider.getMemberRepository().deleteById(member.getId());
				gymView.memberDeleted(member);
				return null;
			});
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void updateMember(Member member) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				MemberRepository memberRepository = repositoryProvider.getMemberRepository();
				if (memberRepository.findById(member.getId()) == null) {
					gymView.showError(String.format(NOT_EXISTING_MEMBER_MSG_TEMPLATE, member.getId()));
					return null;
				}

				memberRepository.update(member);
				gymView.memberUpdated(member);
				return null;
			});
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void addCourse(Course course) {
		try {
			transactionManager.doInTransaction(repositoryprovider -> {
				CourseRepository courseRepository = repositoryprovider.getCourseRepository();
				if (courseRepository.findById(course.getId()) != null) {
					gymView.showError(String.format(EXISTING_COURSE_MSG_TEMPLATE, course.getId()));
					return null;
				}
				courseRepository.save(course);
				gymView.courseAdded(course);
				return null;
			});
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void deleteCourse(Course course) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				CourseRepository courseRepository = repositoryProvider.getCourseRepository();
				if (courseRepository.findById(course.getId()) == null) {
					gymView.showError(String.format(NOT_EXISTING_COURSE_MSG_TEMPLATE, course.getId()));
					return null;
				}
				courseRepository.deleteById(course.getId());
				gymView.courseDeleted(course);
				return null;
			});
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void updateCourse(Course updatedCourse) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				CourseRepository courseRepository = repositoryProvider.getCourseRepository();
				if (courseRepository.findById(updatedCourse.getId()) == null) {
					gymView.showError(String.format(NOT_EXISTING_COURSE_MSG_TEMPLATE, updatedCourse.getId()));
					return null;
				}
				courseRepository.update(updatedCourse);
				gymView.courseUpdated(updatedCourse);
				return null;
			});
		} catch (TransactionException e) {
			gymView.showError(e.getMessage());
		}
	}

}
