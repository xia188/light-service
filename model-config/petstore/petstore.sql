drop table if exists pet;
create table pet(
  id int PRIMARY KEY AUTO_INCREMENT,
  name varchar(20) NOT NULL,
  tag varchar(20)
);
insert into
  pet(id, name, tag)
values(1, 'Peppa', 'Pig');
insert into
  pet(id, name, tag)
values(2, 'Daddy', 'Pig');
insert into
  pet(id, name, tag)
values(3, 'Mommy', 'Pig');