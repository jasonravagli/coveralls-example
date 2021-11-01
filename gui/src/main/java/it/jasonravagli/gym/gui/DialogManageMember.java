package it.jasonravagli.gym.gui;

import it.jasonravagli.gym.logic.GymView;
import it.jasonravagli.gym.model.Member;

public interface DialogManageMember extends GymView {

	void setMember(Member member);
	
	DialogResult showDialog();
	
	void setModalState(boolean modal);
}
