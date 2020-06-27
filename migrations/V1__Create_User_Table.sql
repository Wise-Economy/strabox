CREATE SCHEMA IF NOT EXISTS strabo;

CREATE TABLE IF NOT EXISTS strabo.user (
    id                      UUID PRIMARY KEY,
    name                    VARCHAR (255) NOT NULL,
    email                   VARCHAR (255) NOT NULL,
    dob                     DATE NOT NULL,
    phone_country_code      CHARACTER(5) NOT NULL,
    phone_number            VARCHAR (20) NOT NULL,
    residence_country_code  VARCHAR (255) NOT NULL,
    photo_url               VARCHAR (255) NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL
);