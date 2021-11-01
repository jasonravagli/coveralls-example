package it.jasonravagli.gym.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;
import net.miginfocom.swing.MigLayout;

public class SwingDialogManageSubs extends JFrame implements DialogManageCourse {

	private static final long serialVersionUID = 9142481637663905420L;
	private static final String UNSUPPORTED_OP_MESSAGE = "Operation not supported";

	private final JPanel contentPanel = new JPanel();
	private DefaultListModel<Member> listModelSubs;
	private DefaultListModel<Member> listModelOtherMembers;
	private JButton buttonAddSub;
	private JButton buttonRemoveSub;
	private JLabel labelError;

	private DialogResult result;

	private transient GymController controller;

	private transient Course course;

	/**
	 * Create the frame.
	 * 
	 * @param controller
	 */
	public SwingDialogManageSubs(GymController controller) {
		this.controller = controller;
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setBounds(100, 100, 595, 369);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("fill",
				"[grow,sizegroup col,left][grow,sizegroup col,center][grow,sizegroup col-s,center][grow,sizegroup col,left][grow,sizegroup col,center]",
				"[grow,sizegroup row-s][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row-s]"));

		JLabel lblNewLabel = new JLabel("Subscribers");
		contentPanel.add(lblNewLabel, "cell 0 0");

		JLabel lblNewLabel_1 = new JLabel("All");
		contentPanel.add(lblNewLabel_1, "cell 3 0");

		JScrollPane scrollPane = new JScrollPane();
		contentPanel.add(scrollPane, "cell 0 1 2 4,grow");

		listModelSubs = new DefaultListModel<>();
		JList<Member> listSubs = new JList<>(listModelSubs);
		listSubs.addListSelectionListener(e -> buttonRemoveSub.setEnabled(listSubs.getSelectedIndex() != -1));
		listSubs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listSubs.setName("listSubs");
		scrollPane.setViewportView(listSubs);

		JScrollPane scrollPane_1 = new JScrollPane();
		contentPanel.add(scrollPane_1, "cell 3 1 2 4,grow");

		listModelOtherMembers = new DefaultListModel<>();
		JList<Member> listOtherMembers = new JList<>(listModelOtherMembers);
		listOtherMembers
				.addListSelectionListener(e -> buttonAddSub.setEnabled(listOtherMembers.getSelectedIndex() != -1));
		listOtherMembers.setName("listOtherMembers");
		listOtherMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane_1.setViewportView(listOtherMembers);

		buttonAddSub = new JButton("<");
		buttonAddSub.addActionListener(e -> {
			labelError.setText(" ");

			Member member = listOtherMembers.getSelectedValue();
			course.getSubscribers().add(member);

			listModelSubs.addElement(member);
			listModelOtherMembers.removeElement(member);
		});
		buttonAddSub.setName("buttonAddSub");
		buttonAddSub.setEnabled(false);
		contentPanel.add(buttonAddSub, "cell 2 2");

		buttonRemoveSub = new JButton(">");
		buttonRemoveSub.addActionListener(e -> {
			labelError.setText(" ");

			Member member = listSubs.getSelectedValue();
			course.getSubscribers().remove(member);

			listModelSubs.removeElement(member);
			listModelOtherMembers.addElement(member);
		});
		buttonRemoveSub.setName("buttonRemoveSub");
		buttonRemoveSub.setEnabled(false);
		contentPanel.add(buttonRemoveSub, "cell 2 3");

		labelError = new JLabel(" ");
		labelError.setName("labelError");
		contentPanel.add(labelError, "cell 1 5 3 1");

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		JButton buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(e -> {
			result = DialogResult.CANCEL;
			setVisible(false);
		});
		buttonCancel.setName("buttonCancel");
		buttonPane.add(buttonCancel);
		JButton buttonOk = new JButton("OK");
		buttonOk.addActionListener(e -> {
			labelError.setText(" ");
			controller.updateCourse(course);
		});
		buttonOk.setName("buttonOk");
		buttonPane.add(buttonOk);
		getRootPane().setDefaultButton(buttonOk);
	}

	@Override
	public void showCourses(List<Course> courses) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void showError(String message) {
		SwingUtilities.invokeLater(() -> labelError.setText(message));
	}

	@Override
	public void showMembers(List<Member> members) {
		members.removeAll(course.getSubscribers());

		SwingUtilities.invokeLater(() -> {
			members.forEach(listModelOtherMembers::addElement);
			course.getSubscribers().forEach(listModelSubs::addElement);
		});
	}

	@Override
	public void memberAdded(Member member) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void memberDeleted(Member member) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void memberUpdated(Member updatedMember) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void courseAdded(Course course) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void courseDeleted(Course course) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void courseUpdated(Course updatedCourse) {
		result = DialogResult.OK;
		setVisible(false);
	}

	@Override
	public DialogResult showDialog() {
		if (course == null)
			throw new InvalidCourseException("Course cannot be null");

		result = DialogResult.CANCEL;

		SwingUtilities.invokeLater(() -> course.getSubscribers().forEach(listModelSubs::addElement));
		controller.allMembers();

		setVisible(true);

		return result;
	}

	@Override
	public void setCourse(Course course) {
		this.course = course;
	}

	/*
	 * Only for testing purposes
	 */
	Course getCourse() {
		return course;
	}

	/*
	 * Only for testing purposes
	 */
	DialogResult getResult() {
		return result;
	}

	/*
	 * Only for testing purposes
	 */
	void setResult(DialogResult result) {
		this.result = result;
	}

	/*
	 * Only for testing purposes
	 */
	DefaultListModel<Member> getListModelSubs() {
		return listModelSubs;
	}

	/*
	 * Only for testing purposes
	 */
	DefaultListModel<Member> getListModelOtherMembers() {
		return listModelOtherMembers;
	}

}
