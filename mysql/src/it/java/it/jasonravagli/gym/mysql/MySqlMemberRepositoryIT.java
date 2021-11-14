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

public class MySqlMemberRepositoryIT {

	private static final String CONN_URL = "jdbc:mysql://localhost:3306/test";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "password";
	private static final String TABLE_COURSES = "courses";
	private static final String TABLE_MEMBERS = "members";
	private static final String TABLE_SUBS = "subscriptions";

	// Queries
	private static final String INSERT_QUERY = "INSERT INTO " + TABLE_MEMBERS
			+ "(id, name, surname, date_of_birth) VALUES(UUID_TO_BIN(?),?,?,?)";
	private static final String INSERT_COURSE_QUERY = "INSERT INTO " + TABLE_COURSES
			+ "(id, name) VALUES(UUID_TO_BIN(?),?)";
	private static final String INSERT_SUB_QUERY = "INSERT INTO " + TABLE_SUBS
			+ "(id_member, id_course) VALUES(UUID_TO_BIN(?),UUID_TO_BIN(?))";
	private static final String RETRIEVE_QUERY = "SELECT BIN_TO_UUID(id) as uuid, name, surname, date_of_birth FROM "
			+ TABLE_MEMBERS;
	private static final String RETRIEVE_ALL_SUBS_QUERY = "SELECT BIN_TO_UUID(id) as uuid_member, surname as surname_member, "
			+ "name as name_member, date_of_birth FROM " + TABLE_SUBS + " as s INNER JOIN " + TABLE_MEMBERS
			+ " as m ON s.id_member = m.id";

	private static final String EXCEPTION_MSG = "An exception occurred";

	AutoCloseable autoCloseable;

	@Spy
	private Connection connection;

	private MySqlMemberRepository repository;

