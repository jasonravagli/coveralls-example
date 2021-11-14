package it.jasonravagli.gym.logic;

import java.util.List;
import java.util.UUID;

import it.jasonravagli.gym.model.Course;

public interface CourseRepository {

	List<Course> findAll() throws Exception;

	Course findById(UUID idCourse) throws Exception;

	void save(Course course) throws Exception;

	void deleteById(UUID idCourse) throws Exception;

	void update(Course updatedCourse) throws Exception;
	
}
