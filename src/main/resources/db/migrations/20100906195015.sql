drop table if exists appdata;

create table appdata (
  key varchar(255),
  record varchar(255),
  status integer default 0,
  version integer default 0
);