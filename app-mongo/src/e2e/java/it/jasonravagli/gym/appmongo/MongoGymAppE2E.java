package it.jasonravagli.gym.appmongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

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
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.lgooddatepicker.components.DatePicker;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

@RunWith(GUITestRunner.class)
public class MongoGymAppE2E extends AssertJSwingJUnitTestCase {

	@Rule
	public RetryOnUbuntuRule retry = new RetryOnUbuntuRule(5);

	private static final String MONGO_HOST = "localhost";
	private static final String MONGO_DATABASE = "gym";
	private static final String MONGO_MEMBER_COLLECTION = "members";
	private static final String MONGO_COURSE_COLLECTION = "courses";
	private static final int MONGO_PORT = 27017;

	private MongoClient client;

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
		client = new MongoClient(new ServerAddress(MONGO_HOST, MONGO_PORT));
		MongoDatabase database = client.getDatabase(MONGO_DATABASE);

		// Start with an empty database
		database.drop();

		// Setup an initial state for the database
		MongoCollection<Document> memberCollection = database.getCollection(MONGO_MEMBER_COLLECTION);
		MongoCollection<Document> courseCollection = database.getCollection(MONGO_COURSE_COLLECTION);
		Member member1 = createTestMember(MEMBER_1_NAME, MEMBER_1_SURNAME, MEMBER_1_DOB);
		Member member2 = createTestMember(MEMBER_2_NAME, MEMBER_2_SURNAME, MEMBER_2_DOB);
		memberCollection.insertOne(convertMemberToDbDocument(member1));
		memberCollection.insertOne(convertMemberToDbDocument(member2));
		Course course = createTestCourse(COURSE_1_NAME, Stream.of(member2).collect(Collectors.toSet()));
		courseCollection.insertOne(convertCourseToDbDocument(course));

		// Start the application
		application("it.jasonravagli.gym.appmongo.MongoGymApp")
				.withArgs("--mongo-host=" + MONGO_HOST, "--mongo-port=" + MONGO_PORT, "--db-name=" + MONGO_DATABASE)
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
		client.close();
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

	private Document convertMemberToDbDocument(Member member) {
		return new Document().append("id", member.getId()).append("name", member.getName())
				.append("surname", member.getSurname()).append("dateOfBirth", member.getDateOfBirth().toString());
	}

	private Document convertCourseToDbDocument(Course course) {
		return new Document().append("id", course.getId()).append("name", course.getName()).append("subscribers",
				course.getSubscribers().stream().map(this::convertMemberToDbDocument).collect(Collectors.toList()));
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
