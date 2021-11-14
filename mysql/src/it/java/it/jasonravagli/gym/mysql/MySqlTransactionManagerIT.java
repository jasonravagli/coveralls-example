package it.jasonravagli.gym.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import it.jasonravagli.gym.logic.RepositoryProvider;
import it.jasonravagli.gym.logic.TransactionCode;
import it.jasonravagli.gym.logic.TransactionException;
import it.jasonravagli.gym.model.Member;

public class MySqlTransactionManagerIT {

	private static final String CONN_URL = "jdbc:mysql://localhost:3306/test";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "password";
	private static final String TABLE_MEMBERS = "members";
	private static final String INSERT_MEMBER_QUERY = "INSERT INTO " + TABLE_MEMBERS
			+ "(id, name, surname, date_of_birth) VALUES(UUID_TO_BIN(?),?,?,?)";
	private static final String RETRIEVE_MEMBERS_QUERY = "SELECT BIN_TO_UUID(id) as uuid, name, surname, date_of_birth FROM "
			+ TABLE_MEMBERS;
	private static final String EXCEPTION_MSG = "An error occurred";

	private AutoCloseable autoCloseable;

	@Spy
	private Connection connection;

	@Mock
	private MySqlRepositoryProvider repositoryProvider;

	private MySqlTransactionManager transactionManager;

	@Before
	public void setUp() throws Exception {
		connection = DriverManager.getConnection(CONN_URL, USERNAME, PASSWORD);
		autoCloseable = MockitoAnnotations.openMocks(this);

		// Clean the table
		connection.prepareStatement("DELETE FROM " + TABLE_MEMBERS).executeUpdate();

		transactionManager = new MySqlTransactionManager(connection, repositoryProvider);
	}

	@After
	public void tearDown() throws Exception {
		if (connection != null && !connection.isClosed())
			connection.close();
		autoCloseable.close();
	}

	@Test
	public void testDoInTransactionWhenEverythingOkShouldExecuteCode() throws SQLException {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		Boolean returnValue = true;
		TransactionCode<Boolean> code = provider -> {
			insertMemberIntoDb(member);
			return returnValue;
		};

		Boolean result = transactionManager.doInTransaction(code);

		assertThat(result).isEqualTo(returnValue);
		assertThat(readAllMembersFromDatabase()).containsExactly(member);
	}

	@Test
	public void testDoInTransactionShouldProvideRepositoryProvider() {
		TransactionCode<RepositoryProvider> code = provider -> {
			return provider;
		};

		assertThat(transactionManager.doInTransaction(code)).isEqualTo(repositoryProvider);
	}

	@Test
	public void testDoInTransactionWhenCodeThrowExceptionShouldCatchAndThrowCustomException() {
		TransactionCode<Boolean> code = provider -> {
			throw new Exception(EXCEPTION_MSG);
		};

		assertThatThrownBy(() -> transactionManager.doInTransaction(code)).isInstanceOf(TransactionException.class)
				.hasMessage(EXCEPTION_MSG);
	}

	@Test
	public void testDoInTransactionShoulEnsureAtomicity() throws SQLException {
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		TransactionCode<Boolean> code = provider -> {
			insertMemberIntoDb(member);
			throw new Exception(EXCEPTION_MSG);
			// insertMemberIntoDb(member2);
		};

		try {
			transactionManager.doInTransaction(code);
		} catch (TransactionException e) {

		}

		assertThat(readAllMembersFromDatabase()).isEmpty();
		// Ensure that the connection autoCommit property is brought back to the default
		assertThat(connection.getAutoCommit()).isTrue();
	}

	@Test
	public void testDoInTransactionWhenSqlExceptionIsThrownDuringRollbackShouldCatchAndThrowCustom()
			throws SQLException {
		String anotherExceptionMsg = "Another exception occurred";
		Member member = createTestMember("name-1", "surname-1", LocalDate.of(1996, 10, 31));
		TransactionCode<Boolean> code = provider -> {
			insertMemberIntoDb(member);
			throw new Exception(EXCEPTION_MSG);
			// insertMemberIntoDb(member2);
		};
		doThrow(new SQLException(anotherExceptionMsg)).when(connection).rollback(any());

		assertThatThrownBy(() -> transactionManager.doInTransaction(code)).isInstanceOf(TransactionException.class)
				.hasMessage(anotherExceptionMsg);
	}

	@Test
	public void testDoInTransactionExceptionWhenEverythingOkShouldCommitTranasctionAndResetAutocommit()
			throws SQLException {
		Member member = createTestMember("name", "surname", LocalDate.of(1996, 10, 31));
		Boolean returnValue = true;
		TransactionCode<Boolean> code = provider -> {
			insertMemberIntoDb(member);
			return returnValue;
		};

		transactionManager.doInTransaction(code);

		verify(connection).commit();
		assertThat(connection.getAutoCommit()).isTrue();
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

	private void insertMemberIntoDb(Member member) throws SQLException {
		PreparedStatement stat = connection.prepareStatement(INSERT_MEMBER_QUERY);
		stat.setString(1, member.getId().toString());
		stat.setString(2, member.getName());
		stat.setString(3, member.getSurname());
		stat.setObject(4, member.getDateOfBirth());
		stat.executeUpdate();
	}

	private List<Member> readAllMembersFromDatabase() throws SQLException {
		PreparedStatement stat = connection.prepareStatement(RETRIEVE_MEMBERS_QUERY);
		ResultSet rs = stat.executeQuery();

		List<Member> members = new ArrayList<>();
		while (rs.next()) {
			members.add(createMemberFromResultSet(rs));
		}

		return members;
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

}
