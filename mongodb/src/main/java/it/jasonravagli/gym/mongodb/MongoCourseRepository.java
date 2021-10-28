package it.jasonravagli.gym.mongodb;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import it.jasonravagli.gym.logic.CourseRepository;
import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public class MongoCourseRepository implements CourseRepository {

	private MongoCollection<Document> courseCollection;
	private ClientSession clientSession;

	public MongoCourseRepository(MongoCollection<Document> courseCollection, ClientSession clientSession) {
		this.courseCollection = courseCollection;
		this.clientSession = clientSession;
	}

	@Override
	public List<Course> findAll() {
		return StreamSupport.stream(courseCollection.find(clientSession).spliterator(), false)
				.map(this::documentToCourse).collect(Collectors.toList());
	}

	@Override
	public Course findById(UUID idCourse) {
		Document doc = courseCollection.find(clientSession, Filters.eq("id", idCourse)).first();

		if (doc == null)
			return null;

		return documentToCourse(doc);
	}

	@Override
	public void save(Course course) {
		courseCollection.insertOne(clientSession, courseToDocument(course));
	}

	@Override
	public void deleteById(UUID idCourse) {
		courseCollection.deleteOne(clientSession, Filters.eq("id", idCourse));
	}

	@Override
	public void update(Course updatedCourse) {
		courseCollection.replaceOne(clientSession, Filters.eq("id", updatedCourse.getId()),
				courseToDocument(updatedCourse));
	}

	private Course documentToCourse(Document doc) {
		Course course = new Course();
		course.setId((UUID) doc.get("id"));
		course.setName(doc.getString("name"));
		List<Document> listMemberDocs = doc.getList("subscribers", Document.class);
		course.setSubscribers(listMemberDocs.stream().map(this::documentToMember).collect(Collectors.toSet()));

		return course;
	}

	private Member documentToMember(Document doc) {
		Member member = new Member();
		member.setId((UUID) doc.get("id"));
		member.setName(doc.getString("name"));
		member.setSurname(doc.getString("surname"));
		member.setDateOfBirth(LocalDate.parse(doc.getString("dateOfBirth")));

		return member;
	}

	private Document courseToDocument(Course course) {
		return new Document().append("id", course.getId()).append("name", course.getName()).append("subscribers",
				course.getSubscribers().stream().map(this::memberToDocument).collect(Collectors.toList()));
	}

	private Document memberToDocument(Member member) {
		return new Document().append("id", member.getId()).append("name", member.getName())
				.append("surname", member.getSurname()).append("dateOfBirth", member.getDateOfBirth().toString());
	}

}
