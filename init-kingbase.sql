CREATE USER cloudbrain WITH PASSWORD 'cloudbrain_dev';
CREATE DATABASE cloudbrain_medical OWNER cloudbrain ENCODING 'UTF8';
GRANT ALL PRIVILEGES ON DATABASE cloudbrain_medical TO cloudbrain;
