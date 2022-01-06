package it.jasonravagli.gym.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.time.LocalDate;
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

public class MongoMemberRepositoryIT {

	private static final String MONGO_HOST = "localhost";
	private static final String MONGO_DATABASE = "test";
	private static final String MONGO_MEMBER_COLLECTION = "members";
	private static final String MONGO_COURSE_COLLECTION = "courses";
	private static final int MONGO_PORT = 27017;

	private MongoClient client;
	private ClientSession clientSession;

	private AutoCloseable closeable;

	@Spy
	private MongoCollection<Document> memberCollection;

	@Spy
	private MongoCollection<Document> courseCollection;

	private MongoMemberRepository repository;

	@Before
	public void setup() {
		client = new MongoClient(new ServerAddress(MONGO_HOST, MONGO_PORT));
		clientSession = client.startSession();
		MongoDatabase database = client.getDatabase(MONGO_DATABASE);
		// Clean the database
		database.drop();
		memberCollection = database.getCollection(MONGO_MEMBER_COLLECTION);
		courseCollection = database.getCollection(MONGO_COURSE_COLLECTION);
		closeable = MockitoAnnotations.openMocks(this);

		repository = new MongoMemberRepository(memberCollection, courseCollection, clientSession);
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
		Member member1 = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		Member member2 = createTestMember("test-name-2", "test-surname-2", LocalDate.of(1996, 10, 31));
		memberCollection.insertMany(Stream.of(convertMemberToDbDocument(member1), convertMemberToDbDocument(member2))
				.collect(Collectors.toList()));

		List<Member> retrievedMembers = repository.findAll();

		assertThat(retrievedMembers).containsExactly(member1, member2);
	}

	@Test
	public void testFindAllShouldUseClientSession() {
		repository.findAll();

		verify(memberCollection).find(clientSession);
	}

	@Test
	public void testFindByIdWhenMemberDoesNotExist() {
		assertThat(repository.findById(UUID.randomUUID())).isNull();
	}

	@Test
	public void testFindByIdWhenMemberExists() {
		Member member1 = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		Member member2 = createTestMember("test-name-2", "test-surname-2", LocalDate.of(1996, 10, 31));
		memberCollection.insertMany(Stream.of(convertMemberToDbDocument(member1), convertMemberToDbDocument(member2))
				.collect(Collectors.toList()));

		assertThat(repository.findById(member2.getId())).isEqualTo(member2);
	}

	@Test
	public void testFindByIdShouldUseClientSession() {
		repository.findById(UUID.randomUUID());

		verify(memberCollection).find(eq(clientSession), any(Bson.class));
	}

	@Test
	public void testSave() {
		Member member = createTestMember("test-name", "test-surname", LocalDate.of(1995, 4, 28));

		repository.save(member);

		assertThat(readAllMembersFromDatabase()).containsExactly(member);
	}

	@Test
	public void testSaveShouldUseClientSession() {
		Member member = createTestMember("test-name", "test-surname", LocalDate.of(1995, 4, 28));

		repository.save(member);

		verify(memberCollection).insertOne(eq(clientSession), any(Document.class));
	}

	@Test
	public void testDeleteById() {
		Member member1 = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		Member member2 = createTestMember("test-name-2", "test-surname-2", LocalDate.of(1996, 10, 31));
		memberCollection.insertMany(Stream.of(convertMemberToDbDocument(member1), convertMemberToDbDocument(member2))
				.collect(Collectors.toList()));

		repository.deleteById(member1.getId());

		assertThat(readAllMembersFromDatabase()).containsExactly(member2);
	}

	@Test
	public void testDeleteByIdShouldUseClientSession() {
		repository.deleteById(UUID.randomUUID());

		verify(memberCollection).deleteOne(eq(clientSession), any(Bson.class));
	}

	@Test
	public void testDeleteByIdShouldDeleteMemberSubscriptionsUsingClientSession() throws SQLException {
		Member member1 = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		Member member2 = createTestMember("test-name-2", "test-surname-2", LocalDate.of(1996, 10, 31));
		memberCollection.insertMany(Stream.of(convertMemberToDbDocument(member1), convertMemberToDbDocument(member2))
				.collect(Collectors.toList()));
		Course existingCourse = createTestCourse("course-1", Stream.of(member1, member2).collect(Collectors.toSet()));
		courseCollection.insertOne(convertCourseToDbDocument(existingCourse));

		repository.deleteById(member2.getId());

		Course retrievedCourse = readAllCoursesFromDatabase().get(0);
		assertThat(retrievedCourse.getSubscribers()).containsExactly(member1);
		verify(courseCollection).replaceOne(eq(clientSession), any(Bson.class), any(Document.class));
	}

	@Test
	public void testUpdate() {
		Member existingMember = createTestMember("test-name", "test-surname", LocalDate.of(1995, 4, 28));
		memberCollection.insertOne(convertMemberToDbDocument(existingMember));

		Member updatedMember = createTestMember("updated-name", "updated-surname", LocalDate.of(1996, 10, 31));
		updatedMember.setId(existingMember.getId());

		repository.update(updatedMember);

		assertThat(readAllMembersFromDatabase()).containsExactly(updatedMember);
	}

	@Test
	public void testUpdateShouldUseClientSession() {
		Member updatedMember = createTestMember("updated-name", "updated-surname", LocalDate.of(1996, 10, 31));

		repository.update(updatedMember);

		verify(memberCollection).replaceOne(eq(clientSession), any(Bson.class), any(Document.class));
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

	private Member convertDocumentToMember(Document doc) {
		Member member = new Member();
		member.setId((UUID) doc.get("id"));
		member.setName(doc.getString("name"));
		member.setSurname(doc.getString("surname"));
		member.setDateOfBirth(LocalDate.parse(doc.getString("dateOfBirth")));

		return member;
	}

	private Course convertDocumentToCourse(Document doc) {
		Course course = new Course();
		course.setId((UUID) doc.get("id"));
		course.setName(doc.getString("name"));
		List<Document> listMemberDocs = doc.getList("subscribers", Document.class);
		course.setSubscribers(listMemberDocs.stream().map(this::convertDocumentToMember).collect(Collectors.toSet()));

		return course;
	}

	private List<Member> readAllMembersFromDatabase() {
		return StreamSupport.stream(memberCollection.find().spliterator(), false).map(this::convertDocumentToMember)
				.collect(Collectors.toList());
	}

	private List<Course> readAllCoursesFromDatabase() {
		return StreamSupport.stream(courseCollection.find().spliterator(), false).map(this::convertDocumentToCourse)
				.collect(Collectors.toList());
	}
}
