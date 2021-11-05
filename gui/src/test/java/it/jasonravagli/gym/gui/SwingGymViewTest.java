package it.jasonravagli.gym.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.data.Index;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JLabelFixture;
import org.assertj.swing.fixture.JListFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

@RunWith(GUITestRunner.class)
public class SwingGymViewTest extends AssertJSwingJUnitTestCase {

	private AutoCloseable autocloseable;

	private FrameFixture window;

	private SwingGymView swingGymView;

	@Mock
	private DialogManageMember dialogManageMember;

	@Mock
	private DialogManageCourse dialogManageCourse;

	@Mock
	private DialogManageCourse dialogManageSubs;

	@Mock
	private GymController controller;

	@Override
	protected void onSetUp() throws Exception {
		autocloseable = MockitoAnnotations.openMocks(this);

		GuiActionRunner.execute(() -> {
			swingGymView = new SwingGymView(controller, dialogManageMember, dialogManageCourse, dialogManageSubs);
			return swingGymView;
		});

		window = new FrameFixture(robot(), swingGymView);
		window.show();
	}

	@Override
	public void onTearDown() throws Exception {
		autocloseable.close();
	}

	@Test
	@GUITest
	public void testControlsInitialState() {
		window.tabbedPane("tabbedPaneMain").requireTabTitles("Members", "Courses").requireSelectedTab(Index.atIndex(0));
		window.label("labelError").requireText(" ");

		window.list("listMembers");
		window.button("buttonAddMember").requireEnabled();
		window.button("buttonDeleteMember").requireDisabled();
		window.button("buttonUpdateMember").requireDisabled();
		window.button("buttonRefreshMembers").requireEnabled();

		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		window.list("listCourses");
		window.button(JButtonMatcher.withName("buttonAddCourse")).requireEnabled();
		window.button(JButtonMatcher.withName("buttonDeleteCourse")).requireDisabled();
		window.button(JButtonMatcher.withName("buttonUpdateCourse")).requireDisabled();
		window.button(JButtonMatcher.withName("buttonRefreshCourses")).requireEnabled();
		window.button(JButtonMatcher.withName("buttonManageSubs")).requireDisabled();
	}

	@Test
	@GUITest
	public void testButtonUpdateAndDeleteMemberShouldBeEnabledOnlyWhenAMemberIsSelected() {
		resetControllerMockInvocations();
		JButtonFixture buttonDelete = window.button("buttonDeleteMember");
		JButtonFixture buttonUpdate = window.button("buttonUpdateMember");
		JListFixture listMembers = window.list("listMembers");
		GuiActionRunner.execute(() -> swingGymView.getListModelMembers()
				.addElement(createTestMember("test-name", "test-surname", LocalDate.of(1996, 4, 30))));

		listMembers.selectItem(0);

		buttonDelete.requireEnabled();
		buttonUpdate.requireEnabled();

		listMembers.clearSelection();

		buttonDelete.requireDisabled();
		buttonUpdate.requireDisabled();
	}

	@Test
	@GUITest
	public void testButtonUpdateAndDeleteCourseAndManageSubsShouldBeEnabledOnlyWhenACourseIsSelected() {
		resetControllerMockInvocations();
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		JButtonFixture buttonDelete = window.button("buttonDeleteCourse");
		JButtonFixture buttonUpdate = window.button("buttonUpdateCourse");
		JButtonFixture buttonManageSubs = window.button("buttonManageSubs");
		JListFixture listCourses = window.list("listCourses");
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(createTestCourse("test-name")));

		listCourses.selectItem(0);

		buttonDelete.requireEnabled();
		buttonUpdate.requireEnabled();
		buttonManageSubs.requireEnabled();

		listCourses.clearSelection();

