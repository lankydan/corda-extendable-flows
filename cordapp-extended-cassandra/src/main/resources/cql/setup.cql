CREATE TABLE IF NOT EXISTS messages_by_party(
  party TEXT,
  id UUID,
  content TEXT,
  committed BOOLEAN,
  PRIMARY KEY((party), id)
) WITH CLUSTERING ORDER BY (id DESC);

-- TRUNCATE TABLE messages_by_party;