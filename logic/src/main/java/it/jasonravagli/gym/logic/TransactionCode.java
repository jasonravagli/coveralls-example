package it.jasonravagli.gym.logic;

@FunctionalInterface
public interface TransactionCode<T> {

	public T apply(RepositoryProvider provider) throws Exception;

}
