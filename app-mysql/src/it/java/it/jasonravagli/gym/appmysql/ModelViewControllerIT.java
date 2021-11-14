package it.jasonravagli.gym.appmysql;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.lgooddatepicker.components.DatePicker;

import it.jasonravagli.gym.gui.SwingDialogManageCourse;
import it.jasonravagli.gym.gui.SwingDialogManageMember;
import it.jasonravagli.gym.gui.SwingDialogManageSubs;
import it.jasonravagli.gym.gui.SwingGymView;
import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;
import it.jasonravagli.gym.mysql.MySqlCourseRepository;
import it.jasonravagli.gym.mysql.MySqlMemberRepository;
import it.jasonravagli.gym.mysql.MySqlRepositoryProvider;
import it.jasonravagli.gym.mysql.MySqlTransactionManager;

@RunWith(GUITestRunner.class)
public class ModelViewControllerIT extends AssertJSwingJUnitTestCase {

	@Rule
	public RetryOnUbuntuRule retry = new RetryOnUbuntuRule(5);

	private static final String CONN_URL = "jdbc:mysql://localhost:3306/test";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "password";

	private Connection connection;

	private FrameFixture windowGymView;
	private DialogFixture windowManageMember;
	private DialogFixture windowManageCourse;
	private DialogFixture windowManageSubs;

	private SwingGymView swingGymView;
	private SwingDialogManageMember dialogManageMember;
	private SwingDialogManageCourse dialogManageCourse;
	private SwingDialogManageSubs dialogManageSubs;
	private GymController controller;
	private MySqlTransactionManager transactionManager;
	private MySqlMemberRepository memberRepository;
	private MySqlCourseRepository courseRepository;
	private MySqlRepositoryProvider repositoryProvider;

	@Override
	protected void onSetUp() throws Exception {
		connection = DriverManager.getConnection(CONN_URL, USERNAME, PASSWORD);

		memberRepository = new MySqlMemberRepository(connection);
		courseRepository = new MySqlCourseRepository(connection);
		
		// Empty the database using the repositories
		for (Course course : courseRepository.findAll()) {
			courseRepository.deleteById(course.getId());
		}
		for (Member member : memberRepository.findAll()) {
			memberRepository.deleteById(member.getId());
		}

		repositoryProvider = new MySqlRepositoryProvider(memberRepository, courseRepository);
		transactionManager = new MySqlTransactionManager(connection, repositoryProvider);

		controller = new GymController();
		controller.setTransactionManager(transactionManager);

		GuiActionRunner.execute(() -> {
			dialogManageMember = new SwingDialogManageMember(controller);
			dialogManageCourse = new SwingDialogManageCourse(controller);
			dialogManageSubs = new SwingDialogManageSubs(controller);

			swingGymView = new SwingGymView(controller, dialogManageMember, dialogManageCourse, dialogManageSubs);
			return swingGymView;
		});
		controller.setView(swingGymView);

		windowManageMember = new DialogFixture(robot(), dialogManageMember);
		windowManageCourse = new DialogFixture(robot(), dialogManageCourse);
		windowManageSubs = new DialogFixture(robot(), dialogManageSubs);

		windowGymView = new FrameFixture(robot(), swingGymView);
		windowGymView.show();
	}

	@Override
	public void onTearDown() throws Exception {
		if (connection != null && !connection.isClosed())
			connection.close();
	}

	@Test
	public void testAddMember() throws SQLException {
		String name = "name 1";
		String surname = "surname 1";
		LocalDate dateOfBirth = LocalDate.of(1996, 10, 31);

		windowGymView.button("buttonAddMember").click();
		windowManageMember.textBox("textFieldName").enterText(name);
		windowManageMember.textBox("textFieldSurname").enterText(surname);
		DatePicker datePicker = (DatePicker) windowManageMember.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(dateOfBirth));
		windowManageMember.button("buttonOk").click();

