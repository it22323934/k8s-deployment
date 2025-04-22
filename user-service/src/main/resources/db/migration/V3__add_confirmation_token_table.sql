CREATE TABLE IF NOT EXISTS confirmation_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP NULL,
  expiry_date TIMESTAMP NOT NULL,
  CONSTRAINT fk_confirmation_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_confirmation_token ON confirmation_token(token);