	@Before
	public void setUp() throws Exception {
		connection = DriverManager.getConnection(CONN_URL, USERNAME, PASSWORD);
		// Clean the database
		connection.prepareStatement("DELETE FROM " + TABLE_SUBS).executeUpdate();
		connection.prepareStatement("DELETE FROM " + TABLE_COURSES).executeUpdate();
		connection.prepareStatement("DELETE FROM " + TABLE_MEMBERS).executeUpdate();

		autoCloseable = MockitoAnnotations.openMocks(this);

		repository = new MySqlMemberRepository(connection);
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
	public void testFindAllWhenDatabaseIsNotEmpty() throws SQLException {
		Member member1 = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		Member member2 = createTestMember("test-name-2", "test-surname-2", LocalDate.of(1996, 10, 31));
		insertMemberIntoDb(member1);
		insertMemberIntoDb(member2);

		assertThat(repository.findAll()).containsExactly(member1, member2);
	}

	@Test
	public void testFindByIdWhenMemberDoesNotExist() throws SQLException {
		assertThat(repository.findById(UUID.randomUUID())).isNull();
	}

	@Test
	public void testFindByIdWhenMemberExists() throws SQLException {
		Member member = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(member);

		assertThat(repository.findById(member.getId())).isEqualTo(member);
	}

	@Test
	public void testSave() throws SQLException {
		Member member = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		repository.save(member);

		assertThat(readAllMembersFromDatabase()).containsExactly(member);
	}

	@Test
	public void testUpdate() throws SQLException {
		Member existingMember = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(existingMember);
		Member updatedMember = createTestMember("updated-name", "updated-surname", LocalDate.of(1996, 10, 31));
		updatedMember.setId(existingMember.getId());

		repository.update(updatedMember);

		assertThat(readAllMembersFromDatabase()).containsExactly(updatedMember);
	}

	@Test
	public void testDeleteById() throws SQLException {
		Member existingMember = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(existingMember);

		repository.deleteById(existingMember.getId());

		assertThat(readAllMembersFromDatabase()).isEmpty();
	}

	@Test
	public void testDeleteByIdShouldDeleteMemberSubscriptions() throws SQLException {
		Member existingMember = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(existingMember);
		Course existingCourse = createTestCourse("course-1", Stream.of(existingMember).collect(Collectors.toSet()));
		insertCourseIntoDb(existingCourse);

		repository.deleteById(existingMember.getId());

		assertThat(readAllSubsFromDatabase()).isEmpty();
	}

	// ------ Additional tests to verify that statements are closed (their
	// implementation is harder and less readable) ------

	@Test
	public void testFindAllWhenEverythingOkShouldCloseTheStatement() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_FIND_ALL);
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_FIND_ALL);

		repository.findAll();

		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testFindAllWhenSqlExceptionOccursShouldCatchCloseTheStatementAndPropagateException()
			throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_FIND_ALL);
		stat = Mockito.spy(stat);
		when(stat.executeQuery()).thenThrow(new SQLException(EXCEPTION_MSG));
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_FIND_ALL);

		assertThatThrownBy(() -> repository.findAll()).isInstanceOf(SQLException.class).hasMessage(EXCEPTION_MSG);
		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testFindByIdWhenEverythingOkShouldCloseTheStatement() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_FIND_BY_ID);
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_FIND_BY_ID);

		repository.findById(UUID.randomUUID());

		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testFindByIdWhenSqlExceptionOccursShouldCatchCloseTheStatementAndPropagateException()
			throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_FIND_BY_ID);
		stat = Mockito.spy(stat);
		doThrow(new SQLException(EXCEPTION_MSG)).when(stat).executeQuery();
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_FIND_BY_ID);

		assertThatThrownBy(() -> repository.findById(UUID.randomUUID())).isInstanceOf(SQLException.class)
				.hasMessage(EXCEPTION_MSG);
		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testSaveWhenEverythingOkShouldCloseTheStatement() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_INSERT);
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_INSERT);
		Member member = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));

		repository.save(member);

		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testSaveWhenSqlExceptionOccursShouldCatchCloseTheStatementAndPropagateException() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_INSERT);
		stat = Mockito.spy(stat);
		doThrow(new SQLException(EXCEPTION_MSG)).when(stat).executeUpdate();
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_INSERT);
		Member member = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));

		assertThatThrownBy(() -> repository.save(member)).isInstanceOf(SQLException.class).hasMessage(EXCEPTION_MSG);
		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testUpdateWhenEverythingOkShouldCloseTheStatement() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_UPDATE);
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_UPDATE);
		Member existingMember = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(existingMember);
		Member updatedMember = createTestMember("updated-name", "updated-surname", LocalDate.of(1996, 10, 31));
		updatedMember.setId(existingMember.getId());

		repository.update(updatedMember);

		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testUpdateWhenSqlExceptionOccursShouldCatchCloseTheStatementAndPropagateException()
			throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_UPDATE);
		stat = Mockito.spy(stat);
		doThrow(new SQLException(EXCEPTION_MSG)).when(stat).executeUpdate();
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_UPDATE);
		Member existingMember = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(existingMember);
		Member updatedMember = createTestMember("updated-name", "updated-surname", LocalDate.of(1996, 10, 31));
		updatedMember.setId(existingMember.getId());

		assertThatThrownBy(() -> repository.update(updatedMember)).isInstanceOf(SQLException.class)
				.hasMessage(EXCEPTION_MSG);
		assertThat(stat.isClosed()).isTrue();
	}

	@Test
	public void testDeleteByIdWhenEverythingOkShouldCloseTheStatement() throws SQLException {
		PreparedStatement statDeleteMember = connection.prepareStatement(MySqlMemberRepository.QUERY_DELETE_MEMBER);
		doReturn(statDeleteMember).when(connection).prepareStatement(MySqlMemberRepository.QUERY_DELETE_MEMBER);
		PreparedStatement statDeleteSubs = connection.prepareStatement(MySqlMemberRepository.QUERY_DELETE_SUBS);
		doReturn(statDeleteSubs).when(connection).prepareStatement(MySqlMemberRepository.QUERY_DELETE_SUBS);
		Member existingMember = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(existingMember);

		repository.deleteById(existingMember.getId());

		assertThat(statDeleteMember.isClosed()).isTrue();
		assertThat(statDeleteSubs.isClosed()).isTrue();
	}

	@Test
	public void testDeleteByIdWhenSqlExceptionOccursShouldCatchCloseTheStatementAndPropagateException()
			throws SQLException {
		PreparedStatement stat = connection.prepareStatement(MySqlMemberRepository.QUERY_DELETE_MEMBER);
		stat = Mockito.spy(stat);
		doThrow(new SQLException(EXCEPTION_MSG)).when(stat).executeUpdate();
		doReturn(stat).when(connection).prepareStatement(MySqlMemberRepository.QUERY_DELETE_MEMBER);
		PreparedStatement statDeleteSubs = connection.prepareStatement(MySqlMemberRepository.QUERY_DELETE_SUBS);
		doReturn(statDeleteSubs).when(connection).prepareStatement(MySqlMemberRepository.QUERY_DELETE_SUBS);
		Member existingMember = createTestMember("test-name-1", "test-surname-1", LocalDate.of(1996, 4, 30));
		insertMemberIntoDb(existingMember);

		assertThatThrownBy(() -> repository.deleteById(existingMember.getId())).isInstanceOf(SQLException.class)
				.hasMessage(EXCEPTION_MSG);
		assertThat(stat.isClosed()).isTrue();
		assertThat(statDeleteSubs.isClosed()).isTrue();
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
		PreparedStatement stat = connection.prepareStatement(INSERT_QUERY);
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

	private List<Member> readAllMembersFromDatabase() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(RETRIEVE_QUERY);
		ResultSet rs = stat.executeQuery();

		List<Member> members = new ArrayList<>();
		while (rs.next()) {
			members.add(createMemberFromResultSet(rs));
		}

		return members;
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

	private Member createMemberFromResultSet(ResultSet rs) throws SQLException {
		UUID uuid = UUID.fromString(rs.getString("uuid"));
		String name = rs.getString("name");
		String surname = rs.getString("surname");
		LocalDate dateOfBirth = rs.getObject("date_of_birth", LocalDate.class);
		Member member = new Member();
		member.setId(uuid);
		member.setName(name);
		member.setSurname(surname);
		member.setDateOfBirth(dateOfBirth);
		return member;
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
