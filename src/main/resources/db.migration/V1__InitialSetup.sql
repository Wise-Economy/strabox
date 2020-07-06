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
    id              UUID PRIMARY KEY,
    name            VARCHAR (255) NOT NULL,
    bankAccountId   VARCHAR (255)
);