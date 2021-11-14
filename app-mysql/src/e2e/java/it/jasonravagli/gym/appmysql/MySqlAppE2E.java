package it.jasonravagli.gym.appmysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.lgooddatepicker.components.DatePicker;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

@RunWith(GUITestRunner.class)
public class MySqlAppE2E extends AssertJSwingJUnitTestCase {

	@Rule
	public RetryOnUbuntuRule retry = new RetryOnUbuntuRule(5);

	private static final String HOST = "localhost";
	private static final int PORT = 3306;
	private static final String USERNAME = "root";
	private static final String PASSWORD = "password";
	private static final String DATABASE = "test";
	private static final String TABLE_COURSES = "courses";
	private static final String TABLE_MEMBERS = "members";
	private static final String TABLE_SUBS = "subscriptions";

	// Queries
	private static final String INSERT_MEMBER_QUERY = "INSERT INTO " + TABLE_MEMBERS
			+ "(id, name, surname, date_of_birth) VALUES(UUID_TO_BIN(?),?,?,?)";
	private static final String INSERT_COURSE_QUERY = "INSERT INTO " + TABLE_COURSES
			+ "(id, name) VALUES(UUID_TO_BIN(?),?)";
	private static final String INSERT_SUB_QUERY = "INSERT INTO " + TABLE_SUBS
			+ "(id_member, id_course) VALUES(UUID_TO_BIN(?),UUID_TO_BIN(?))";

	private Connection connection;

	private FrameFixture window;

	private static final String FRAME_TITLE = "Gym Manager";

	// Data inside database
	private static final String MEMBER_1_SURNAME = "surname-1";
	private static final String MEMBER_1_NAME = "name-1";
	private static final LocalDate MEMBER_1_DOB = LocalDate.of(1996, 10, 31);
	private static final String MEMBER_2_SURNAME = "surname-2";
	private static final String MEMBER_2_NAME = "name-2";
	private static final LocalDate MEMBER_2_DOB = LocalDate.of(1996, 4, 30);
	private static final String COURSE_1_NAME = "course-1";

