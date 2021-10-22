package it.jasonravagli.gym.logic;

import java.util.List;
import java.util.UUID;

import it.jasonravagli.gym.model.Member;

public interface MemberRepository {

	List<Member> findAll();

	void save(Member member);

	Member findById(UUID id);

	void deleteById(UUID id);

	void update(Member member);

}
