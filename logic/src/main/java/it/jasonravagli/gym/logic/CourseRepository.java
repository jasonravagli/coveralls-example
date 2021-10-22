package it.jasonravagli.gym.logic;

import java.util.List;
import java.util.UUID;

import it.jasonravagli.gym.model.Course;

public interface CourseRepository {

	List<Course> findAll();

	Course findById(UUID idCourse);

	void save(Course course);

	void deleteById(UUID idCourse);

	void update(Course updatedCourse);
	
}
