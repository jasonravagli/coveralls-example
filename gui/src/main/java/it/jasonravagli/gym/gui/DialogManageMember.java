package it.jasonravagli.gym.gui;

import it.jasonravagli.gym.logic.GymView;
import it.jasonravagli.gym.model.Member;

public interface DialogManageMember extends GymView {
	
	DialogResult showDialog();
	
	void setMember(Member member);
}
