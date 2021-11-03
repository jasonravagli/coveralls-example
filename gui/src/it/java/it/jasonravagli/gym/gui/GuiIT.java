package it.jasonravagli.gym.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.lgooddatepicker.components.DatePicker;

import it.jasonravagli.gym.logic.CourseRepository;
import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.logic.MemberRepository;
import it.jasonravagli.gym.logic.RepositoryProvider;
import it.jasonravagli.gym.logic.TransactionCode;
import it.jasonravagli.gym.logic.TransactionManager;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

@RunWith(GUITestRunner.class)
public class GuiIT extends AssertJSwingJUnitTestCase {
	
	@Rule
    public RetryOnUbuntuRule retry = new RetryOnUbuntuRule(5);

	private AutoCloseable autocloseable;

	private FrameFixture windowGymView;
	private DialogFixture windowManageMember;
	private DialogFixture windowManageCourse;
	private DialogFixture windowManageSubs;

	private SwingGymView swingGymView;
	private SwingDialogManageMember dialogManageMember;
	private SwingDialogManageCourse dialogManageCourse;
	private SwingDialogManageSubs dialogManageSubs;
	private GymController controller;

	@Mock
	private TransactionManager transactionManager;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private RepositoryProvider repositoryProvider;

	@Captor
	private ArgumentCaptor<Member> memberCaptor;

	private List<Member> listMembers;
	private List<Course> listCourses;

	@Override
	protected void onSetUp() throws Exception {
		autocloseable = MockitoAnnotations.openMocks(this);

		listMembers = new ArrayList<>();
		listCourses = new ArrayList<>();

		// Stub repositories methods to create fakes that use the list as database
		when(memberRepository.findAll()).thenReturn(listMembers);
		when(courseRepository.findAll()).thenReturn(listCourses);
		doAnswer(answer((Member member) -> listMembers.add(member))).when(memberRepository).save(any());
		doAnswer(answer((Course course) -> listCourses.add(course))).when(courseRepository).save(any());
		doAnswer(answer((Member member) -> {
			listMembers.removeIf(m -> member.getId().equals(m.getId()));
			listMembers.add(member);
			return null;
		})).when(memberRepository).update(any());
		doAnswer(answer((Course course) -> {
			listCourses.removeIf(c -> course.getId().equals(c.getId()));
			listCourses.add(course);
			return null;
		})).when(courseRepository).update(any());
		doAnswer(answer((UUID id) -> {
			return listMembers.stream().filter(m -> id.equals(m.getId())).findFirst().orElse(null);
		})).when(memberRepository).findById(any());
		doAnswer(answer((UUID id) -> {
			return listCourses.stream().filter(c -> id.equals(c.getId())).findFirst().orElse(null);
		})).when(courseRepository).findById(any());

		when(repositoryProvider.getMemberRepository()).thenReturn(memberRepository);
		when(repositoryProvider.getCourseRepository()).thenReturn(courseRepository);
		when(transactionManager.doInTransaction(any()))
				.thenAnswer(answer((TransactionCode<?> code) -> code.apply(repositoryProvider)));

		controller = new GymController();

		GuiActionRunner.execute(() -> {
			dialogManageMember = new SwingDialogManageMember(controller);
			dialogManageCourse = new SwingDialogManageCourse(controller);
			dialogManageSubs = new SwingDialogManageSubs(controller);

			swingGymView = new SwingGymView(controller, dialogManageMember, dialogManageCourse, dialogManageSubs);
			return swingGymView;
		});

		controller.setTransactionManager(transactionManager);
		controller.setView(swingGymView);

		windowManageMember = new DialogFixture(robot(), dialogManageMember);
		windowManageCourse = new DialogFixture(robot(), dialogManageCourse);
		windowManageSubs = new DialogFixture(robot(), dialogManageSubs);

		windowGymView = new FrameFixture(robot(), swingGymView);
		windowGymView.show();
	}

	@Override
	public void onTearDown() throws Exception {
		autocloseable.close();
	}

	@Test
	@GUITest
	public void testAllMembers() {
		Member member1 = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name 2", "surname 2", LocalDate.of(1996, 4, 30));
		listMembers.add(member1);
		listMembers.add(member2);

		controller.allMembers();

		assertThat(windowGymView.list("listMembers").contents()).containsExactly(member1.toString(),
				member2.toString());
	}

	@Test
	@GUITest
	public void testAllCourses() {
		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		Member member = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		Course course1 = createTestCourse("name 1", Stream.of(member).collect(Collectors.toSet()));
		Course course2 = createTestCourse("name 2", Collections.emptySet());
		listCourses.add(course1);
		listCourses.add(course2);

		controller.allCourses();

		assertThat(windowGymView.list("listCourses").contents()).containsExactly(course1.toString(),
				course2.toString());
	}

	@Test
	@GUITest
	public void testAddMemberWhenInsertIsOk() {
		String name = "name";
		String surname = "surname";
		LocalDate dateOfBirth = LocalDate.of(1996, 10, 31);

		windowGymView.button("buttonAddMember").click();
		windowManageMember.textBox("textFieldName").enterText(name);
		windowManageMember.textBox("textFieldSurname").enterText(surname);
		DatePicker datePicker = (DatePicker) windowManageMember.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(dateOfBirth));
		windowManageMember.button("buttonOk").click();

