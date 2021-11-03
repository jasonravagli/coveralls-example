package it.jasonravagli.gym.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

@RunWith(GUITestRunner.class)
public class SwingDialogManageSubsTest extends AssertJSwingJUnitTestCase {

	private static final String UNSUPPORTED_OP_MESSAGE = "Operation not supported";

	AutoCloseable autoCloseable;

	private DialogFixture dialogFixture;

	@Mock
	private GymController controller;

	private SwingDialogManageSubs dialogManageSubs;

	@Override
	protected void onSetUp() throws Exception {
		autoCloseable = MockitoAnnotations.openMocks(this);

		GuiActionRunner.execute(() -> {
			dialogManageSubs = new SwingDialogManageSubs(controller);
			dialogManageSubs.setCourse(createTestCourse("name", Collections.emptySet()));
			return dialogManageSubs;
		});

		dialogFixture = new DialogFixture(robot(), dialogManageSubs);
		dialogFixture.show();
	}

	@Override
	public void onTearDown() throws Exception {
		autoCloseable.close();
	}

	@Test
	@GUITest
	public void testControlsInitialState() {
		dialogFixture.label(JLabelMatcher.withText("Subscribers"));
		dialogFixture.label(JLabelMatcher.withText("All"));
		dialogFixture.label("labelError").requireText(" ");
		dialogFixture.list("listSubs").requireEnabled();
		dialogFixture.list("listOtherMembers").requireEnabled();
		dialogFixture.button("buttonAddSub").requireDisabled();
		dialogFixture.button("buttonRemoveSub").requireDisabled();
		dialogFixture.button("buttonCancel").requireEnabled();
		dialogFixture.button("buttonOk").requireEnabled();
	}

	@Test
	@GUITest
	public void testButtonAddSubShouldBeEnabledOnlyWhenAMemberIsSelected() {
		GuiActionRunner.execute(() -> dialogManageSubs.getListModelOtherMembers()
				.addElement(createTestMember("name", "surname", LocalDate.of(1996, 10, 31))));
		dialogFixture.list("listOtherMembers").selectItem(0);

		dialogFixture.button("buttonAddSub").requireEnabled();

		dialogFixture.list("listOtherMembers").clearSelection();

		dialogFixture.button("buttonAddSub").requireDisabled();
	}

	@Test
	@GUITest
	public void testButtonAddSubWhenClickedShouldClearLabelErrorAndAddMemberToCourse() {
		GuiActionRunner.execute(() -> dialogFixture.label("labelError").target().setText("Some errors"));
		Member subscriber = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course = createTestCourse("name", Stream.of(subscriber).collect(Collectors.toSet()));
		dialogManageSubs.setCourse(course);
		Member otherMember = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		GuiActionRunner.execute(() -> dialogManageSubs.getListModelOtherMembers().addElement(otherMember));
		dialogFixture.list("listOtherMembers").selectItem(0);

		dialogFixture.button("buttonAddSub").click();

		dialogFixture.label("labelError").requireText(" ");
		assertThat(dialogManageSubs.getCourse().getSubscribers()).containsOnly(subscriber, otherMember);
	}

	@Test
	@GUITest
	public void testButtonAddSubWhenClickedShouldUpdateTheLists() {
		Member subscriber = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course = createTestCourse("name", Stream.of(subscriber).collect(Collectors.toSet()));
		dialogManageSubs.setCourse(course);
		dialogManageSubs.getListModelSubs().clear();
		GuiActionRunner.execute(() -> dialogManageSubs.getListModelSubs().addElement(subscriber));
		Member otherMember = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		GuiActionRunner.execute(() -> dialogManageSubs.getListModelOtherMembers().addElement(otherMember));
		dialogFixture.list("listOtherMembers").selectItem(0);

		dialogFixture.button("buttonAddSub").click();

		assertThat(dialogFixture.list("listOtherMembers").contents()).isEmpty();
		assertThat(dialogFixture.list("listSubs").contents()).containsOnly(subscriber.toString(),
				otherMember.toString());
	}