		buttonDelete.requireDisabled();
		buttonUpdate.requireDisabled();
		buttonManageSubs.requireDisabled();
	}

	@Test
	@GUITest
	public void testButtonAddMemberWhenClickedShouldChangeControllerViewAndShowModalDialogManageMember() {
		resetControllerMockInvocations();
		window.button("buttonAddMember").click();

		InOrder inOrder = Mockito.inOrder(dialogManageMember, controller);
		inOrder.verify(controller).setView(dialogManageMember);
		inOrder.verify(dialogManageMember).setModalState(true);
		inOrder.verify(dialogManageMember).showDialog();
	}

	@Test
	@GUITest
	public void testButtonAddMemberWhenDialogClosesAndResultIsOkShouldResetControllerViewAndReloadAllMembers() {
		resetControllerMockInvocations();
		when(dialogManageMember.showDialog()).thenReturn(DialogResult.OK);

		window.button("buttonAddMember").click();

		InOrder inOrder = Mockito.inOrder(dialogManageMember, controller);
		inOrder.verify(dialogManageMember).showDialog();
		inOrder.verify(dialogManageMember).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verify(controller).allMembers();
	}

	@Test
	@GUITest
	public void testButtonAddMemberWhenDialogClosesAndResultIsNotOkShouldResetControllerView() {
		resetControllerMockInvocations();
		when(dialogManageMember.showDialog()).thenReturn(DialogResult.CANCEL);

		window.button("buttonAddMember").click();

		InOrder inOrder = Mockito.inOrder(dialogManageMember, controller);
		inOrder.verify(dialogManageMember).showDialog();
		inOrder.verify(dialogManageMember).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	@GUITest
	public void testButtonUpdateMemberWhenClickedShouldChangeControllerViewAndSetupAndShowDialog() {
		resetControllerMockInvocations();
		JListFixture listMembers = window.list("listMembers");
		Member member = createTestMember("test-name", "test-surname", LocalDate.of(1996, 4, 30));
		GuiActionRunner.execute(() -> swingGymView.getListModelMembers().addElement(member));
		listMembers.selectItem(0);

		window.button("buttonUpdateMember").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageMember);
		inOrder.verify(controller).setView(dialogManageMember);
		inOrder.verify(dialogManageMember).setMember(member);
		inOrder.verify(dialogManageMember).setModalState(true);
		inOrder.verify(dialogManageMember).showDialog();
	}

	@Test
	@GUITest
	public void testButtonUpdateMemberWhenDialogClosesAndResultIsOkShouldResetControllerViewAndReloadAllMembers() {
		resetControllerMockInvocations();
		JListFixture listMembers = window.list("listMembers");
		GuiActionRunner.execute(() -> swingGymView.getListModelMembers()
				.addElement(createTestMember("test-name", "test-surname", LocalDate.of(1996, 4, 30))));
		listMembers.selectItem(0);
		when(dialogManageMember.showDialog()).thenReturn(DialogResult.OK);

		window.button("buttonUpdateMember").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageMember);
		inOrder.verify(dialogManageMember).showDialog();
		inOrder.verify(dialogManageMember).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verify(controller).allMembers();
	}

	@Test
	@GUITest
	public void testButtonUpdateMemberWhenDialogClosesAndResultIsNotOkShouldResetControllerView() {
		resetControllerMockInvocations();
		JListFixture listMembers = window.list("listMembers");
		GuiActionRunner.execute(() -> swingGymView.getListModelMembers()
				.addElement(createTestMember("test-name", "test-surname", LocalDate.of(1996, 4, 30))));
		listMembers.selectItem(0);
		when(dialogManageMember.showDialog()).thenReturn(DialogResult.CANCEL);

		window.button("buttonUpdateMember").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageMember);
		inOrder.verify(dialogManageMember).showDialog();
		inOrder.verify(dialogManageMember).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	@GUITest
	public void testButtonDeleteMemberWhenClickedShouldCallControllerMethod() {
		resetControllerMockInvocations();
		JListFixture listMembers = window.list("listMembers");
		Member member = createTestMember("test-name", "test-surname", LocalDate.of(1996, 4, 30));
		GuiActionRunner.execute(() -> swingGymView.getListModelMembers().addElement(member));
		listMembers.selectItem(0);

		window.button("buttonDeleteMember").click();

		verify(controller).deleteMember(member);
	}

	@Test
	@GUITest
	public void testButtonRefreshMembersWhenClickedShouldCallControllerMethod() {
		resetControllerMockInvocations();
		window.button("buttonRefreshMembers").click();

		verify(controller).allMembers();
	}

	@Test
	@GUITest
	public void testButtonAddCourseWhenClickedShouldChangeControllerViewAndShowModalDialogManageCourse() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();

		window.button("buttonAddCourse").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageCourse);
		inOrder.verify(controller).setView(dialogManageCourse);
		inOrder.verify(dialogManageCourse).setModalState(true);
		inOrder.verify(dialogManageCourse).showDialog();
	}

	@Test
	@GUITest
	public void testButtonAddCourseWhenDialogClosesAndResultIsOkShouldResetControllerViewAndReloadAllCourses() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		when(dialogManageCourse.showDialog()).thenReturn(DialogResult.OK);

		window.button("buttonAddCourse").click();

		InOrder inOrder = Mockito.inOrder(dialogManageCourse, controller);
		inOrder.verify(dialogManageCourse).showDialog();
		inOrder.verify(dialogManageCourse).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verify(controller).allCourses();
	}

	@Test
	@GUITest
	public void testButtonAddCourseWhenDialogClosesAndResultIsNotOkShouldResetControllerView() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		when(dialogManageCourse.showDialog()).thenReturn(DialogResult.CANCEL);

		window.button("buttonAddCourse").click();

		InOrder inOrder = Mockito.inOrder(dialogManageCourse, controller);
		inOrder.verify(dialogManageCourse).showDialog();
		inOrder.verify(dialogManageCourse).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	@GUITest
	public void testButtonUpdateCourseWhenClickedShouldChangeControllerViewAndSetupAndShowDialog() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		Course course = createTestCourse("test-name");
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(course));
		window.list("listCourses").selectItem(0);

		window.button("buttonUpdateCourse").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageCourse);
		inOrder.verify(controller).setView(dialogManageCourse);
		inOrder.verify(dialogManageCourse).setCourse(course);
		inOrder.verify(dialogManageCourse).setModalState(true);
		inOrder.verify(dialogManageCourse).showDialog();
	}

	@Test
	@GUITest
	public void testButtonUpdateCourseWhenDialogClosesAndResultIsOkShouldResetControllerViewAndReloadAllCourses() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(createTestCourse("test-name")));
		window.list("listCourses").selectItem(0);
		when(dialogManageCourse.showDialog()).thenReturn(DialogResult.OK);

		window.button("buttonUpdateCourse").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageCourse);
		inOrder.verify(dialogManageCourse).showDialog();
		inOrder.verify(dialogManageCourse).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verify(controller).allCourses();
	}

	@Test
	@GUITest
	public void testButtonUpdateCourseWhenDialogClosesAndResultIsNotOkShouldResetControllerView() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(createTestCourse("test-name")));
		window.list("listCourses").selectItem(0);
		when(dialogManageCourse.showDialog()).thenReturn(DialogResult.CANCEL);

		window.button("buttonUpdateCourse").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageCourse);
		inOrder.verify(dialogManageCourse).showDialog();
		inOrder.verify(dialogManageCourse).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	@GUITest
	public void testButtonDeleteCourseWhenClickedShouldCallControllerMethod() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		Course course = createTestCourse("test-name");
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(course));
		window.list("listCourses").selectItem(0);

		window.button("buttonDeleteCourse").click();

		verify(controller).deleteCourse(course);
	}

	@Test
	@GUITest
	public void testButtonRefreshCoursesWhenClickedShouldCallControllerMethod() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		window.button("buttonRefreshCourses").click();

		verify(controller).allCourses();
	}

	@Test
	@GUITest
	public void testButtonManageSubsWhenClickedShouldChangeControllerViewAndSetupAndShowDialog() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		Course course = createTestCourse("test-name");
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(course));
		window.list("listCourses").selectItem(0);

		window.button("buttonManageSubs").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageSubs);
		inOrder.verify(controller).setView(dialogManageSubs);
		inOrder.verify(dialogManageSubs).setCourse(course);
		inOrder.verify(dialogManageSubs).setModalState(true);
		inOrder.verify(dialogManageSubs).showDialog();
	}
	
	@Test
	@GUITest
	public void testButtonManageSubsWhenDialogClosesShouldReloadAllCourses() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		resetControllerMockInvocations();
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(createTestCourse("test-name")));
		window.list("listCourses").selectItem(0);

		window.button("buttonManageSubs").click();

		InOrder inOrder = Mockito.inOrder(controller, dialogManageSubs);
		inOrder.verify(dialogManageSubs).showDialog();
		inOrder.verify(dialogManageSubs).setModalState(false);
		inOrder.verify(controller).setView(swingGymView);
		inOrder.verify(controller).allCourses();
	}

	@Test
	public void testWhenCoursesTabIsSelectedShouldLoadCourses() {
		resetControllerMockInvocations();
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");

		verify(controller).allCourses();
	}

	@Test
	public void testWhenMembersTabIsSelectedShouldLoadMembers() {
		resetControllerMockInvocations();
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		window.tabbedPane("tabbedPaneMain").selectTab("Members");

		verify(controller).allMembers();
	}

	@Test
	public void testWhenViewIsShownShouldLoadMembers() {
		verify(controller).allMembers();
	}

	@Test
	public void testShowMembersShouldClearTheListAndAddMembers() {
		Member oldMember = createTestMember("name-old", "surname-old", LocalDate.of(1995, 4, 28));
		GuiActionRunner.execute(() -> swingGymView.getListModelMembers().addElement(oldMember));

		Member member1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		swingGymView.showMembers(Arrays.asList(member1, member2));

		assertThat(window.list("listMembers").contents()).containsExactly(member1.toString(), member2.toString());
	}

	@Test
	public void testShowMembersShouldClearTheErrorLabel() {
		JLabelFixture errorLabel = window.label("labelError");
		GuiActionRunner.execute(() -> errorLabel.target().setText("Some text"));

		swingGymView.showMembers(Collections.emptyList());

		errorLabel.requireText(" ");
	}

	@Test
	public void testMemberAddedShouldThrow() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> swingGymView.memberAdded(member)).isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("Operation not supported");
	}

	@Test
	public void testMemberUpdatedShouldThrow() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		assertThatThrownBy(() -> swingGymView.memberUpdated(member)).isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("Operation not supported");
	}

	@Test
	@GUITest
	public void testMemberDeletedShouldRemoveMemberFromTheList() {
		Member member1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		GuiActionRunner
				.execute(() -> Arrays.asList(member1, member2).forEach(swingGymView.getListModelMembers()::addElement));

		swingGymView.memberDeleted(member2);

		assertThat(window.list("listMembers").contents()).containsExactly(member1.toString());
	}

	@Test
	@GUITest
	public void testMemberDeletedShouldClearTheErrorLabel() {
		JLabelFixture errorLabel = window.label("labelError");
		GuiActionRunner.execute(() -> errorLabel.target().setText("Some text"));
		Member member1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		GuiActionRunner
				.execute(() -> Arrays.asList(member1, member2).forEach(swingGymView.getListModelMembers()::addElement));

		swingGymView.memberDeleted(member2);

		errorLabel.requireText(" ");
	}

	@Test
	@GUITest
	public void testShowCoursesShouldClearTheListAndAddCourses() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		Course oldCourse = createTestCourse("name-old");
		GuiActionRunner.execute(() -> swingGymView.getListModelCourses().addElement(oldCourse));

		Course course1 = createTestCourse("name-1");
		Course course2 = createTestCourse("name-2");
		swingGymView.showCourses(Arrays.asList(course1, course2));

		assertThat(window.list("listCourses").contents()).containsExactly(course1.toString(), course2.toString());
	}

	@Test
	@GUITest
	public void testShowCoursesShouldClearTheErrorLabel() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		JLabelFixture errorLabel = window.label("labelError");
		GuiActionRunner.execute(() -> errorLabel.target().setText("Some text"));

		swingGymView.showCourses(Collections.emptyList());

		errorLabel.requireText(" ");
	}

	@Test
	public void testCourseAddedShouldThrow() {
		Course course = createTestCourse("name-1");
		assertThatThrownBy(() -> swingGymView.courseAdded(course)).isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("Operation not supported");
	}

	@Test
	public void tesCourseUpdatedShouldThrow() {
		Course course = createTestCourse("name-1");
		assertThatThrownBy(() -> swingGymView.courseUpdated(course)).isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("Operation not supported");
	}

	@Test
	@GUITest
	public void testCourseDeletedShouldRemoveCourseFromTheList() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		Course course1 = createTestCourse("name-1");
		Course course2 = createTestCourse("name-2");
		GuiActionRunner
				.execute(() -> Arrays.asList(course1, course2).forEach(swingGymView.getListModelCourses()::addElement));

		swingGymView.courseDeleted(course2);

		assertThat(window.list("listCourses").contents()).containsExactly(course1.toString());
	}

	@Test
	@GUITest
	public void testCourseDeletedShouldClearTheErrorLabel() {
		window.tabbedPane("tabbedPaneMain").selectTab("Courses");
		JLabelFixture errorLabel = window.label("labelError");
		GuiActionRunner.execute(() -> errorLabel.target().setText("Some text"));
		Course course1 = createTestCourse("name-1");
		Course course2 = createTestCourse("name-2");
		GuiActionRunner
				.execute(() -> Arrays.asList(course1, course2).forEach(swingGymView.getListModelCourses()::addElement));

		swingGymView.courseDeleted(course2);

		errorLabel.requireText(" ");
	}

	@Test
	@GUITest
	public void testShowErrorShouldShowTheErrorInTheLabel() {
		String errorMessage = "An error occurred";
		swingGymView.showError(errorMessage);

		window.label("labelError").requireText(errorMessage);
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

	/*
	 * Useful to clear the controller mock from invocations done by automatic events
	 * like the window opening
	 */
	private void resetControllerMockInvocations() {
		Mockito.clearInvocations(controller);
	}

}
