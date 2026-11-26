-- Disposable MySQL 8.4 test schema. Representative columns and explicit ERD constraints.
CREATE TABLE member (
    member_id BIGINT PRIMARY KEY,
    nickname VARCHAR(20)
) ENGINE=InnoDB;

CREATE TABLE expert (
    member_id BIGINT PRIMARY KEY,
    review_count INT NOT NULL DEFAULT 0,
    avg_rating DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE=InnoDB;

CREATE TABLE `request` (
    request_id BIGINT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE=InnoDB;

CREATE TABLE proposal (
    proposal_id BIGINT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    UNIQUE (request_id, member_id),
    FOREIGN KEY (request_id) REFERENCES `request` (request_id),
    FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE=InnoDB;

CREATE TABLE reservation (
    reservation_id BIGINT PRIMARY KEY,
    proposal_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (proposal_id) REFERENCES proposal (proposal_id)
) ENGINE=InnoDB;

CREATE TABLE review (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE,
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_tags JSON,
    review_content TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id)
) ENGINE=InnoDB AUTO_INCREMENT=401;

INSERT INTO member VALUES (1, 'Requester A'), (2, 'Requester B'), (3, 'Requester C'),
                          (10, 'Expert A'), (20, 'Expert B'), (30, 'Expert C');
INSERT INTO expert (member_id) VALUES (10), (20), (30);
INSERT INTO `request` VALUES (101, 1), (102, 2), (103, 3);
INSERT INTO proposal VALUES (201, 101, 10), (202, 102, 10), (203, 103, 20);
INSERT INTO reservation VALUES (301, 201, 'CONFIRMED'), (302, 202, 'COMPLETED'), (303, 203, 'COMPLETED');