	@Test
	@GUITest
	public void testButtonRemoveSubShouldBeEnabledOnlyWhenASubIsSelected() {
		GuiActionRunner.execute(() -> dialogManageSubs.getListModelSubs()
				.addElement(createTestMember("name", "surname", LocalDate.of(1996, 10, 31))));
		dialogFixture.list("listSubs").selectItem(0);

		dialogFixture.button("buttonRemoveSub").requireEnabled();

		dialogFixture.list("listSubs").clearSelection();

		dialogFixture.button("buttonRemoveSub").requireDisabled();
	}

	@Test
	@GUITest
	public void testButtonRemoveSubWhenClickedShouldClearLabelErrorAndRemoveMemberFromCourse() {
		GuiActionRunner.execute(() -> dialogFixture.label("labelError").target().setText("Some errors"));
		Member subscriber1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member subscriber2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		Course course = createTestCourse("name", Stream.of(subscriber1, subscriber2).collect(Collectors.toSet()));
		dialogManageSubs.setCourse(course);
		GuiActionRunner.execute(() -> {
			dialogManageSubs.getListModelSubs().addElement(subscriber1);
			dialogManageSubs.getListModelSubs().addElement(subscriber2);
		});
		dialogFixture.list("listSubs").selectItem(1);

		dialogFixture.button("buttonRemoveSub").click();

		dialogFixture.label("labelError").requireText(" ");
		assertThat(dialogManageSubs.getCourse().getSubscribers()).containsOnly(subscriber1);
	}

	@Test
	@GUITest
	public void testButtonRemoveSubWhenClickedShouldUpdateTheLists() {
		Member subscriber1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member subscriber2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		Course course = createTestCourse("name", Stream.of(subscriber1, subscriber2).collect(Collectors.toSet()));
		dialogManageSubs.setCourse(course);
		GuiActionRunner.execute(() -> {
			dialogManageSubs.getListModelSubs().addElement(subscriber1);
			dialogManageSubs.getListModelSubs().addElement(subscriber2);
		});
		dialogFixture.list("listSubs").selectItem(1);

		dialogFixture.button("buttonRemoveSub").click();

		assertThat(dialogFixture.list("listSubs").contents()).containsOnly(subscriber1.toString());
		assertThat(dialogFixture.list("listOtherMembers").contents()).containsOnly(subscriber2.toString());
	}

	@Test
	@GUITest
	public void testCancelButtonWhenClickedShouldSetDialogResultAndClose() {
		dialogManageSubs.setResult(DialogResult.OK);

		dialogFixture.button("buttonCancel").click();

		assertThat(dialogManageSubs.getResult()).isEqualTo(DialogResult.CANCEL);
		dialogFixture.requireNotVisible();
	}

	@Test
	@GUITest
	public void testOkButtonWhenClickedShouldClearLabelErrorAndCallUpdateCourse() {
		GuiActionRunner.execute(() -> dialogFixture.label("labelError").target().setText("Some errors"));
		dialogManageSubs.setCourse(createTestCourse("name", Collections.emptySet()));

		dialogFixture.button("buttonOk").click();

		dialogFixture.label("labelError").requireText(" ");
		verify(controller).updateCourse(dialogManageSubs.getCourse());
	}

	@Test
	@GUITest
	public void testShowDialogShouldResetDialogResultToCancel() {
		dialogManageSubs.setResult(DialogResult.OK);
		dialogFixture.close();

		dialogManageSubs.showDialog();

		assertThat(dialogManageSubs.getResult()).isEqualTo(DialogResult.CANCEL);
	}

	@Test
	@GUITest
	public void testShowDialogShouldShowDialogAndReturnCurrentResult() {
		dialogFixture.close();

		DialogResult retrievedResult = dialogManageSubs.showDialog();

		dialogFixture.requireVisible();
		assertThat(retrievedResult).isEqualTo(dialogManageSubs.getResult());
	}

