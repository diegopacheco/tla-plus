INSERT INTO account (id, owner, balance) VALUES (1, 'alice', 100), (2, 'bob', 50) ON CONFLICT (id) DO NOTHING;
