package it.jasonravagli.gym.gui;

import java.awt.Color;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.logic.GymView;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;
import net.miginfocom.swing.MigLayout;
import javax.swing.ListSelectionModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SwingGymView extends JFrame implements GymView {

	private static final String UNSUPPORTED_OP_MESSAGE = "Operation not supported";

	private static final long serialVersionUID = 2358001748821893268L;

	private JPanel contentPane;
	private DefaultListModel<Member> listModelMembers;
	private DefaultListModel<Course> listModelCourses;
	private JList<Member> listMembers;
	private JList<Course> listCourses;
	private JLabel labelError;

	public SwingGymView(GymController controller, DialogManageMember dialogManageMember,
			DialogManageCourse dialogManageCourse, DialogManageCourse dialogManageSubs) {
		setTitle("Gym Manager");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setBounds(100, 100, 899, 411);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		JTabbedPane tabbedPaneMain = new JTabbedPane(SwingConstants.TOP);
		tabbedPaneMain.setName("tabbedPaneMain");
		contentPane.add(tabbedPaneMain);

		JPanel panel = new JPanel();
		tabbedPaneMain.addTab("Members", null, panel, null);
		panel.setLayout(new MigLayout("fill",
				"[grow,center,sg col][grow,center,sg col][grow,center,sg col-s][grow,center,sg col][grow,center,sg col-s][grow,center,sg col][grow,center,sg col]",
				"[grow,sg row][grow,sg row][grow,sg row][grow,sg row][grow,sg row][grow,sg row][grow,sg row][grow,sg row]"));

		JButton buttonRefreshMembers = new JButton("Refresh");
		buttonRefreshMembers.addActionListener(e -> controller.allMembers());
		buttonRefreshMembers.setName("buttonRefreshMembers");
		panel.add(buttonRefreshMembers, "cell 0 0");

		JButton buttonDeleteMember = new JButton("Delete");
		buttonDeleteMember.addActionListener(e -> controller.deleteMember(listMembers.getSelectedValue()));
		buttonDeleteMember.setEnabled(false);
		buttonDeleteMember.setName("buttonDeleteMember");
		panel.add(buttonDeleteMember, "cell 0 7");

		JButton buttonUpdateMember = new JButton("Update");
		buttonUpdateMember.setEnabled(false);
		buttonUpdateMember.setName("buttonUpdateMember");
		buttonUpdateMember.addActionListener(e -> {
			controller.setView(dialogManageMember);
			dialogManageMember.setMember(listMembers.getSelectedValue());
			
			DialogResult dialogResult = dialogManageMember.showDialog();
			controller.setView(this);
			
			if (dialogResult == DialogResult.OK)
				controller.allMembers();
		});
		panel.add(buttonUpdateMember, "cell 1 7");

		listModelMembers = new DefaultListModel<>();
		listMembers = new JList<>(listModelMembers);
		listMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listMembers.addListSelectionListener(e -> {
			boolean enabled = listMembers.getSelectedIndex() != -1;
			buttonDeleteMember.setEnabled(enabled);
			buttonUpdateMember.setEnabled(enabled);
		});
		listMembers.setName("listMembers");
		panel.add(listMembers, "cell 0 1 7 6,grow");

		JButton buttonAddMember = new JButton("Add");
		buttonAddMember.setName("buttonAddMember");
		buttonAddMember.addActionListener(e -> {
			controller.setView(dialogManageMember);
			
			dialogManageMember.setModalState(true);
			DialogResult result = dialogManageMember.showDialog();
			controller.setView(this);
			
			if (result == DialogResult.OK)
				controller.allMembers();
		});
		panel.add(buttonAddMember, "cell 3 7");

		JPanel panel_1 = new JPanel();
		tabbedPaneMain.addTab("Courses", null, panel_1, null);
		panel_1.setLayout(new MigLayout("fill",
				"[grow,sizegroup col,center][grow,sizegroup col,center][grow,sizegroup col-s,center][grow,sizegroup col,center][grow,sizegroup col-s,center][grow,sizegroup col,center][grow,sizegroup col,center]",
				"[grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row]"));

		JButton buttonRefreshCourses = new JButton("Refresh");
		buttonRefreshCourses.addActionListener(e -> controller.allCourses());
		buttonRefreshCourses.setName("buttonRefreshCourses");
		panel_1.add(buttonRefreshCourses, "cell 0 0");

		JButton buttonDeleteCourse = new JButton("Delete");
		buttonDeleteCourse.addActionListener(e -> controller.deleteCourse(listCourses.getSelectedValue()));
		buttonDeleteCourse.setEnabled(false);
		buttonDeleteCourse.setName("buttonDeleteCourse");
		panel_1.add(buttonDeleteCourse, "cell 0 7");

		JButton buttonUpdateCourse = new JButton("Update");
		buttonUpdateCourse.addActionListener(e -> {
			controller.setView(dialogManageCourse);
			dialogManageCourse.setCourse(listCourses.getSelectedValue());
			
			DialogResult dialogResult = dialogManageCourse.showDialog();
			controller.setView(this);
			
			if (dialogResult == DialogResult.OK)
				controller.allCourses();
		});
		buttonUpdateCourse.setEnabled(false);
		buttonUpdateCourse.setName("buttonUpdateCourse");
		panel_1.add(buttonUpdateCourse, "cell 1 7");

		JButton buttonAddCourse = new JButton("Add");
		buttonAddCourse.addActionListener(e -> {
			controller.setView(dialogManageCourse);
			
			DialogResult result = dialogManageCourse.showDialog();
			controller.setView(this);
			
			if (result == DialogResult.OK)
				controller.allCourses();
		});
		buttonAddCourse.setName("buttonAddCourse");
		panel_1.add(buttonAddCourse, "cell 3 7");

		JButton buttonManageSubs = new JButton("Manage Subs.");
		buttonManageSubs.addActionListener(e -> {
			dialogManageSubs.setCourse(listCourses.getSelectedValue());
			if (dialogManageSubs.showDialog() == DialogResult.OK)
				controller.allCourses();
		});
		buttonManageSubs.setEnabled(false);
		buttonManageSubs.setName("buttonManageSubs");
		panel_1.add(buttonManageSubs, "cell 6 7");

		listModelCourses = new DefaultListModel<>();
		listCourses = new JList<>(listModelCourses);
		listCourses.addListSelectionListener(e -> {
			boolean enabled = listCourses.getSelectedIndex() != -1;
			buttonDeleteCourse.setEnabled(enabled);
			buttonUpdateCourse.setEnabled(enabled);
			buttonManageSubs.setEnabled(enabled);
		});
		listCourses.setName("listCourses");
		panel_1.add(listCourses, "cell 0 1 7 6,grow");
		
		tabbedPaneMain.addChangeListener(e -> {
			if (tabbedPaneMain.getSelectedIndex() == 1)
				controller.allCourses();
			else
				controller.allMembers();
		});

		labelError = new JLabel(" ");
		labelError.setName("labelError");
		labelError.setForeground(Color.RED);
		contentPane.add(labelError);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				controller.allMembers();
			}
		});
	}

	/*
	 * Only for testing purposes
	 */
	DefaultListModel<Member> getListModelMembers() {
		return listModelMembers;
	}

	/*
	 * Only for testing purposes
	 */
	DefaultListModel<Course> getListModelCourses() {
		return listModelCourses;
	}

	@Override
	public void showCourses(List<Course> courses) {
		SwingUtilities.invokeLater(() -> {
			labelError.setText(" ");
			listModelCourses.clear();
			courses.forEach(listModelCourses::addElement);
		});
	}

	@Override
	public void showError(String message) {
		SwingUtilities.invokeLater(() -> labelError.setText(message));
	}

	@Override
	public void showMembers(List<Member> members) {
		SwingUtilities.invokeLater(() -> {
			labelError.setText(" ");
			listModelMembers.clear();
			members.forEach(listModelMembers::addElement);
		});
	}

	@Override
	public void memberAdded(Member member) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);

	}

	@Override
	public void memberDeleted(Member member) {
		SwingUtilities.invokeLater(() -> {
			labelError.setText(" ");
			listModelMembers.removeElement(member);
		});
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
		SwingUtilities.invokeLater(() -> {
			labelError.setText(" ");
			listModelCourses.removeElement(course);
		});
	}

	@Override
	public void courseUpdated(Course updatedCourse) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

}
