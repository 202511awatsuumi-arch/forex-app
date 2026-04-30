CREATE TABLE IF NOT EXISTS exchange_rate (
    id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(15, 6) NOT NULL,
    fetched_date DATE NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_rate_per_day
        UNIQUE (base_currency, target_currency, fetched_date)
);

CREATE TABLE IF NOT EXISTS alert_setting (
    id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    threshold_rate DECIMAL(15, 6) NOT NULL,
    alert_type VARCHAR(10) NOT NULL,
    triggered BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);