		windowManageMember.requireNotVisible();
		Member newMember = createTestMember(name, surname, dateOfBirth);
		assertThat(windowGymView.list("listMembers").contents()).containsExactly(newMember.toString());
	}

	@Test
	@GUITest
	public void testAddMemberWhenInsertRaiseError() {
		String name = "name";
		String surname = "surname";
		LocalDate dateOfBirth = LocalDate.of(1996, 10, 31);
		Member existingMember = createTestMember(name, surname, dateOfBirth);
		when(memberRepository.findById(any())).thenReturn(existingMember);

		windowGymView.button("buttonAddMember").click();
		windowManageMember.textBox("textFieldName").enterText(name);
		windowManageMember.textBox("textFieldSurname").enterText(surname);
		DatePicker datePicker = (DatePicker) windowManageMember.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(dateOfBirth));
		windowManageMember.button("buttonOk").click();

		windowManageMember.requireVisible();
		windowManageMember.label("labelError").requireText("A member with id .* already exists");
	}

	@Test
	@GUITest
	public void testUpdateMember() {
		Member member1 = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name 2", "surname 2", LocalDate.of(1996, 4, 30));
		listMembers.add(member1);
		listMembers.add(member2);
		String updatedName = "name updated";
		String updatedSurname = "surname updated";
		LocalDate updatedDateOfBirth = LocalDate.of(1995, 4, 28);

		controller.allMembers();
		windowGymView.list("listMembers").selectItem(member2.toString());
		windowGymView.button("buttonUpdateMember").click();
		windowManageMember.textBox("textFieldName").deleteText().enterText(updatedName);
		windowManageMember.textBox("textFieldSurname").deleteText().enterText(updatedSurname);
		DatePicker datePicker = (DatePicker) windowManageMember.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(updatedDateOfBirth));
		windowManageMember.button("buttonOk").click();

		windowManageMember.requireNotVisible();
		Member updatedMember = createTestMember(updatedName, updatedSurname, updatedDateOfBirth);
		assertThat(windowGymView.list("listMembers").contents()).containsOnly(member1.toString(),
				updatedMember.toString());
	}

	@Test
	@GUITest
	public void testDeleteMember() {
		Member member1 = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name 2", "surname 2", LocalDate.of(1996, 4, 30));
		listMembers.add(member1);
		listMembers.add(member2);

		controller.allMembers();
		windowGymView.list("listMembers").selectItem(member2.toString());
		windowGymView.button("buttonDeleteMember").click();

		assertThat(windowGymView.list("listMembers").contents()).containsOnly(member1.toString());
	}

	@Test
	@GUITest
	public void testAddCourseWhenInsertIsOk() {
		String name = "course";

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.button("buttonAddCourse").click();
		windowManageCourse.textBox("textFieldName").enterText(name);
		windowManageCourse.button("buttonOk").click();

		windowManageCourse.requireNotVisible();
		Course newCourse = createTestCourse(name, Collections.emptySet());
		assertThat(windowGymView.list("listCourses").contents()).containsExactly(newCourse.toString());
	}

	@Test
	@GUITest
	public void testAddCourseWhenInsertRaiseError() {
		String name = "course";
		Course existingCourse = createTestCourse(name, Collections.emptySet());
		when(courseRepository.findById(any())).thenReturn(existingCourse);

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.button("buttonAddCourse").click();
		windowManageCourse.textBox("textFieldName").enterText(name);
		windowManageCourse.button("buttonOk").click();

		windowManageCourse.requireVisible();
		windowManageCourse.label("labelError").requireText("A course with id .* already exists");
	}

	@Test
	@GUITest
	public void testUpdateCourse() {
		Course course1 = createTestCourse("course 1", Collections.emptySet());
		Course course2 = createTestCourse("course 2", Collections.emptySet());
		listCourses.add(course1);
		listCourses.add(course2);
		String updatedName = "course updated";

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(course2.toString());
		windowGymView.button("buttonUpdateCourse").click();
		windowManageCourse.textBox("textFieldName").deleteText().enterText(updatedName);
		windowManageCourse.button("buttonOk").click();

		windowManageCourse.requireNotVisible();
		Course updatedCourse = createTestCourse(updatedName, Collections.emptySet());
		assertThat(windowGymView.list("listCourses").contents()).containsOnly(course1.toString(),
				updatedCourse.toString());
	}

	@Test
	@GUITest
	public void testDeleteCourse() {
		Course course1 = createTestCourse("course 1", Collections.emptySet());
		Course course2 = createTestCourse("course 2", Collections.emptySet());
		listCourses.add(course1);
		listCourses.add(course2);

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(course2.toString());
		windowGymView.button("buttonDeleteCourse").click();

		assertThat(windowGymView.list("listCourses").contents()).containsOnly(course1.toString());
	}

	@Test
	@GUITest
	public void testManageSubs() {
		Member member1 = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name 2", "surname 2", LocalDate.of(1996, 4, 30));
		listMembers.add(member1);
		listMembers.add(member2);
		Course course = createTestCourse("course", new HashSet<>());
		course.getSubscribers().add(member1);
		listCourses.add(course);

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(course.toString());
		windowGymView.button("buttonManageSubs").click();
		windowManageSubs.list("listSubs").selectItem(member1.toString());
		windowManageSubs.button("buttonRemoveSub").click();
		windowManageSubs.list("listOtherMembers").selectItem(member2.toString());
		windowManageSubs.button("buttonAddSub").click();
		windowManageSubs.button("buttonOk").click();

		windowManageSubs.requireNotVisible();
		assertThat(course.getSubscribers()).containsExactly(member2);
		assertThat(windowGymView.list("listCourses").contents()).containsExactly(course.toString());
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
