package it.jasonravagli.gym.mongodb;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import it.jasonravagli.gym.logic.MemberRepository;
import it.jasonravagli.gym.model.Member;

public class MongoMemberRepository implements MemberRepository {

	private ClientSession clientSession;
	private MongoCollection<Document> memberCollection;
	private MongoCollection<Document> courseCollection;

	public MongoMemberRepository(MongoCollection<Document> memberCollection, MongoCollection<Document> courseCollection,
			ClientSession clientSession) {
		this.memberCollection = memberCollection;
		this.courseCollection = courseCollection;
		this.clientSession = clientSession;
	}

	@Override
	public List<Member> findAll() {
		return StreamSupport.stream(memberCollection.find(clientSession).spliterator(), false)
				.map(this::documentToMember).collect(Collectors.toList());
	}

	@Override
	public void save(Member member) {
		memberCollection.insertOne(clientSession, memberToDocument(member));
	}

	@Override
	public Member findById(UUID id) {
		Document doc = memberCollection.find(clientSession, Filters.eq("id", id)).first();

		if (doc == null)
			return null;

		return documentToMember(doc);
	}

	@Override
	public void deleteById(UUID id) {
		memberCollection.deleteOne(clientSession, Filters.eq("id", id));
		FindIterable<Document> iterableCourses = courseCollection.find(clientSession, Filters.eq("subscribers.id", id));
		for (Document doc : iterableCourses) {
			List<Document> subs = doc.getList("subscribers", Document.class);
			subs.removeIf(s -> id.equals(s.get("id", UUID.class)));
			doc.replace("subscribers", subs);
			courseCollection.replaceOne(clientSession, Filters.eq("id", doc.get("id")), doc);
		}
	}

	@Override
	public void update(Member member) {
		memberCollection.replaceOne(clientSession, Filters.eq("id", member.getId()), memberToDocument(member));
	}

	private Member documentToMember(Document doc) {
		Member member = new Member();
		member.setId((UUID) doc.get("id"));
		member.setName(doc.getString("name"));
		member.setSurname(doc.getString("surname"));
		member.setDateOfBirth(LocalDate.parse(doc.getString("dateOfBirth")));

		return member;
	}

	private Document memberToDocument(Member member) {
		return new Document().append("id", member.getId()).append("name", member.getName())
				.append("surname", member.getSurname()).append("dateOfBirth", member.getDateOfBirth().toString());
	}

}
