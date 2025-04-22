-- Create delivery_personnel profile table
CREATE TABLE `delivery_personnel_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `vehicle_type` VARCHAR(50),
    `vehicle_number` VARCHAR(20),
    `current_status` VARCHAR(20) DEFAULT 'OFFLINE',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

