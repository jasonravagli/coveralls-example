package it.jasonravagli.gym.logic;

import java.util.List;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public class GymController {

	private GymView gymView;
	private TransactionManager transactionManager;

	public GymController(GymView gymView, TransactionManager transactionManager) {
		this.gymView = gymView;
		this.transactionManager = transactionManager;
	}

	public void allMembers() {
		try {
			List<Member> members = transactionManager
					.doInTransaction(repositoryProvider -> repositoryProvider.getMemberRepository().findAll());
			gymView.showMembers(members);
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void allCourses() {
		try {
			List<Course> courses = transactionManager
					.doInTransaction(repositoryProvider -> repositoryProvider.getCourseRepository().findAll());
			gymView.showCourses(courses);
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void addMember(Member member) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				MemberRepository memberRepository = repositoryProvider.getMemberRepository();
				if (memberRepository.findById(member.getId()) != null) {
					gymView.showError("A member with id " + member.getId() + " already exists");
					return null;
				}

				memberRepository.save(member);
				gymView.memberAdded(member);
				return null;
			});
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void deleteMember(Member member) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				MemberRepository memberRepository = repositoryProvider.getMemberRepository();
				if (memberRepository.findById(member.getId()) == null) {
					gymView.showError("Member with id " + member.getId() + " does not exist");
					return null;
				}
				repositoryProvider.getMemberRepository().deleteById(member.getId());
				gymView.memberDeleted(member);
				return null;
			});
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void updateMember(Member member) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				MemberRepository memberRepository = repositoryProvider.getMemberRepository();
				if (memberRepository.findById(member.getId()) == null) {
					gymView.showError("Member with id " + member.getId() + " does not exist");
					return null;
				}

				memberRepository.update(member);
				gymView.memberUpdated(member);
				return null;
			});
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void addCourse(Course course) {
		try {
			transactionManager.doInTransaction(repositoryprovider -> {
				CourseRepository courseRepository = repositoryprovider.getCourseRepository();
				if (courseRepository.findById(course.getId()) != null) {
					gymView.showError("A course with id " + course.getId() + " already exists");
					return null;
				}
				courseRepository.save(course);
				gymView.courseAdded(course);
				return null;
			});
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void deleteCourse(Course course) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				CourseRepository courseRepository = repositoryProvider.getCourseRepository();
				if (courseRepository.findById(course.getId()) == null) {
					gymView.showError("Course with id " + course.getId() + " does not exist");
					return null;
				}
				courseRepository.deleteById(course.getId());
				gymView.courseDeleted(course);
				return null;
			});
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

	public void updateCourse(Course updatedCourse) {
		try {
			transactionManager.doInTransaction(repositoryProvider -> {
				CourseRepository courseRepository = repositoryProvider.getCourseRepository();
				if (courseRepository.findById(updatedCourse.getId()) == null) {
					gymView.showError("Course with id " + updatedCourse.getId() + " does not exist");
					return null;
				}
				courseRepository.update(updatedCourse);
				gymView.courseUpdated(updatedCourse);
				return null;
			});
		} catch (RuntimeException e) {
			gymView.showError(e.getMessage());
		}
	}

}
