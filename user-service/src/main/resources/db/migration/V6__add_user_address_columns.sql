ALTER TABLE `users`
ADD COLUMN `address` VARCHAR(255),
ADD COLUMN `location_type` VARCHAR(50),
ADD COLUMN `latitude` DOUBLE,
ADD COLUMN `longitude` DOUBLE;