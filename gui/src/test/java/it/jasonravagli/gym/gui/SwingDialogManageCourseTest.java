package it.jasonravagli.gym.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

@RunWith(GUITestRunner.class)
public class SwingDialogManageCourseTest extends AssertJSwingJUnitTestCase {

	private static final String UNSUPPORTED_OP_MESSAGE = "Operation not supported";

	AutoCloseable autoCloseable;

	private DialogFixture dialogFixture;

	@Mock
	private GymController controller;

	@Captor
	private ArgumentCaptor<Course> courseCaptor;

	private SwingDialogManageCourse dialogManageCourse;

	@Override
	protected void onSetUp() throws Exception {
		autoCloseable = MockitoAnnotations.openMocks(this);

		GuiActionRunner.execute(() -> {
			dialogManageCourse = new SwingDialogManageCourse(controller);
			return dialogManageCourse;
		});

		dialogFixture = new DialogFixture(robot(), dialogManageCourse);
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
		dialogFixture.label("labelError").requireText(" ");
		dialogFixture.textBox("textFieldId").requireDisabled();
		dialogFixture.textBox("textFieldName").requireEnabled();
		dialogFixture.button("buttonCancel");
		dialogFixture.button("buttonOk").requireDisabled();
	}

	@Test
	@GUITest
	public void testOkButtonShouldBeEnabledOnlyWhenFieldNameIsNotBlank() {
		dialogFixture.textBox("textFieldName").setText("name");

		dialogFixture.button("buttonOk").requireEnabled();

		dialogFixture.textBox("textFieldName").setText(" ");

		dialogFixture.button("buttonOk").requireDisabled();
	}

	@Test
	@GUITest
	public void testOkButtonWhenClickedShouldClearLabelError() {
		dialogFixture.textBox("textFieldName").setText("name");
		GuiActionRunner.execute(() -> dialogFixture.label("labelError").target().setText("Some errors"));

		dialogFixture.button("buttonOk").click();

		dialogFixture.label("labelError").requireText(" ");
	}

	@Test
	@GUITest
	public void testOkButtonWhenClickedAndCourseIsNotSetShouldCallAddCourse() {
		String nameWithLeadingSpaces = " test-name";
		dialogFixture.textBox("textFieldName").setText(nameWithLeadingSpaces);

		dialogFixture.button("buttonOk").click();

		verify(controller).addCourse(courseCaptor.capture());
		Course usedCourse = courseCaptor.getValue();
		assertThat(usedCourse.getId()).isNotNull();
		assertThat(usedCourse.getName()).isEqualTo(nameWithLeadingSpaces.trim());
	}

	@Test
	@GUITest
	public void testOkButtonWhenClickedAndMemberIsSetShouldCallUpdateCourse() {
		String nameWithTrailingSpaces = "test-name ";
		dialogFixture.textBox("textFieldName").setText(nameWithTrailingSpaces);
		Course courseToUpdate = createTestCourse("name");
		dialogManageCourse.setCourse(courseToUpdate);

		dialogFixture.button("buttonOk").click();

		Course expectedCourse = new Course();
		expectedCourse.setId(courseToUpdate.getId());
		expectedCourse.setName(nameWithTrailingSpaces.trim());
		verify(controller).updateCourse(expectedCourse);
	}

	@Test
	@GUITest
	public void testCancelButtonWhenClickedShouldSetDialogResultAndClose() {
		dialogManageCourse.setResult(DialogResult.OK);

		dialogFixture.button("buttonCancel").click();

		assertThat(dialogManageCourse.getResult()).isEqualTo(DialogResult.CANCEL);
		dialogFixture.requireNotVisible();
	}

	@Test
	@GUITest
	public void testShowDialogShouldResetDialogResultToCancel() {
		dialogManageCourse.setResult(DialogResult.OK);
		dialogFixture.close();

		GuiActionRunner.execute(() -> dialogManageCourse.showDialog());

		assertThat(dialogManageCourse.getResult()).isEqualTo(DialogResult.CANCEL);
	}

	@Test
	@GUITest
	public void testShowDialogShouldShowDialogAndReturnCurrentResult() {
		dialogFixture.close();

		DialogResult retrievedResult = GuiActionRunner.execute(() -> {
			return dialogManageCourse.showDialog();
		});

		dialogFixture.requireVisible();
		assertThat(retrievedResult).isEqualTo(dialogManageCourse.getResult());
	}

	@Test
	@GUITest
	public void testShowDialogWhenCourseIsNotSetShouldClearTheFields() {
		dialogManageCourse.setCourse(null);
		GuiActionRunner.execute(() -> dialogFixture.textBox("textFieldId").target().setText("id"));
		dialogFixture.textBox("textFieldName").setText("name");
		dialogFixture.close();

		GuiActionRunner.execute(() -> dialogManageCourse.showDialog());

		dialogFixture.textBox("textFieldId").requireDisabled().requireText("- Autogenerated -");
		dialogFixture.textBox("textFieldName").requireText("");
	}

	@Test
	@GUITest
	public void testShowDialogWhenCourseIsSetShouldFillTheFields() {
		Course course = createTestCourse("name-1");
		dialogManageCourse.setCourse(course);
		dialogFixture.close();

		GuiActionRunner.execute(() -> dialogManageCourse.showDialog());

		dialogFixture.textBox("textFieldId").requireDisabled().requireText(course.getId().toString());
		dialogFixture.textBox("textFieldName").requireText(course.getName());
	}

	@Test
	public void testShowCoursesShouldThrow() {
		List<Course> courses = Collections.emptyList();
		assertThatThrownBy(() -> dialogManageCourse.showCourses(courses))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testShowMembersShouldThrow() {
		List<Member> members = Collections.emptyList();
		assertThatThrownBy(() -> dialogManageCourse.showMembers(members))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testMemberAddedShouldThrow() {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> dialogManageCourse.memberAdded(member))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testMemberUpdatedShouldThrow() {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> dialogManageCourse.memberUpdated(member))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testMemberDeletedShouldThrow() {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> dialogManageCourse.memberDeleted(member))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testCourseDeletedShouldThrow() {
		Course course = createTestCourse("name");
		assertThatThrownBy(() -> dialogManageCourse.courseDeleted(course))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testCourseAddedShouldSetTheResultAndClose() {
		dialogManageCourse.courseAdded(createTestCourse("name"));

		assertThat(dialogManageCourse.getResult()).isEqualTo(DialogResult.OK);
		dialogFixture.requireNotVisible();
	}

	@Test
	public void testMemberUpdatedShouldSetTheResultAndClose() {
		dialogManageCourse.courseUpdated(createTestCourse("name"));

		assertThat(dialogManageCourse.getResult()).isEqualTo(DialogResult.OK);
		dialogFixture.requireNotVisible();
	}

	@Test
	public void testShowErrorShouldShowMessageInLabelError() {
		String errorMessage = "An error occurred";

		dialogManageCourse.showError(errorMessage);

		dialogFixture.label("labelError").requireText(errorMessage);
	}

	@Test
	public void testSetModalState() {
		boolean modalState = true;
		dialogManageCourse.setModalState(modalState);

		assertThat(dialogManageCourse.isModal()).isEqualTo(modalState);

		modalState = false;
		dialogManageCourse.setModalState(modalState);

		assertThat(dialogManageCourse.isModal()).isEqualTo(modalState);
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
