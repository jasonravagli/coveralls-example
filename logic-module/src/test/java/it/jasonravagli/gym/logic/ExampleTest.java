package it.jasonravagli.gym.logic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ExampleTest {

	@Test
	public void testWitAdd() {
		Example ex = new Example();
		int value = ex.example(true);
		assertThat(value).isEqualTo(1);
	}
	
	@Test
	public void testWithoutAdd() {
		Example ex = new Example();
		int value = ex.example(false);
		assertThat(value).isEqualTo(0);
	}

}
