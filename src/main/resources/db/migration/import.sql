INSERT INTO APP_USER (username, password)
VALUES ('user123', 'hashed_password');

INSERT INTO USER_WALLET (username, currency, balance)
VALUES ('user123', 'USDT', 50000.00);

INSERT INTO CRYPTO_PAIR (base_currency, quote_currency)
VALUES ('ETH', 'USDT'), ('BTC', 'USDT');