	@Test
	@GUITest
	public void testShowDialogWhenCourseIsNotSetShouldThrow() {
		dialogManageSubs.setCourse(null);
		dialogFixture.close();

		assertThatThrownBy(() -> dialogManageSubs.showDialog()).isInstanceOf(InvalidCourseException.class)
				.hasMessage("Course cannot be null");
	}

	@Test
	@GUITest
	public void testShowDialogShouldShowCourseSubsInListAndLoadAllMembers() {
		Member subscriber1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member subscriber2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		Course course = createTestCourse("name", Stream.of(subscriber1, subscriber2).collect(Collectors.toSet()));
		dialogManageSubs.setCourse(course);
		dialogFixture.close();

		GuiActionRunner.execute(() -> dialogManageSubs.showDialog());

		assertThat(dialogFixture.list("listSubs").contents()).containsOnly(subscriber1.toString(),
				subscriber2.toString());
		verify(controller).allMembers();
	}

	@Test
	public void testShowCoursesShouldThrow() {
		List<Course> courses = Collections.emptyList();
		assertThatThrownBy(() -> dialogManageSubs.showCourses(courses))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testMemberAddedShouldThrow() {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> dialogManageSubs.memberAdded(member)).isInstanceOf(UnsupportedOperationException.class)
				.hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testMemberUpdatedShouldThrow() {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> dialogManageSubs.memberUpdated(member))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testMemberDeletedShouldThrow() {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> dialogManageSubs.memberDeleted(member))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testCourseAddedShouldThrow() {
		Course course = createTestCourse("name", Collections.emptySet());
		assertThatThrownBy(() -> dialogManageSubs.courseAdded(course)).isInstanceOf(UnsupportedOperationException.class)
				.hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testCourseDeletedShouldThrow() {
		Course course = createTestCourse("name", Collections.emptySet());
		assertThatThrownBy(() -> dialogManageSubs.courseDeleted(course))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage(UNSUPPORTED_OP_MESSAGE);
	}

	@Test
	public void testCourseUpdatedShouldSetCurrentResultAndClose() {
		dialogManageSubs.courseUpdated(createTestCourse("name", Collections.emptySet()));

		assertThat(dialogManageSubs.getResult()).isEqualTo(DialogResult.OK);
		dialogFixture.requireNotVisible();
	}

	@Test
	@GUITest
	public void testShowMembersShouldShowAllMembersThatAreNotSubsInTheList() throws InterruptedException {
		Member otherMember1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member otherMember2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		Member subscriber = createTestMember("name-3", "surname-3", LocalDate.of(1995, 4, 28));
		dialogManageSubs.getCourse().setSubscribers(Stream.of(subscriber).collect(Collectors.toSet()));
		List<Member> allMembers = Stream.of(otherMember1, otherMember2, subscriber).collect(Collectors.toList());

		dialogManageSubs.showMembers(allMembers);

		assertThat(dialogFixture.list("listSubs").contents()).containsExactly(subscriber.toString());
		assertThat(dialogFixture.list("listOtherMembers").contents()).containsExactly(otherMember1.toString(),
				otherMember2.toString());
	}

	@Test
	public void testShowErrorShouldShowMessageInLabelError() {
		String errorMessage = "An error occurred";

		dialogManageSubs.showError(errorMessage);

		dialogFixture.label("labelError").requireText(errorMessage);
	}

	@Test
	public void testSetModalState() {
		boolean modalState = true;
		dialogManageSubs.setModalState(modalState);

		assertThat(dialogManageSubs.isModal()).isEqualTo(modalState);
		
		modalState = false;
		dialogManageSubs.setModalState(modalState);

		assertThat(dialogManageSubs.isModal()).isEqualTo(modalState);
	}

	private Member createTestMember(String name, String surname, LocalDate dateOfBirth) {
		Member member = new Member();
		member.setId(UUID.randomUUID());
		member.setName(name);
		member.setSurname(surname);
		member.setDateOfBirth(dateOfBirth);

		return member;
	}

	private Course createTestCourse(String name, Set<Member> subscribers) {
		Course course = new Course();
		course.setId(UUID.randomUUID());
		course.setName(name);
		course.setSubscribers(subscribers);

		return course;
	}

}
