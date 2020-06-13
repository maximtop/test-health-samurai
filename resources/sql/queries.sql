-- :name create-patient! :! :n
-- :doc creates a new patient record
INSERT INTO patients
(full_name, sex, birthday, address, insurance_number)
VALUES (:full_name, :sex, :birthday, :address, :insurance_number)

-- :name update-patient! :! :n
-- :doc updates an existing patient record
UPDATE patients
SET full_name = :full_name, sex = :sex, birthday = :birthday, address = :address, insurance_number = :insurance_number
WHERE id = :id

-- :name get-patient :? :1
-- :doc retrieves a patient record given the id
SELECT * FROM patients
WHERE id = :id

-- :name get-patients :? :*
-- :doc retrieves all patients list
SELECT * FROM patients

-- :name delete-patient! :! :n
-- :doc deletes a patient record given the id
DELETE FROM patients
WHERE id = :id
