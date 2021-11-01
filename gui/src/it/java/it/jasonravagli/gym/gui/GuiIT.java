package it.jasonravagli.gym.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import org.junit.Test;
import org.junit.runner.RunWith;
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

	private AutoCloseable autocloseable;

	private FrameFixture windowGymView;
	private DialogFixture windowManageMember;
	private FrameFixture windowManageCourse;
	private FrameFixture windowManageSubs;

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

	private List<Member> listMembers;
	private List<Course> listCourses;

	@Override
	protected void onSetUp() throws Exception {
		autocloseable = MockitoAnnotations.openMocks(this);

		listMembers = new ArrayList<>();
		listCourses = new ArrayList<>();

		// Stub repositories methods to use the list attributes
		when(memberRepository.findAll()).thenReturn(listMembers);
		when(courseRepository.findAll()).thenReturn(listCourses);
		doAnswer(answer((Member member) -> listMembers.add(member))).when(memberRepository).save(any());
		doAnswer(answer((Course course) -> listCourses.add(course))).when(courseRepository).save(any());

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
		windowManageCourse = new FrameFixture(robot(), dialogManageCourse);
		windowManageSubs = new FrameFixture(robot(), dialogManageSubs);

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
		Member member1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
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
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course1 = createTestCourse("name-1", Stream.of(member).collect(Collectors.toSet()));
		Course course2 = createTestCourse("name-2", Collections.emptySet());
		listCourses.add(course1);
		listCourses.add(course2);

		controller.allCourses();

		assertThat(windowGymView.list("listCourses").contents()).containsExactly(course1.toString(),
				course2.toString());
	}

	@Test
	@GUITest
	public void testButtonAddMemberWhenInsertIsOk() {
		String name = "name";
		String surname = "surname";
		LocalDate dateOfBirth = LocalDate.of(1996, 10, 31);

		windowGymView.button("buttonAddMember").click();
		windowManageMember.textBox("textFieldName").enterText(name);
		windowManageMember.textBox("textFieldSurname").enterText(surname);
		DatePicker datePicker = (DatePicker) windowManageMember.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(dateOfBirth));
		windowManageMember.button("buttonOk").click();

		Member newMember = createTestMember(name, surname, dateOfBirth);
		assertThat(windowGymView.list("listMembers").contents()).containsExactly(newMember.toString());
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
