package it.jasonravagli.gym.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.lgooddatepicker.components.DatePicker;

import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

@RunWith(GUITestRunner.class)
public class SwingDialogManageMemberTest extends AssertJSwingJUnitTestCase {

	AutoCloseable autoCloseable;

	private FrameFixture dialogFixture;

	@Mock
	private GymController controller;

	private SwingDialogManageMember dialogManageMember;

	@Override
	protected void onSetUp() throws Exception {
		autoCloseable = MockitoAnnotations.openMocks(this);

		GuiActionRunner.execute(() -> {
			dialogManageMember = new SwingDialogManageMember(controller);
			return dialogManageMember;
		});

		dialogFixture = new FrameFixture(robot(), dialogManageMember);
		dialogFixture.show();
	}

	@Override
	public void onTearDown() throws Exception {
		autoCloseable.close();
	}

	@Test
	@GUITest
	public void testControlsInitialState() {
		dialogFixture.label(JLabelMatcher.withText("Id:"));
		dialogFixture.label(JLabelMatcher.withText("Name:"));
		dialogFixture.label(JLabelMatcher.withText("Surname:"));
		dialogFixture.label(JLabelMatcher.withText("Date of birth:"));
		dialogFixture.label("labelError").requireText(" ");
		dialogFixture.textBox("textFieldId").requireDisabled();
		dialogFixture.textBox("textFieldName").requireEnabled();
		dialogFixture.textBox("textFieldSurname").requireEnabled();
		dialogFixture.panel("datePickerBirth");
		DatePicker datePicker = (DatePicker) dialogFixture.panel("datePickerBirth").target();
		assertThat(datePicker.getSettings().getAllowKeyboardEditing()).isFalse();
		assertThat(datePicker.getSettings().getAllowEmptyDates()).isFalse();
		assertThat(datePicker.getDate()).isEqualTo(LocalDate.now());
		dialogFixture.button("buttonCancel");
		dialogFixture.button("buttonOk").requireDisabled();
	}

	@Test
	@GUITest
	public void testOkButtonShouldBeEnabledOnlyWhenAllFieldsAreNotBlank() {
		dialogFixture.textBox("textFieldName").setText("name");
		dialogFixture.textBox("textFieldSurname").setText("surname");

		dialogFixture.button("buttonOk").requireEnabled();

		dialogFixture.textBox("textFieldName").setText(" ");
		dialogFixture.textBox("textFieldSurname").setText("surname");

		dialogFixture.button("buttonOk").requireDisabled();

		dialogFixture.textBox("textFieldName").setText("name");
		dialogFixture.textBox("textFieldSurname").setText(" ");

		dialogFixture.button("buttonOk").requireDisabled();
	}

	@Test
	@GUITest
	public void testOkButtonWhenClickedShouldClearLabelError() {
		dialogFixture.textBox("textFieldName").setText("name");
		dialogFixture.textBox("textFieldSurname").setText("surname");
		GuiActionRunner.execute(() -> dialogFixture.label("labelError").target().setText("Some errors"));

		dialogFixture.button("buttonOk").click();

		dialogFixture.label("labelError").requireText(" ");
	}

	@Test
	@GUITest
	public void testOkButtonWhenClickedAndMemberIsNotSetShouldCallAddMember() {
		String nameWithLeadingSpaces = " test-name";
		String surnameWithTrailingSpaces = "test-surname ";
		LocalDate dateOfBirth = LocalDate.of(1996, 10, 31);
		dialogFixture.textBox("textFieldName").setText(nameWithLeadingSpaces);
		dialogFixture.textBox("textFieldSurname").setText(surnameWithTrailingSpaces);
		DatePicker datePicker = (DatePicker) dialogFixture.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(dateOfBirth));
		GuiActionRunner.execute(() -> dialogFixture.label("labelError").target().setText("Some errors"));

		dialogFixture.button("buttonOk").click();