		List<Member> members = memberRepository.findAll();
		assertThat(members.size()).isEqualTo(1);
		Member member = members.get(0);
		assertThat(member.getName()).isEqualTo(name);
		assertThat(member.getSurname()).isEqualTo(surname);
		assertThat(member.getDateOfBirth()).isEqualTo(dateOfBirth);
	}

	@Test
	@GUITest
	public void testUpdateMemberWhenOperationOk() throws SQLException {
		Member member = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		memberRepository.save(member);
		String updatedName = "new name";
		String updatedSurname = "new surname";
		LocalDate updatedDateOfBirth = LocalDate.of(1995, 4, 28);

		controller.allMembers();
		windowGymView.list("listMembers").selectItem(0);
		windowGymView.button("buttonUpdateMember").click();
		windowManageMember.textBox("textFieldName").deleteText().enterText(updatedName);
		windowManageMember.textBox("textFieldSurname").deleteText().enterText(updatedSurname);
		DatePicker datePicker = (DatePicker) windowManageMember.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(updatedDateOfBirth));
		windowManageMember.button("buttonOk").click();

		Member updatedMember = new Member();
		updatedMember.setId(member.getId());
		updatedMember.setName(updatedName);
		updatedMember.setSurname(updatedSurname);
		updatedMember.setDateOfBirth(updatedDateOfBirth);
		assertThat(memberRepository.findById(member.getId())).isEqualTo(updatedMember);
	}

	@Test
	@GUITest
	public void testUpdateMemberWhenOperationCanceled() throws SQLException {
		Member member = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		memberRepository.save(member);
		String updatedName = "new name";
		String updatedSurname = "new surname";
		LocalDate updatedDateOfBirth = LocalDate.of(1995, 4, 28);

		controller.allMembers();
		windowGymView.list("listMembers").selectItem(0);
		windowGymView.button("buttonUpdateMember").click();
		windowManageMember.textBox("textFieldName").deleteText().enterText(updatedName);
		windowManageMember.textBox("textFieldSurname").deleteText().enterText(updatedSurname);
		DatePicker datePicker = (DatePicker) windowManageMember.panel("datePickerBirth").target();
		GuiActionRunner.execute(() -> datePicker.setDate(updatedDateOfBirth));
		windowManageMember.button("buttonCancel").click();

		assertThat(memberRepository.findById(member.getId())).isEqualTo(member);
	}

	@Test
	public void testDeleteMember() throws SQLException {
		Member member = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		memberRepository.save(member);

		controller.allMembers();
		windowGymView.list("listMembers").selectItem(0);
		windowGymView.button("buttonDeleteMember").click();

		assertThat(memberRepository.findById(member.getId())).isNull();
	}

	@Test
	public void testAddCourse() throws SQLException {
		String name = "course 1";

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.button("buttonAddCourse").click();
		windowManageCourse.textBox("textFieldName").enterText(name);
		windowManageCourse.button("buttonOk").click();

		List<Course> courses = courseRepository.findAll();
		assertThat(courses.size()).isEqualTo(1);
		Course course = courses.get(0);
		assertThat(course.getName()).isEqualTo(name);
		assertThat(course.getSubscribers()).isEmpty();
	}

	@Test
	public void testUpdateCourseWhenOperationOk() throws SQLException {
		Course course = createTestCourse("course 1", Collections.emptySet());
		courseRepository.save(course);
		String updatedName = "new course";

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(0);
		windowGymView.button("buttonUpdateCourse").click();
		windowManageCourse.textBox("textFieldName").deleteText().enterText(updatedName);
		windowManageCourse.button("buttonOk").click();

		Course updatedCourse = new Course();
		updatedCourse.setId(course.getId());
		updatedCourse.setName(updatedName);
		updatedCourse.setSubscribers(Collections.emptySet());
		assertThat(courseRepository.findById(course.getId())).isEqualTo(updatedCourse);
	}

	@Test
	public void testUpdateCourseWhenOperationCanceled() throws SQLException {
		Course course = createTestCourse("course 1", Collections.emptySet());
		courseRepository.save(course);
		String updatedName = "new course";

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(0);
		windowGymView.button("buttonUpdateCourse").click();
		windowManageCourse.textBox("textFieldName").deleteText().enterText(updatedName);
		windowManageCourse.button("buttonCancel").click();

		assertThat(courseRepository.findById(course.getId())).isEqualTo(course);
	}

	@Test
	public void testDeleteCourse() throws SQLException {
		Course course = createTestCourse("course 1", Collections.emptySet());
		courseRepository.save(course);

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(0);
		windowGymView.button("buttonDeleteCourse").click();

		assertThat(courseRepository.findById(course.getId())).isNull();
	}

	@Test
	@GUITest
	public void testManageSubsWhenOperationOk() throws SQLException {
		Member member = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		Course course = createTestCourse("course 1", Collections.emptySet());
		memberRepository.save(member);
		courseRepository.save(course);

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(0);
		windowGymView.button("buttonManageSubs").click();
		windowManageSubs.list("listOtherMembers").selectItem(0);
		windowManageSubs.button("buttonAddSub").click();
		windowManageSubs.button("buttonOk").click();

		Course updatedCourse = courseRepository.findById(course.getId());
		assertThat(updatedCourse.getSubscribers()).containsExactly(member);
	}

	@Test
	@GUITest
	public void testManageSubsWhenOperationCanceled() throws SQLException {
		Member member = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		Course course = createTestCourse("course 1", Collections.emptySet());
		memberRepository.save(member);
		courseRepository.save(course);

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(0);
		windowGymView.button("buttonManageSubs").click();
		windowManageSubs.list("listOtherMembers").selectItem(0);
		windowManageSubs.button("buttonAddSub").click();
		windowManageSubs.button("buttonCancel").click();

		Course retrievedCourse = courseRepository.findById(course.getId());
		assertThat(retrievedCourse.getSubscribers()).isEmpty();
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
