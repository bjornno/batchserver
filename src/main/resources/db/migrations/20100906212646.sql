drop table if exists events;

create table events (
  key varchar(255),
  message varchar(255),
  time date
);