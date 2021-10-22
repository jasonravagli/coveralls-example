package it.jasonravagli.gym.logic;

import java.util.function.Function;

@FunctionalInterface
public interface TransactionCode<T> extends Function<RepositoryProvider, T> {
	
}
