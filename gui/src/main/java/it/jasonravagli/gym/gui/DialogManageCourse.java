package it.jasonravagli.gym.gui;

import it.jasonravagli.gym.logic.GymView;
import it.jasonravagli.gym.model.Course;

public interface DialogManageCourse extends GymView {

	void setCourse(Course course);

	DialogResult showDialog();
}
