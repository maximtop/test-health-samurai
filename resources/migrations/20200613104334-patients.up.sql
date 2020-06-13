CREATE TABLE patients
(id SERIAL PRIMARY KEY,
 full_name VARCHAR(100),
 sex VARCHAR(10),
 birthday DATE,
 address VARCHAR(100),
 insurance_number VARCHAR(16));
