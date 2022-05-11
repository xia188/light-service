drop table if exists pet;
create table pet(
  id int PRIMARY KEY,
  name varchar(20) NOT NULL,
  tag varchar(20)
);
insert into
  pet(id, name, tag)
values(1, 'Peppa', 'Pig');