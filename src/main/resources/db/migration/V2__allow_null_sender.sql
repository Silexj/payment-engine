ALTER TABLE transactions ALTER COLUMN sender_account_id DROP NOT NULL;
ALTER TABLE accounts ADD COLUMN currency VARCHAR(3) NOT NULL;

CREATE INDEX idx_transactions_deposit ON transactions(receiver_account_id) WHERE sender_account_id IS NULL;