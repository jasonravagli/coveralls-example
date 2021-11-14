package it.jasonravagli.gym.logic;

import java.util.List;
import java.util.UUID;

import it.jasonravagli.gym.model.Member;

public interface MemberRepository {

	List<Member> findAll() throws Exception;

	void save(Member member) throws Exception;

	Member findById(UUID id) throws Exception;

	void deleteById(UUID id) throws Exception;

	void update(Member member) throws Exception;

}
