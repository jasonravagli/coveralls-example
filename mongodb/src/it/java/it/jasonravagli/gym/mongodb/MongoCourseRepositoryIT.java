package it.jasonravagli.gym.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public class MongoCourseRepositoryIT {

	private static final String MONGO_HOST = "localhost";
	private static final String MONGO_DATABASE = "test";
	private static final String MONGO_COLLECTION = "courses";

	// Get the docker container mapped port
	private static int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));

	private MongoClient client;
	private ClientSession clientSession;

	private AutoCloseable closeable;

	@Spy
	private MongoCollection<Document> courseCollection;
	private MongoCourseRepository repository;

	@Before
	public void setup() {
		client = new MongoClient(new ServerAddress(MONGO_HOST, mongoPort));
		clientSession = client.startSession();
		MongoDatabase database = client.getDatabase(MONGO_DATABASE);
		// Clean the database
		database.drop();
		courseCollection = database.getCollection(MONGO_COLLECTION);
		closeable = MockitoAnnotations.openMocks(this);

		repository = new MongoCourseRepository(courseCollection, clientSession);
	}

	@After
	public void tearDown() throws Exception {
		clientSession.close();
		client.close();
		closeable.close();
	}

	@Test
	public void testFindAllWhenDatabaseIsEmpty() {
		assertThat(repository.findAll()).isEmpty();
	}

	@Test
	public void testFindAllWhenDatabaseIsNotEmpty() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course1 = createTestCourse("coruse-1", Collections.emptySet());
		Course course2 = createTestCourse("course-2", Collections.singleton(member));
		courseCollection.insertMany(
				Stream.of(course1, course2).map(this::convertCourseToDbDocument).collect(Collectors.toList()));

		assertThat(repository.findAll()).containsExactly(course1, course2);
	}

	@Test
	public void testFindAllShouldUseClientSession() {
		repository.findAll();

		verify(courseCollection).find(clientSession);
	}

	@Test
	public void testFindByIdWhenCourseDoesNotExist() {
		Course retrieved = repository.findById(UUID.randomUUID());

		assertThat(retrieved).isNull();
	}

	@Test
	public void testFindByIdWhenCourseExists() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course1 = createTestCourse("course-1", Collections.emptySet());
		Course course2 = createTestCourse("course-2", Collections.singleton(member));
		courseCollection.insertMany(
				Stream.of(course1, course2).map(this::convertCourseToDbDocument).collect(Collectors.toList()));

		Course retrieved = repository.findById(course2.getId());

		assertThat(retrieved).isEqualTo(course2);
	}

	@Test
	public void testFindByIdShouldUseClientSession() {
		repository.findById(UUID.randomUUID());

		verify(courseCollection).find(eq(clientSession), any(Bson.class));
	}

	@Test
	public void testSave() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course = createTestCourse("course-1", Collections.singleton(member));

		repository.save(course);

		assertThat(readAllCoursesFromDatabase()).containsExactly(course);
	}

	@Test
	public void testSaveShoulUseClientSession() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course = createTestCourse("course-1", Collections.singleton(member));

		repository.save(course);

		verify(courseCollection).insertOne(eq(clientSession), any(Document.class));
	}

	@Test
	public void testDeleteById() {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course course1 = createTestCourse("course-1", Collections.emptySet());
		Course course2 = createTestCourse("course-2", Collections.singleton(member));
		courseCollection.insertMany(
				Stream.of(course1, course2).map(this::convertCourseToDbDocument).collect(Collectors.toList()));

		repository.deleteById(course2.getId());

		assertThat(readAllCoursesFromDatabase()).containsExactly(course1);
	}

	@Test
	public void testDeleteByIdShouldUseClientSession() {
		repository.deleteById(UUID.randomUUID());

		verify(courseCollection).deleteOne(eq(clientSession), any(Bson.class));
	}

	@Test
	public void testUpdate() {
		Course existingCourse = createTestCourse("course-1", Collections.emptySet());
		courseCollection.insertOne(convertCourseToDbDocument(existingCourse));

		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Course updatedCourse = createTestCourse("updated-1", Collections.singleton(member));
		updatedCourse.setId(existingCourse.getId());

		repository.update(updatedCourse);

		assertThat(readAllCoursesFromDatabase()).containsExactly(updatedCourse);
	}

	@Test
	public void testUpdateShouldUseClientSession() {
		Course updatedCourse = createTestCourse("updated-1", Collections.emptySet());

		repository.update(updatedCourse);

		verify(courseCollection).replaceOne(eq(clientSession), any(Bson.class), any(Document.class));
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

	private Document convertCourseToDbDocument(Course course) {
		return new Document().append("id", course.getId()).append("name", course.getName()).append("subscribers",
				course.getSubscribers().stream().map(this::convertMemberToDbDocument).collect(Collectors.toList()));
	}

	private Document convertMemberToDbDocument(Member member) {
		return new Document().append("id", member.getId()).append("name", member.getName())
				.append("surname", member.getSurname()).append("dateOfBirth", member.getDateOfBirth().toString());
	}

	private Course convertDocumentToCourse(Document doc) {
		Course course = new Course();
		course.setId((UUID) doc.get("id"));
		course.setName(doc.getString("name"));
		List<Document> listMemberDocs = doc.getList("subscribers", Document.class);
		course.setSubscribers(listMemberDocs.stream().map(this::convertDocumentToMember).collect(Collectors.toSet()));

		return course;
	}

	private Member convertDocumentToMember(Document doc) {
		Member member = new Member();
		member.setId((UUID) doc.get("id"));
		member.setName(doc.getString("name"));
		member.setSurname(doc.getString("surname"));
		member.setDateOfBirth(LocalDate.parse(doc.getString("dateOfBirth")));

		return member;
	}

	private List<Course> readAllCoursesFromDatabase() {
		return StreamSupport.stream(courseCollection.find().spliterator(), false).map(this::convertDocumentToCourse)
				.collect(Collectors.toList());
	}

}
