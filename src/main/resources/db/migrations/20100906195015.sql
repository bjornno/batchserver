drop table if exists appdata;

create table appdata (
  key varchar,
  record varchar,
  status integer default 0
)