		Member expectedMember = new Member();
		expectedMember.setId(null);
		expectedMember.setName(nameWithLeadingSpaces.trim());
		expectedMember.setSurname(surnameWithTrailingSpaces.trim());
		expectedMember.setDateOfBirth(dateOfBirth);
		verify(controller).addMember(expectedMember);
	}

	@Test
	@GUITest
	public void testOkButtonWhenClickedAndMemberIsSetShouldCallUpdateMember() {
		String nameWithLeadingSpaces = " test-name";
		String surnameWithTrailingSpaces = "test-surname ";
		LocalDate dateOfBirth = LocalDate.of(1996, 10, 31);
		dialogFixture.textBox("textFieldName").setText(nameWithLeadingSpaces);
		dialogFixture.textBox("textFieldSurname").setText(surnameWithTrailingSpaces);
		DatePicker datePicker = (DatePicker) dialogFixture.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(dateOfBirth));
		GuiActionRunner.execute(() -> dialogFixture.label("labelError").target().setText("Some errors"));
		Member memberToUpdate = createTestMember("name", "surname", LocalDate.of(1996, 4, 30));
		dialogManageMember.setMember(memberToUpdate);

		dialogFixture.button("buttonOk").click();

		Member expectedMember = new Member();
		expectedMember.setId(memberToUpdate.getId());
		expectedMember.setName(nameWithLeadingSpaces.trim());
		expectedMember.setSurname(surnameWithTrailingSpaces.trim());
		expectedMember.setDateOfBirth(dateOfBirth);
		verify(controller).updateMember(expectedMember);
	}

	@Test
	@GUITest
	public void testCancelButtonWhenClickedShouldSetDialogResultAndClose() {
		dialogManageMember.setResult(DialogResult.OK);

		dialogFixture.button("buttonCancel").click();

		assertThat(dialogManageMember.getResult()).isEqualTo(DialogResult.CANCEL);
		dialogFixture.requireNotVisible();
	}

	@Test
	@GUITest
	public void testShowDialogShouldResetDialogResultToCancel() {
		dialogManageMember.setResult(DialogResult.OK);
		dialogFixture.close();

		dialogManageMember.showDialog();

		assertThat(dialogManageMember.getResult()).isEqualTo(DialogResult.CANCEL);
	}

	@Test
	@GUITest
	public void testShowDialogShouldShowDialogAndReturnCurrentResult() {
		dialogFixture.close();

		DialogResult retrievedResult = dialogManageMember.showDialog();

		dialogFixture.requireVisible();
		assertThat(retrievedResult).isEqualTo(dialogManageMember.getResult());
	}

	@Test
	@GUITest
	public void testShowDialogWhenMemberIsNotSetShouldClearTheFields() {
		dialogManageMember.setMember(null);
		GuiActionRunner.execute(() -> dialogFixture.textBox("textFieldId").target().setText("id"));
		dialogFixture.textBox("textFieldName").setText("name");
		dialogFixture.textBox("textFieldSurname").setText("surname");
		DatePicker datePickerBirth = (DatePicker) dialogFixture.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePickerBirth.setDate(LocalDate.of(1996, 10, 31)));
		dialogFixture.close();

		dialogManageMember.showDialog();

		dialogFixture.textBox("textFieldId").requireDisabled().requireText("- Autogenerated -");
		dialogFixture.textBox("textFieldName").requireText("");
		dialogFixture.textBox("textFieldSurname").requireText("");
		assertThat(datePickerBirth.getDate()).isEqualTo(LocalDate.now());
	}

	@Test
	@GUITest
	public void testShowDialogWhenMemberIsSetShouldFillTheFields() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		dialogManageMember.setMember(member);
		dialogFixture.close();

		dialogManageMember.showDialog();

		dialogFixture.textBox("textFieldId").requireDisabled().requireText(member.getId().toString());
		dialogFixture.textBox("textFieldName").requireText(member.getName());
		dialogFixture.textBox("textFieldSurname").requireText(member.getSurname());
		assertThat(((DatePicker) dialogFixture.panel("datePickerBirth").target()).getDate())
				.isEqualTo(member.getDateOfBirth());
	}

	@Test
	public void testShowCoursesShouldThrow() {
		assertThatThrownBy(() -> dialogManageMember.showCourses(Collections.emptyList()))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage("Operation not supported");
	}

	@Test
	public void testShowMembersShouldThrow() {
		assertThatThrownBy(() -> dialogManageMember.showMembers(Collections.emptyList()))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage("Operation not supported");
	}

	@Test
	public void testMemberDeletedShouldThrow() {
		assertThatThrownBy(
				() -> dialogManageMember.memberDeleted(createTestMember("name", "surname", LocalDate.of(1996, 10, 31))))
						.isInstanceOf(UnsupportedOperationException.class).hasMessage("Operation not supported");
	}

	@Test
	public void testCourseAddedShouldThrow() {
		assertThatThrownBy(() -> dialogManageMember.courseAdded(createTestCourse("name")))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage("Operation not supported");
	}

	@Test
	public void testCourseUpdatedShouldThrow() {
		assertThatThrownBy(() -> dialogManageMember.courseUpdated(createTestCourse("name")))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage("Operation not supported");
	}

	@Test
	public void testCourseDeletedShouldThrow() {
		assertThatThrownBy(() -> dialogManageMember.courseDeleted(createTestCourse("name")))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage("Operation not supported");
	}

	@Test
	public void testMemberAddedShouldSetTheResultAndClose() {
		dialogManageMember.memberAdded(createTestMember("name", "surname", LocalDate.of(1996, 10, 31)));

		assertThat(dialogManageMember.getResult()).isEqualTo(DialogResult.OK);
		dialogFixture.requireNotVisible();
	}

	@Test
	public void testMemberUpdatedShouldSetTheResultAndClose() {
		dialogManageMember.memberUpdated(createTestMember("name", "surname", LocalDate.of(1996, 10, 31)));

		assertThat(dialogManageMember.getResult()).isEqualTo(DialogResult.OK);
		dialogFixture.requireNotVisible();
	}
	
	@Test
	public void testShowErrorShouldShowMessageInLabelError() {
		String errorMessage = "An error occurred";
		
		dialogManageMember.showError(errorMessage);
		
		dialogFixture.label("labelError").requireText(errorMessage);
	}

	private Member createTestMember(String name, String surname, LocalDate dateOfBirth) {
		Member member = new Member();
		member.setId(UUID.randomUUID());
		member.setName(name);
		member.setSurname(surname);
		member.setDateOfBirth(dateOfBirth);

		return member;
	}

	private Course createTestCourse(String name) {
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setName(name);
		course.setSubscribers(Collections.emptySet());

		return course;
	}

}
