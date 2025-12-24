
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(20) NOT NULL UNIQUE,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_accounts_number ON accounts(number);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    external_id UUID NOT NULL,

    sender_account_id BIGINT NOT NULL REFERENCES accounts(id),
    receiver_account_id BIGINT NOT NULL REFERENCES accounts(id),

    amount DECIMAL(19, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,

    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,

    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(255)
);

CREATE INDEX idx_transactions_sender ON transactions(sender_account_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_account_id);

CREATE UNIQUE INDEX idx_transactions_external_id ON transactions(external_id);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);