
CREATE TABLE APP_USER (
    username VARCHAR(255) NOT NULL primary key ,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE USER_WALLET (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL ,
    currency VARCHAR(10) NOT NULL,
    balance DECIMAL(18, 8) NOT NULL,
    UNIQUE (username)
);

ALTER TABLE USER_WALLET ADD CONSTRAINT fk_user_wallet_user FOREIGN KEY (username) REFERENCES APP_USER (username) ON DELETE CASCADE;

CREATE TABLE CRYPTO_PAIR (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    base_currency VARCHAR(10) NOT NULL,
    quote_currency VARCHAR(10) NOT NULL,
    UNIQUE (base_currency, quote_currency)
);

CREATE TABLE TRADE_TRANSACTION (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username nvarchar(255) NOT NULL,
    symbol VARCHAR(50) NOT NULL,
    trade_type VARCHAR(10) NOT NULL, -- BUY/SELL
    quantity DECIMAL(18, 8) NOT NULL,
    price DECIMAL(18, 8) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (username) REFERENCES APP_USER (username) ON DELETE CASCADE
);

INSERT INTO APP_USER (username, password)
VALUES ('user123', 'hashed_password');

INSERT INTO USER_WALLET (username, currency, balance)
VALUES ('user123', 'USDT', 50000.00);

INSERT INTO CRYPTO_PAIR (base_currency, quote_currency)
VALUES ('ETH', 'USDT'), ('BTC', 'USDT');


CREATE TABLE BEST_PRICING (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(50) NOT NULL,
  bid_price DECIMAL(18,8) NOT NULL,
  ask_price DECIMAL(18,8) NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);







