package it.jasonravagli.gym.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import it.jasonravagli.gym.logic.CourseRepository;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public class MySqlCourseRepository implements CourseRepository {

	private static final String TABLE_COURSES = "courses";
	private static final String TABLE_MEMBERS = "members";
	private static final String TABLE_SUBS = "subscriptions";

	// Query fields are package-private to make them accessible from test classes
	static final String QUERY_FIND_ALL = "SELECT BIN_TO_UUID(id) as uuid, name FROM " + TABLE_COURSES
			+ " ORDER BY name";
	static final String QUERY_FIND_SUBS = "SELECT BIN_TO_UUID(id) as uuid_member, surname as surname_member, name as name_member, date_of_birth FROM "
			+ TABLE_SUBS + " as s INNER JOIN " + TABLE_MEMBERS
			+ " as m ON s.id_member = m.id WHERE BIN_TO_UUID(s.id_course) = ? ORDER BY surname";
	static final String QUERY_FIND_BY_ID = "SELECT BIN_TO_UUID(id) as uuid, name FROM " + TABLE_COURSES
			+ " WHERE BIN_TO_UUID(id) = ?";
	static final String QUERY_INSERT_COURSE = "INSERT INTO " + TABLE_COURSES + "(id, name) VALUES(UUID_TO_BIN(?), ?)";
	static final String QUERY_INSERT_SUB = "INSERT INTO " + TABLE_SUBS
			+ "(id_course, id_member) VALUES(UUID_TO_BIN(?), UUID_TO_BIN(?))";
	static final String QUERY_UPDATE_COURSE = "UPDATE " + TABLE_COURSES + " SET name=? WHERE BIN_TO_UUID(id)=?";
	static final String QUERY_DELETE_COURSE = "DELETE FROM " + TABLE_COURSES + " WHERE BIN_TO_UUID(id) = ?";
	static final String QUERY_DELETE_SUBS = "DELETE FROM " + TABLE_SUBS + " WHERE BIN_TO_UUID(id_course) = ?";

	private Connection connection;

	public MySqlCourseRepository(Connection connection) {
		this.connection = connection;
	}

	@Override
	public List<Course> findAll() throws SQLException {
		try (PreparedStatement statCourse = connection.prepareStatement(QUERY_FIND_ALL);
				PreparedStatement statSubs = connection.prepareStatement(QUERY_FIND_SUBS);) {
			ResultSet rs = statCourse.executeQuery();

			List<Course> courses = new ArrayList<>();
			while (rs.next()) {
				Course course = createCourseFromResultSet(rs, statSubs);

				courses.add(course);
			}

			return courses;
		}
	}

	@Override
	public Course findById(UUID idCourse) throws SQLException {
		try (PreparedStatement statCourse = connection.prepareStatement(QUERY_FIND_BY_ID);
				PreparedStatement statSubs = connection.prepareStatement(QUERY_FIND_SUBS);) {
			statCourse.setString(1, idCourse.toString());
			ResultSet rs = statCourse.executeQuery();

			Course course = null;
			if (rs.next()) {
				course = createCourseFromResultSet(rs, statSubs);
			}

			return course;
		}
	}

	@Override
	public void save(Course course) throws SQLException {
		try (PreparedStatement statInsertCourse = connection.prepareStatement(QUERY_INSERT_COURSE);
				PreparedStatement statInsertSub = connection.prepareStatement(QUERY_INSERT_SUB);) {
			statInsertCourse.setString(1, course.getId().toString());
			statInsertCourse.setString(2, course.getName());
			statInsertCourse.executeUpdate();

			for (Member member : course.getSubscribers()) {
				statInsertSub.setString(1, course.getId().toString());
				statInsertSub.setString(2, member.getId().toString());
				statInsertSub.executeUpdate();
			}
		}
	}

	@Override
	public void deleteById(UUID idCourse) throws SQLException {
		try (PreparedStatement statDeleteSubs = connection.prepareStatement(QUERY_DELETE_SUBS);
				PreparedStatement statDeleteCourse = connection.prepareStatement(QUERY_DELETE_COURSE);) {
			statDeleteSubs.setString(1, idCourse.toString());
			statDeleteSubs.executeUpdate();

			statDeleteCourse.setString(1, idCourse.toString());
			statDeleteCourse.executeUpdate();
		}
	}

	@Override
	public void update(Course updatedCourse) throws SQLException {
		try (PreparedStatement statUpdateCourse = connection.prepareStatement(QUERY_UPDATE_COURSE);
				PreparedStatement statDeleteSubs = connection.prepareStatement(QUERY_DELETE_SUBS);
				PreparedStatement statInsertSub = connection.prepareStatement(QUERY_INSERT_SUB);) {
			statUpdateCourse.setString(1, updatedCourse.getName());
			statUpdateCourse.setString(2, updatedCourse.getId().toString());
			statUpdateCourse.executeUpdate();

			statDeleteSubs.setString(1, updatedCourse.getId().toString());
			statDeleteSubs.executeUpdate();
			for (Member member : updatedCourse.getSubscribers()) {
				statInsertSub.setString(1, updatedCourse.getId().toString());
				statInsertSub.setString(2, member.getId().toString());
				statInsertSub.executeUpdate();
			}
		}
	}

	private Course createCourseFromResultSet(ResultSet rs, PreparedStatement statSubs) throws SQLException {
		UUID uuid = UUID.fromString(rs.getString("uuid"));
		String name = rs.getString("name");

		Course course = new Course();
		course.setId(uuid);
		course.setName(name);

		Set<Member> subscribers = new HashSet<>();
		statSubs.setString(1, course.getId().toString());
		ResultSet rsSubs = statSubs.executeQuery();
		while (rsSubs.next()) {
			UUID memberUuid = UUID.fromString(rsSubs.getString("uuid_member"));
			String memberName = rsSubs.getString("name_member");
			String memberSurname = rsSubs.getString("surname_member");
			LocalDate dateOfBirth = rsSubs.getObject("date_of_birth", LocalDate.class);

			Member member = new Member();
			member.setId(memberUuid);
			member.setName(memberName);
			member.setSurname(memberSurname);
			member.setDateOfBirth(dateOfBirth);

			subscribers.add(member);
		}
		course.setSubscribers(subscribers);
		return course;
	}

}
