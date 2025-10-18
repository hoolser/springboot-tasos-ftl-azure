USE appdb;

CREATE TABLE course
(
   courseid int,   
   course_name varchar(1000),
   rating numeric(2,1)
);

INSERT INTO course(courseid,course_name,rating) VALUES(1, 'Docker and Kubernetes',4.5);

INSERT INTO course(courseid,course_name,rating) VALUES(2,'AZ-204 Azure Developer',4.6);

INSERT INTO course(courseid,course_name,rating) VALUES(3,'AZ-104 Administrator',4.7);
     	
