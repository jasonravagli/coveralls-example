package it.jasonravagli.gym.appmongo;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.lgooddatepicker.components.DatePicker;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import it.jasonravagli.gym.gui.SwingDialogManageCourse;
import it.jasonravagli.gym.gui.SwingDialogManageMember;
import it.jasonravagli.gym.gui.SwingDialogManageSubs;
import it.jasonravagli.gym.gui.SwingGymView;
import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;
import it.jasonravagli.gym.mongodb.MongoCourseRepository;
import it.jasonravagli.gym.mongodb.MongoMemberRepository;
import it.jasonravagli.gym.mongodb.MongoRepositoryProvider;
import it.jasonravagli.gym.mongodb.MongoTransactionManager;

@RunWith(GUITestRunner.class)
public class ModelViewControllerIT extends AssertJSwingJUnitTestCase {

	@Rule
	public RetryOnUbuntuRule retry = new RetryOnUbuntuRule(5);

	private static final String MONGO_HOST = "localhost";
	private static final String MONGO_DATABASE = "test";
	private static final String MONGO_MEMBER_COLLECTION = "members";
	private static final String MONGO_COURSE_COLLECTION = "courses";
	private static final int MONGO_PORT = 27017;

	private MongoClient client;
	private ClientSession clientSession;

	private FrameFixture windowGymView;
	private DialogFixture windowManageMember;
	private DialogFixture windowManageCourse;
	private DialogFixture windowManageSubs;

	private SwingGymView swingGymView;
	private SwingDialogManageMember dialogManageMember;
	private SwingDialogManageCourse dialogManageCourse;
	private SwingDialogManageSubs dialogManageSubs;
	private GymController controller;
	private MongoTransactionManager transactionManager;
	private MongoMemberRepository memberRepository;
	private MongoCourseRepository courseRepository;
	private MongoRepositoryProvider repositoryProvider;

	@Override
	protected void onSetUp() throws Exception {
		client = new MongoClient(new ServerAddress(MONGO_HOST, MONGO_PORT));
		clientSession = client.startSession();
		MongoDatabase database = client.getDatabase(MONGO_DATABASE);

		MongoCollection<Document> memberCollection = database.getCollection(MONGO_MEMBER_COLLECTION);
		MongoCollection<Document> courseCollection = database.getCollection(MONGO_COURSE_COLLECTION);

		memberRepository = new MongoMemberRepository(memberCollection, clientSession);
		courseRepository = new MongoCourseRepository(courseCollection, clientSession);

		// Empty the database using the repositories
		for (Member member : memberRepository.findAll()) {
			memberRepository.deleteById(member.getId());
		}
		for (Course course : courseRepository.findAll()) {
			courseRepository.deleteById(course.getId());
		}

		repositoryProvider = new MongoRepositoryProvider(memberRepository, courseRepository);
		transactionManager = new MongoTransactionManager(clientSession, repositoryProvider);

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
		clientSession.close();
		client.close();
	}

	@Test
	public void testAddMember() {
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
	public void testUpdateMemberWhenOperationOk() {
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
	public void testUpdateMemberWhenOperationCanceled() {
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
	public void testDeleteMember() {
		Member member = createTestMember("name 1", "surname 1", LocalDate.of(1996, 10, 31));
		memberRepository.save(member);

		controller.allMembers();
		windowGymView.list("listMembers").selectItem(0);
		windowGymView.button("buttonDeleteMember").click();

		assertThat(memberRepository.findById(member.getId())).isNull();
	}

	@Test
	public void testAddCourse() {
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
	public void testUpdateCourseWhenOperationOk() {
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
	public void testUpdateCourseWhenOperationCanceled() {
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
	public void testDeleteCourse() {
		Course course = createTestCourse("course 1", Collections.emptySet());
		courseRepository.save(course);

		windowGymView.tabbedPane("tabbedPaneMain").selectTab("Courses");
		windowGymView.list("listCourses").selectItem(0);
		windowGymView.button("buttonDeleteCourse").click();

		assertThat(courseRepository.findById(course.getId())).isNull();
	}

	@Test
	@GUITest
	public void testManageSubsWhenOperationOk() {
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
	public void testManageSubsWhenOperationCanceled() {
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
