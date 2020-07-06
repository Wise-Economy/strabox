CREATE SCHEMA IF NOT EXISTS strabo;

CREATE TABLE IF NOT EXISTS strabo.users (
    id                  UUID PRIMARY KEY,
    name                VARCHAR (255) NOT NULL,
    email               VARCHAR (255) NOT NULL UNIQUE,
    dob                 DATE NOT NULL,
    phoneCountryCode    CHARACTER(5) NOT NULL,
    phoneNumber         VARCHAR (20) NOT NULL,
    residenceCountry    VARCHAR (255) NOT NULL,
    photoUrl            VARCHAR (255) NOT NULL,
    createdAt           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE strabo.auth_tokens (
    token          UUID PRIMARY KEY,
    userId         UUID NOT NULL references strabo.users (id),
    createdAt      TIMESTAMP WITH TIME ZONE NOT NULL,
    invalidatedAt  TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE strabo.accounts (
    id                UUID PRIMARY KEY,
    name              VARCHAR (255) NOT NULL,
    bankAccountId     VARCHAR (255) NOT NULL,
    currency          CHARACTER (10) NOT NULL,
    balance           DECIMAL NOT NULL,
    accountNature     VARCHAR (255) NOT NULL,
    accountHolderName VARCHAR (255) NOT NULL,
    availableMoney    DECIMAL NOT NULL,
    customerId        UUID NOT NULL REFERENCES strabo.customers (id),
    userId            UUID NOT NULL REFERENCES strabo.users (id),
    userConnectionId  UUID NOT NULL REFERENCES strabo.userConnections (id),
    createdAt         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE strabo.bankCustomers (
    id               UUID PRIMARY KEY,
    firstName        VARCHAR (255) NOT NULL,
    lastName         VARCHAR (255) NOT NULL,
    middleName       VARCHAR (255),
    email            VARCHAR (255) NOT NULL,
    phoneNumber      VARCHAR (20) NOT NULL,
    originCountry    VARCHAR (20) NOT NULL,
    accountType      VARCHAR (20) NOT NULL,
    residenceCountry VARCHAR (255) NOT NULL,
    address          VARCHAR (255) NOT NULL,
    createdAt        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE Countries (

);

CREATE TABLE strabo.userConnections (

);

CREATE TABLE strabo.transactions (
    id              UUID PRIMARY KEY,
    transactionId   VARCHAR (255) NOT NULL UNIQUE,
    status          VARCHAR (255) NOT NULL,
    currency        VARCHAR (10) NOT NULL,
    transactionAmount DECIMAL NOT NULL,
    description       VARCHAR (255),
    category          VARCHAR (255) NOT NULL,
    transactionMode   VARCHAR (255) NOT NULL
);

