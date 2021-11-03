package it.jasonravagli.gym.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;
import net.miginfocom.swing.MigLayout;

public class SwingDialogManageMember extends JDialog implements DialogManageMember {

	private static final long serialVersionUID = -7493514996863733430L;
	private static final String UNSUPPORTED_OP_MESSAGE = "Operation not supported";

	private final JPanel contentPanel = new JPanel();

	private JTextField textFieldId;
	private JTextField textFieldName;
	private JTextField textFieldSurname;
	private DatePicker datePickerBirth;
	private DialogResult result;

	private transient Member member;

	private JButton buttonOk;

	private JLabel labelError;

	public SwingDialogManageMember(GymController controller) {
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setBounds(100, 100, 465, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("fill",
				"[grow,sizegroup col-s,left][grow,sizegroup col,center][grow,sizegroup col,center]",
				"[grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row][grow,sizegroup row-s]"));

		JLabel lblNewLabel = new JLabel("Id:");
		contentPanel.add(lblNewLabel, "cell 0 0,alignx trailing");

		textFieldId = new JTextField();
		textFieldId.setName("textFieldId");
		textFieldId.setEnabled(false);
		contentPanel.add(textFieldId, "cell 1 0 2 1,growx");
		textFieldId.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("Name:");
		contentPanel.add(lblNewLabel_1, "cell 0 1,alignx trailing");

		textFieldName = new JTextField();
		textFieldName.setName("textFieldName");
		textFieldName.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateButtonOkState());
		contentPanel.add(textFieldName, "cell 1 1 2 1,growx");
		textFieldName.setColumns(10);

		JLabel lblNewLabel_2 = new JLabel("Surname:");
		contentPanel.add(lblNewLabel_2, "cell 0 2,alignx trailing");

		textFieldSurname = new JTextField();
		textFieldSurname.setName("textFieldSurname");
		textFieldSurname.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateButtonOkState());
		contentPanel.add(textFieldSurname, "cell 1 2 2 1,growx");
		textFieldSurname.setColumns(10);

		JLabel lblNewLabel_3 = new JLabel("Date of birth:");
		contentPanel.add(lblNewLabel_3, "cell 0 3,alignx trailing");

		DatePickerSettings settings = new DatePickerSettings();
		settings.setAllowKeyboardEditing(false);
		settings.setAllowEmptyDates(false);
		datePickerBirth = new DatePicker(settings);
		datePickerBirth.setName("datePickerBirth");
		datePickerBirth.setDateToToday();
		contentPanel.add(datePickerBirth, "cell 1 3 2 1,growx");

		labelError = new JLabel(" ");
		labelError.setName("labelError");
		contentPanel.add(labelError, "cell 1 4");

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
		buttonOk = new JButton("OK");
		buttonOk.addActionListener(e -> {
			labelError.setText(" ");

			Member newMember = new Member();
			newMember.setName(textFieldName.getText().trim());
			newMember.setSurname(textFieldSurname.getText().trim());
			newMember.setDateOfBirth(datePickerBirth.getDate());
			if (member == null) {
				newMember.setId(UUID.randomUUID());
				controller.addMember(newMember);
			} else {
				newMember.setId(member.getId());
				controller.updateMember(newMember);
			}
		});
		buttonOk.setEnabled(false);
		buttonOk.setName("buttonOk");
		buttonPane.add(buttonOk);
		getRootPane().setDefaultButton(buttonOk);
	}

	private void updateButtonOkState() {
		buttonOk.setEnabled(!textFieldName.getText().trim().isEmpty() && !textFieldSurname.getText().trim().isEmpty());
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
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void memberAdded(Member member) {
		result = DialogResult.OK;
		setVisible(false);
	}

	@Override
	public void memberDeleted(Member member) {
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public void memberUpdated(Member updatedMember) {
		result = DialogResult.OK;
		setVisible(false);
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
		throw new UnsupportedOperationException(UNSUPPORTED_OP_MESSAGE);
	}

	@Override
	public DialogResult showDialog() {
		result = DialogResult.CANCEL;

		if (member == null) {
			textFieldId.setText("- Autogenerated -");
			textFieldName.setText("");
			textFieldSurname.setText("");
			datePickerBirth.setDateToToday();
		} else {
			textFieldId.setText(member.getId().toString());
			textFieldName.setText(member.getName());
			textFieldSurname.setText(member.getSurname());
			datePickerBirth.setDate(member.getDateOfBirth());
		}

		setVisible(true);

		return result;
	}

	@Override
	public void setModalState(boolean modal) {
		setModal(modal);
	}

	@Override
	public void setMember(Member member) {
		this.member = member;
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

}
