CREATE TABLE IF NOT EXISTS password_reset_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  expiry_date TIMESTAMP NOT NULL,
  CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_token ON password_reset_token(token);