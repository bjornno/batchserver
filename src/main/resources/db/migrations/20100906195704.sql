drop table if exists errormessages;

create table errormessages (
  key varchar,
  message varchar,
  status varchar,
  time date
);
