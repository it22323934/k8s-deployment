-- Create user table
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `first_name` VARCHAR(50) NOT NULL,
    `last_name` VARCHAR(50) NOT NULL,
    `phone_number` VARCHAR(15),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `enabled` BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (`id`)
);

-- Create roles table
CREATE TABLE `roles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
);

-- Create user_roles mapping table
CREATE TABLE `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
);

-- Create customer profile table
CREATE TABLE `customer_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `default_address` VARCHAR(255),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

-- Create restaurant_admin profile table
CREATE TABLE `restaurant_admin_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `restaurant_id` BIGINT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

-- Insert default roles
INSERT INTO `roles` (`name`) VALUES
('ROLE_CUSTOMER'),
('ROLE_RESTAURANT_ADMIN'),
('ROLE_DELIVERY_PERSONNEL'),
('ROLE_ADMIN');