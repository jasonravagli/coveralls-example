package it.jasonravagli.gym.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public class MySqlCourseRepositoryIT {

	private static final String CONN_URL = "jdbc:mysql://localhost:3306/test";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "password";
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
	private static final String RETRIEVE_COURSES_QUERY = "SELECT BIN_TO_UUID(id) as uuid, name FROM " + TABLE_COURSES;
	private static final String RETRIEVE_SUBS_QUERY = "SELECT BIN_TO_UUID(id) as uuid_member, surname as surname_member, "
			+ "name as name_member, date_of_birth FROM " + TABLE_SUBS + " as s INNER JOIN " + TABLE_MEMBERS
			+ " as m ON s.id_member = m.id WHERE BIN_TO_UUID(s.id_course) = ?";
	private static final String RETRIEVE_ALL_SUBS_QUERY = "SELECT BIN_TO_UUID(id) as uuid_member, surname as surname_member, "
			+ "name as name_member, date_of_birth FROM " + TABLE_SUBS + " as s INNER JOIN " + TABLE_MEMBERS
			+ " as m ON s.id_member = m.id";

	private static final String EXCEPTION_MSG = "An exception occurred";

	private AutoCloseable autoCloseable;

	@Spy
	private Connection connection;

	private MySqlCourseRepository repository;

	@Before
	public void setUp() throws Exception {
		connection = DriverManager.getConnection(CONN_URL, USERNAME, PASSWORD);
		// Clean the database
		connection.prepareStatement("DELETE FROM " + TABLE_SUBS).executeUpdate();
		connection.prepareStatement("DELETE FROM " + TABLE_COURSES).executeUpdate();
		connection.prepareStatement("DELETE FROM " + TABLE_MEMBERS).executeUpdate();

		autoCloseable = MockitoAnnotations.openMocks(this);

		repository = new MySqlCourseRepository(connection);
	}

	@After
	public void tearDown() throws Exception {
		if (connection != null && !connection.isClosed())
			connection.close();
		autoCloseable.close();
	}

	@Test
	public void testFindAllWhenDatabaseIsEmpty() throws SQLException {
		assertThat(repository.findAll()).isEmpty();
	}

	@Test
	public void testFindAllWhenDatabaseContainsCoursesWithoutSubs() throws SQLException {
		Course course1 = createTestCourse("coruse-1", Collections.emptySet());
		Course course2 = createTestCourse("course-2", Collections.emptySet());
		insertCourseIntoDb(course1);
		insertCourseIntoDb(course2);

		assertThat(repository.findAll()).containsExactly(course1, course2);
	}

	@Test
	public void testFindAllWhenDatabaseContainsCoursesWithSubs() throws SQLException {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course course1 = createTestCourse("coruse-1", Collections.emptySet());
		Course course2 = createTestCourse("course-2", Stream.of(member).collect(Collectors.toSet()));
		insertCourseIntoDb(course1);
		insertCourseIntoDb(course2);

		assertThat(repository.findAll()).containsExactly(course1, course2);
	}

	@Test
	public void testFindByIdWhenCourseDoesNotExist() throws SQLException {
		assertThat(repository.findById(UUID.randomUUID())).isNull();
	}

	@Test
	public void testFindByIdWhenCourseExists() throws SQLException {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course course = createTestCourse("course-2", Stream.of(member).collect(Collectors.toSet()));
		insertCourseIntoDb(course);

		assertThat(repository.findById(course.getId())).isEqualTo(course);
	}

	@Test
	public void testSaveWhenCourseHasNoMembers() throws SQLException {
		Course course = createTestCourse("course-1", Collections.emptySet());

		repository.save(course);

		assertThat(readAllCoursesFromDatabase()).containsExactly(course);
	}

	@Test
	public void testSaveWhenCourseHasMembers() throws SQLException {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course course = createTestCourse("course-1", Stream.of(member).collect(Collectors.toSet()));

		repository.save(course);

		assertThat(readAllCoursesFromDatabase()).containsExactly(course);
	}

	@Test
	public void testUpdateWhenCourseHasNoMembers() throws SQLException {
		Course existingCourse = createTestCourse("course-1", Collections.emptySet());
		insertCourseIntoDb(existingCourse);

		Course updatedCourse = createTestCourse("course-updated", Collections.emptySet());
		updatedCourse.setId(existingCourse.getId());
		repository.update(updatedCourse);

		assertThat(readAllCoursesFromDatabase()).containsExactly(updatedCourse);
	}

	@Test
	public void testUpdateWhenCourseHasMembers() throws SQLException {
		Member member1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(member1);
		insertMemberIntoDb(member2);
		Course existingCourse = createTestCourse("course-1", Stream.of(member1).collect(Collectors.toSet()));
		insertCourseIntoDb(existingCourse);

		Course updatedCourse = createTestCourse("course-updated", Stream.of(member2).collect(Collectors.toSet()));
		updatedCourse.setId(existingCourse.getId());
		repository.update(updatedCourse);

		assertThat(readAllCoursesFromDatabase()).containsExactly(updatedCourse);
	}

	@Test
	public void testDeleteByIdWhenCourseHasNoMembers() throws SQLException {
		Course course1 = createTestCourse("course-1", Collections.emptySet());
		Course course2 = createTestCourse("course-2", Collections.emptySet());
		insertCourseIntoDb(course1);
		insertCourseIntoDb(course2);

		repository.deleteById(course2.getId());

		assertThat(readAllCoursesFromDatabase()).containsExactly(course1);
	}

	@Test
	public void testDeleteByIdWhenCourseHasMembers() throws SQLException {
		Member member1 = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		Member member2 = createTestMember("name-2", "surname-2", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(member1);
		insertMemberIntoDb(member2);
		Course course = createTestCourse("course-1", Stream.of(member1).collect(Collectors.toSet()));
		insertCourseIntoDb(course);

		repository.deleteById(course.getId());

		assertThat(readAllCoursesFromDatabase()).isEmpty();
		assertThat(readAllSubsFromDatabase()).isEmpty();
	}

	// ------ Additional tests to verify that statements are closed (their
	// implementation is harder and less readable) ------

	@Test
	public void testFindAllWhenEverythingOkShouldCloseAllStatements() throws SQLException {
		PreparedStatement statCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_ALL);
		doReturn(statCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_ALL);
		PreparedStatement statSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);
		doReturn(statSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);

		repository.findAll();

		assertThat(statCourse.isClosed()).isTrue();
		assertThat(statSubs.isClosed()).isTrue();
	}

	@Test
	public void testFindAllWhenSqlExceptionOccursShouldCatchCloseAllStatementsAndPropagateException()
			throws SQLException {
		PreparedStatement statCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_ALL);
		statCourse = Mockito.spy(statCourse);
		when(statCourse.executeQuery()).thenThrow(new SQLException(EXCEPTION_MSG));
		doReturn(statCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_ALL);
		PreparedStatement statSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);
		doReturn(statSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);

		assertThatThrownBy(() -> repository.findAll()).isInstanceOf(SQLException.class).hasMessage(EXCEPTION_MSG);
		assertThat(statCourse.isClosed()).isTrue();
		assertThat(statSubs.isClosed()).isTrue();
	}

	@Test
	public void testFindByIdWhenEverythingOkShouldCloseAllStatements() throws SQLException {
		PreparedStatement statCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_BY_ID);
		doReturn(statCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_BY_ID);
		PreparedStatement statSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);
		doReturn(statSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);

		repository.findById(UUID.randomUUID());

		assertThat(statCourse.isClosed()).isTrue();
		assertThat(statSubs.isClosed()).isTrue();
	}

	@Test
	public void testFindByIdWhenSqlExceptionOccursShouldCatchCloseTheStatementAndPropagateException()
			throws SQLException {
		UUID id = UUID.randomUUID();
		PreparedStatement statCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_BY_ID);
		statCourse = Mockito.spy(statCourse);
		doThrow(new SQLException(EXCEPTION_MSG)).when(statCourse).executeQuery();
		doReturn(statCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_BY_ID);
		PreparedStatement statSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);
		doReturn(statSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_FIND_SUBS);

		assertThatThrownBy(() -> repository.findById(id)).isInstanceOf(SQLException.class).hasMessage(EXCEPTION_MSG);
		assertThat(statCourse.isClosed()).isTrue();
		assertThat(statSubs.isClosed()).isTrue();
	}

	@Test
	public void testSaveWhenEverythingOkShouldCloseAllStatements() throws SQLException {
		PreparedStatement statInsertCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_INSERT_COURSE);
		statInsertCourse = Mockito.spy(statInsertCourse);
		doReturn(statInsertCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_INSERT_COURSE);
		PreparedStatement statInsertSub = connection.prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);
		statInsertSub = Mockito.spy(statInsertSub);
		doReturn(statInsertSub).when(connection).prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course course = createTestCourse("course-1", Stream.of(member).collect(Collectors.toSet()));

		repository.save(course);

		assertThat(statInsertCourse.isClosed()).isTrue();
		assertThat(statInsertSub.isClosed()).isTrue();
	}

	@Test
	public void testSaveWhenSqlExceptionOccursShouldCatchCloseAllStatementsAndPropagateException() throws SQLException {
		PreparedStatement statInsertCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_INSERT_COURSE);
		statInsertCourse = Mockito.spy(statInsertCourse);
		doReturn(statInsertCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_INSERT_COURSE);
		doThrow(new SQLException(EXCEPTION_MSG)).when(statInsertCourse).executeUpdate();
		PreparedStatement statInsertSub = connection.prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);
		statInsertSub = Mockito.spy(statInsertSub);
		doReturn(statInsertSub).when(connection).prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);

		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course course = createTestCourse("course-1", Stream.of(member).collect(Collectors.toSet()));

		assertThatThrownBy(() -> repository.save(course)).isInstanceOf(SQLException.class).hasMessage(EXCEPTION_MSG);

		assertThat(statInsertCourse.isClosed()).isTrue();
		assertThat(statInsertSub.isClosed()).isTrue();
	}

	@Test
	public void testUpdateWhenEverythingOkShouldCloseAllStatements() throws SQLException {
		PreparedStatement statUpdateCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_UPDATE_COURSE);
		statUpdateCourse = Mockito.spy(statUpdateCourse);
		doReturn(statUpdateCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_UPDATE_COURSE);
		PreparedStatement statDeleteSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		statDeleteSubs = Mockito.spy(statDeleteSubs);
		doReturn(statDeleteSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		PreparedStatement statInsertSub = connection.prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);
		statInsertSub = Mockito.spy(statInsertSub);
		doReturn(statInsertSub).when(connection).prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);

		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course existingCourse = createTestCourse("course-1", Collections.emptySet());
		insertCourseIntoDb(existingCourse);

		Course updatedCourse = createTestCourse("course-updated", Stream.of(member).collect(Collectors.toSet()));
		updatedCourse.setId(existingCourse.getId());
		repository.update(updatedCourse);

		assertThat(statUpdateCourse.isClosed()).isTrue();
		assertThat(statDeleteSubs.isClosed()).isTrue();
		assertThat(statInsertSub.isClosed()).isTrue();
	}

	@Test
	public void testUpdateWhenSqlExceptionOccursShouldCatchCloseAllStatementsAndPropagateException()
			throws SQLException {
		PreparedStatement statUpdateCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_UPDATE_COURSE);
		statUpdateCourse = Mockito.spy(statUpdateCourse);
		doReturn(statUpdateCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_UPDATE_COURSE);
		doThrow(new SQLException(EXCEPTION_MSG)).when(statUpdateCourse).executeUpdate();
		PreparedStatement statDeleteSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		statDeleteSubs = Mockito.spy(statDeleteSubs);
		doReturn(statDeleteSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		PreparedStatement statInsertSub = connection.prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);
		statInsertSub = Mockito.spy(statInsertSub);
		doReturn(statInsertSub).when(connection).prepareStatement(MySqlCourseRepository.QUERY_INSERT_SUB);

		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course existingCourse = createTestCourse("course-1", Collections.emptySet());
		insertCourseIntoDb(existingCourse);

		Course updatedCourse = createTestCourse("course-updated", Stream.of(member).collect(Collectors.toSet()));
		updatedCourse.setId(existingCourse.getId());

		assertThatThrownBy(() -> repository.update(updatedCourse)).isInstanceOf(SQLException.class)
				.hasMessage(EXCEPTION_MSG);
		assertThat(statUpdateCourse.isClosed()).isTrue();
		assertThat(statDeleteSubs.isClosed()).isTrue();
		assertThat(statInsertSub.isClosed()).isTrue();
	}

	@Test
	public void testDeleteByIdWhenEverythingOkShouldCloseAllStatements() throws SQLException {
		PreparedStatement statDeleteSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		statDeleteSubs = Mockito.spy(statDeleteSubs);
		doReturn(statDeleteSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		PreparedStatement statDeleteCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_DELETE_COURSE);
		statDeleteCourse = Mockito.spy(statDeleteCourse);
		doReturn(statDeleteCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_DELETE_COURSE);

		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course course = createTestCourse("course-1", Stream.of(member).collect(Collectors.toSet()));
		insertCourseIntoDb(course);

		repository.deleteById(course.getId());

		assertThat(statDeleteSubs.isClosed()).isTrue();
		assertThat(statDeleteCourse.isClosed()).isTrue();
	}

	@Test
	public void testDeleteByIdWhenSqlExceptionOccursShouldCatchCloseAllStatementsAndPropagateException()
			throws SQLException {
		PreparedStatement statDeleteSubs = connection.prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		statDeleteSubs = Mockito.spy(statDeleteSubs);
		doReturn(statDeleteSubs).when(connection).prepareStatement(MySqlCourseRepository.QUERY_DELETE_SUBS);
		doThrow(new SQLException(EXCEPTION_MSG)).when(statDeleteSubs).executeUpdate();
		PreparedStatement statDeleteCourse = connection.prepareStatement(MySqlCourseRepository.QUERY_DELETE_COURSE);
		statDeleteCourse = Mockito.spy(statDeleteCourse);
		doReturn(statDeleteCourse).when(connection).prepareStatement(MySqlCourseRepository.QUERY_DELETE_COURSE);

		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member);
		Course course = createTestCourse("course-1", Stream.of(member).collect(Collectors.toSet()));
		insertCourseIntoDb(course);

		assertThatThrownBy(() -> repository.deleteById(course.getId())).isInstanceOf(SQLException.class)
				.hasMessage(EXCEPTION_MSG);
		assertThat(statDeleteSubs.isClosed()).isTrue();
		assertThat(statDeleteCourse.isClosed()).isTrue();
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

	private List<Course> readAllCoursesFromDatabase() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(RETRIEVE_COURSES_QUERY);
		ResultSet rs = stat.executeQuery();

		List<Course> courses = new ArrayList<>();
		while (rs.next()) {
			UUID uuid = UUID.fromString(rs.getString("uuid"));
			String name = rs.getString("name");

			Course course = new Course();
			course.setId(uuid);
			course.setName(name);

			Set<Member> subscribers = new HashSet<>();
			PreparedStatement statSubs = connection.prepareStatement(RETRIEVE_SUBS_QUERY);
			statSubs.setString(1, course.getId().toString());
			ResultSet rsSubs = statSubs.executeQuery();
			while (rsSubs.next()) {
				subscribers.add(createSubFromResultSet(rsSubs));
			}
			course.setSubscribers(subscribers);

			courses.add(course);
		}

		return courses;
	}

	private List<Member> readAllSubsFromDatabase() throws SQLException {
		List<Member> subscribers = new ArrayList<>();
		PreparedStatement statSubs = connection.prepareStatement(RETRIEVE_ALL_SUBS_QUERY);
		ResultSet rsSubs = statSubs.executeQuery();
		while (rsSubs.next()) {
			subscribers.add(createSubFromResultSet(rsSubs));
		}

		return subscribers;
	}

	private Member createSubFromResultSet(ResultSet rsSubs) throws SQLException {
		UUID memberUuid = UUID.fromString(rsSubs.getString("uuid_member"));
		String memberName = rsSubs.getString("name_member");
		String memberSurname = rsSubs.getString("surname_member");
		LocalDate dateOfBirth = rsSubs.getObject("date_of_birth", LocalDate.class);

		Member member = new Member();
		member.setId(memberUuid);
		member.setName(memberName);
		member.setSurname(memberSurname);
		member.setDateOfBirth(dateOfBirth);
		return member;
	}
}
