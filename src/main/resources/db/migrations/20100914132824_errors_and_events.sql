--drop table if exists errormessages;

create table errormessages (
  key varchar(255),
  message varchar(255),
  status varchar(255),
  time date
);

--drop table if exists events;

create table events (
  key varchar(255),
  message varchar(255),
  time date
);