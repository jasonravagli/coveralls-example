CREATE TABLE members(
	id BINARY(16) NOT NULL PRIMARY KEY,
	name VARCHAR(50) NOT NULL,
	surname VARCHAR(50) NOT NULL,
	date_of_birth DATE NOT NULL
);

CREATE TABLE courses(
	id BINARY(16) NOT NULL PRIMARY KEY,
	name VARCHAR(50) NOT NULL
);

CREATE TABLE subscriptions(
	id_member BINARY(16) NOT NULL,
	id_course BINARY(16) NOT NULL,
	PRIMARY KEY(id_member, id_course),
	FOREIGN KEY(id_member) REFERENCES members(id),
	FOREIGN KEY(id_course) REFERENCES courses(id)
);