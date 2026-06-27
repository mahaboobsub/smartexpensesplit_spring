CREATE TABLE IF NOT EXISTS users (
  user_id   INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email     VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `groups` (
  group_id    INT AUTO_INCREMENT PRIMARY KEY,
  group_name  VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  created_by  INT NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_members (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  group_id  INT NOT NULL,
  user_id   INT NOT NULL,
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_group_user (group_id, user_id),
  FOREIGN KEY (group_id) REFERENCES `groups`(group_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)  REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS expenses (
  expense_id  INT AUTO_INCREMENT PRIMARY KEY,
  group_id    INT NOT NULL,
  paid_by     INT NOT NULL,
  description VARCHAR(200) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  expense_date DATE NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (group_id) REFERENCES `groups`(group_id) ON DELETE CASCADE,
  FOREIGN KEY (paid_by)  REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS expense_splits (
  split_id   INT AUTO_INCREMENT PRIMARY KEY,
  expense_id INT NOT NULL,
  user_id    INT NOT NULL,
  share      DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (expense_id) REFERENCES expenses(expense_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)    REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS settlements (
  settlement_id INT AUTO_INCREMENT PRIMARY KEY,
  group_id      INT NOT NULL,
  paid_by       INT NOT NULL,
  paid_to       INT NOT NULL,
  amount        DECIMAL(10,2) NOT NULL,
  note          VARCHAR(200),
  settled_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (group_id) REFERENCES `groups`(group_id) ON DELETE CASCADE,
  FOREIGN KEY (paid_by)  REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (paid_to)  REFERENCES users(user_id) ON DELETE CASCADE
);
