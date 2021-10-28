package it.jasonravagli.gym.mongodb;

import it.jasonravagli.gym.logic.CourseRepository;
import it.jasonravagli.gym.logic.MemberRepository;
import it.jasonravagli.gym.logic.RepositoryProvider;

public class MongoRepositoryProvider implements RepositoryProvider {

	private MongoMemberRepository mongoMemberRepository;
	private MongoCourseRepository mongoCourseRepository;

	public MongoRepositoryProvider(MongoMemberRepository mongoMemberRepository,
			MongoCourseRepository mongoCourseRepository) {
		this.mongoMemberRepository = mongoMemberRepository;
		this.mongoCourseRepository = mongoCourseRepository;
	}

	@Override
	public MemberRepository getMemberRepository() {
		return mongoMemberRepository;
	}

	@Override
	public CourseRepository getCourseRepository() {
		return mongoCourseRepository;
	}

}