	@Override
	protected void onSetUp() throws Exception {
		String connectionUrl = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE;
		connection = DriverManager.getConnection(connectionUrl, USERNAME, PASSWORD);

		// Start with an empty database
		connection.prepareStatement("DELETE FROM " + TABLE_SUBS).executeUpdate();
		connection.prepareStatement("DELETE FROM " + TABLE_COURSES).executeUpdate();
		connection.prepareStatement("DELETE FROM " + TABLE_MEMBERS).executeUpdate();

		// Setup an initial state for the database
		Member member1 = createTestMember(MEMBER_1_NAME, MEMBER_1_SURNAME, MEMBER_1_DOB);
		Member member2 = createTestMember(MEMBER_2_NAME, MEMBER_2_SURNAME, MEMBER_2_DOB);
		insertMemberIntoDb(member1);
		insertMemberIntoDb(member2);
		Course course = createTestCourse(COURSE_1_NAME, Stream.of(member2).collect(Collectors.toSet()));
		insertCourseIntoDb(course);

		// Start the application (use the default password by not specifying the mysql-pwd argument)
		application("it.jasonravagli.gym.appmysql.MySqlGymApp").withArgs("--mysql-host=" + HOST,
				"--mysql-port=" + PORT, "--mysql-user=" + USERNAME, "--db-name=" + DATABASE)
				.start();

		// Connect to the application JFrame
		window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
			@Override
			protected boolean isMatching(JFrame frame) {
				return FRAME_TITLE.equals(frame.getTitle()) && frame.isShowing();
			}
		}).using(robot());
	}

	@Override
	public void onTearDown() throws Exception {
		connection.close();
	}

	@Test
	@GUITest
	public void testWhenApplicationStartsShouldShowAllMembers() {
		assertThat(window.list().contents()).anyMatch(el -> el.contains(MEMBER_1_SURNAME) && el.contains(MEMBER_1_NAME)
				&& el.contains(MEMBER_1_DOB.toString()));
		assertThat(window.list().contents()).anyMatch(el -> el.contains(MEMBER_2_SURNAME) && el.contains(MEMBER_2_NAME)
				&& el.contains(MEMBER_2_DOB.toString()));
	}

	@Test
	@GUITest
	public void testWhenCourseTabIsSelectedShouldShowAllCourses() {
		window.tabbedPane().selectTab("Courses");
		assertThat(window.list().contents()).anyMatch(el -> el.contains(COURSE_1_NAME));
	}

	@Test
	@GUITest
	public void testAddMember() {
		String memberName = "new name";
		String memberSurname = "new surname";
		LocalDate memberBirth = LocalDate.of(1995, 4, 28);

		window.button(JButtonMatcher.withText("Add").andShowing()).click();
		DialogFixture dialogManageMember = findShowingDialog();
		dialogManageMember.textBox("textFieldName").enterText(memberName);
		dialogManageMember.textBox("textFieldSurname").enterText(memberSurname);
		// We cannot interact with the date picker component using AssertJ Swing
		DatePicker datePickerBirth = dialogManageMember.panel("datePickerBirth").targetCastedTo(DatePicker.class);
		GuiActionRunner.execute(() -> datePickerBirth.setDate(memberBirth));
		dialogManageMember.button(JButtonMatcher.withText("OK")).click();

		assertThat(window.list().contents()).anyMatch(
				el -> el.contains(memberSurname) && el.contains(memberName) && el.contains(memberBirth.toString()));
	}

	@Test
	@GUITest
	public void testUpdateMember() {
		String updatedName = "updated name";
		String updatedSurname = "updated surname";
		LocalDate updatedBirth = LocalDate.of(1994, 7, 10);

		window.list().selectItem(Pattern.compile(".*" + MEMBER_1_SURNAME + ".*"));
		window.button(JButtonMatcher.withText("Update").andShowing()).click();
		DialogFixture dialogManageMember = findShowingDialog();
		dialogManageMember.textBox("textFieldName").deleteText().enterText(updatedName);
		dialogManageMember.textBox("textFieldSurname").deleteText().enterText(updatedSurname);
		// We cannot interact with the date picker component using AssertJ Swing
		DatePicker datePickerBirth = dialogManageMember.panel("datePickerBirth").targetCastedTo(DatePicker.class);
		GuiActionRunner.execute(() -> datePickerBirth.setDate(updatedBirth));
		dialogManageMember.button(JButtonMatcher.withText("OK")).click();

		assertThat(window.list().contents()).anyMatch(
				el -> el.contains(updatedSurname) && el.contains(updatedName) && el.contains(updatedBirth.toString()));
	}

	@Test
	@GUITest
	public void testDeleteMember() {
		window.list().selectItem(Pattern.compile(".*" + MEMBER_2_SURNAME + ".*"));
		window.button(JButtonMatcher.withText("Delete").andShowing()).click();

		assertThat(window.list().contents()).noneMatch(el -> el.contains(MEMBER_2_SURNAME));
	}

	@Test
	@GUITest
	public void testAddCourse() {
		String courseName = "new course";

		window.tabbedPane().selectTab("Courses");
		window.button(JButtonMatcher.withText("Add").andShowing()).click();
		DialogFixture dialogMangeCourse = findShowingDialog();
		dialogMangeCourse.textBox("textFieldName").enterText(courseName);
		dialogMangeCourse.button(JButtonMatcher.withText("OK").andShowing()).click();

		assertThat(window.list().contents()).anyMatch(el -> el.contains(courseName));
	}

	@Test
	@GUITest
	public void testUpdateCourse() {
		String updatedName = "updated course";

		window.tabbedPane().selectTab("Courses");
		window.list().selectItem(Pattern.compile(".*" + COURSE_1_NAME + ".*"));
		window.button(JButtonMatcher.withText("Update").andShowing()).click();
		DialogFixture dialogMangeCourse = findShowingDialog();
		dialogMangeCourse.textBox("textFieldName").deleteText().enterText(updatedName);
		dialogMangeCourse.button(JButtonMatcher.withText("OK").andShowing()).click();

		assertThat(window.list().contents()).anyMatch(el -> el.contains(updatedName));
	}

	@Test
	@GUITest
	public void testDeleteCourse() {
		window.tabbedPane().selectTab("Courses");
		window.list().selectItem(Pattern.compile(".*" + COURSE_1_NAME + ".*"));
		window.button(JButtonMatcher.withText("Delete").andShowing()).click();

		assertThat(window.list().contents()).noneMatch(el -> el.contains(COURSE_1_NAME));
	}

	@Test
	@GUITest
	public void testManageSubs() {
		String subscriberSurname = MEMBER_2_SURNAME;
		String otherMemberSurname = MEMBER_1_SURNAME;

		window.tabbedPane().selectTab("Courses");
		window.list().selectItem(Pattern.compile(".*" + COURSE_1_NAME + ".*"));
		window.button(JButtonMatcher.withText("Manage Subs.").andShowing()).click();
		DialogFixture dialogManageSubs = findShowingDialog();
		dialogManageSubs.list("listOtherMembers").selectItem(Pattern.compile(".*" + otherMemberSurname + ".*"));
		dialogManageSubs.button("buttonAddSub").click();
		dialogManageSubs.list("listSubs").selectItem(Pattern.compile(".*" + subscriberSurname + ".*"));
		dialogManageSubs.button("buttonRemoveSub").click();
		dialogManageSubs.button(JButtonMatcher.withText("OK").andShowing()).click();

		// Reopen the dialog and check the lists contents to ensure that subscribers
		// have been modified correctly
		window.list().selectItem(Pattern.compile(".*" + COURSE_1_NAME + ".*"));
		window.button(JButtonMatcher.withText("Manage Subs.").andShowing()).click();
		dialogManageSubs = findShowingDialog();
		assertThat(dialogManageSubs.list("listOtherMembers").contents()).anyMatch(el -> el.contains(subscriberSurname));
		assertThat(dialogManageSubs.list("listSubs").contents()).anyMatch(el -> el.contains(otherMemberSurname));
	}

	// ----- Utility methods -----

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

	private void insertMemberIntoDb(Member member) throws SQLException {
		PreparedStatement stat = connection.prepareStatement(INSERT_MEMBER_QUERY);
		stat.setString(1, member.getId().toString());
		stat.setString(2, member.getName());
		stat.setString(3, member.getSurname());
		stat.setObject(4, member.getDateOfBirth());
		stat.executeUpdate();
	}

	private void insertCourseIntoDb(Course course) throws SQLException {
		PreparedStatement stat = connection.prepareStatement(INSERT_COURSE_QUERY);
		stat.setString(1, course.getId().toString());
		stat.setString(2, course.getName());
		stat.executeUpdate();

		for (Member member : course.getSubscribers()) {
			insertSubIntoDb(course, member);
		}
	}

	private void insertSubIntoDb(Course course, Member member) throws SQLException {
		PreparedStatement stat = connection.prepareStatement(INSERT_SUB_QUERY);
		stat.setString(1, member.getId().toString());
		stat.setString(2, course.getId().toString());
		stat.executeUpdate();
	}

	private DialogFixture findShowingDialog() {
		return WindowFinder.findDialog(new GenericTypeMatcher<JDialog>(JDialog.class) {

			@Override
			protected boolean isMatching(JDialog dialog) {
				return dialog.isShowing();
			}
		}).using(robot());
	}

}
