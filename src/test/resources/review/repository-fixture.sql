-- Minimal test schema. The FK, UNIQUE and CHECK rules model the local ERD comments.
-- These H2 constraints do not establish which constraints are deployed in MySQL.
CREATE TABLE expert (
    member_id BIGINT PRIMARY KEY
);

CREATE TABLE `request` (
    request_id BIGINT PRIMARY KEY,
    member_id BIGINT NOT NULL
);

CREATE TABLE proposal (
    proposal_id BIGINT PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES `request`(request_id),
    member_id BIGINT NOT NULL,
    UNIQUE (request_id, member_id)
);

CREATE TABLE reservation (
    reservation_id BIGINT PRIMARY KEY,
    proposal_id BIGINT NOT NULL UNIQUE REFERENCES proposal(proposal_id),
    status VARCHAR(20) NOT NULL
);

CREATE TABLE review (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE REFERENCES reservation(reservation_id),
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_tags JSON,
    review_content TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

ALTER TABLE review ALTER COLUMN review_id RESTART WITH 401;

INSERT INTO expert (member_id) VALUES (10), (20), (30);
INSERT INTO `request` (request_id, member_id) VALUES (101, 1), (102, 2), (103, 3);
INSERT INTO proposal (proposal_id, request_id, member_id) VALUES (201, 101, 10), (202, 102, 10), (203, 103, 20);
INSERT INTO reservation (reservation_id, proposal_id, status)
VALUES (301, 201, 'CONFIRMED'), (302, 202, 'COMPLETED'), (303, 203, 'COMPLETED');

-- A reservation chain whose proposal member has no expert row.
INSERT INTO `request` (request_id, member_id) VALUES (104, 4);
INSERT INTO proposal (proposal_id, request_id, member_id) VALUES (204, 104, 40);
INSERT INTO reservation (reservation_id, proposal_id, status) VALUES (304, 204, 'COMPLETED');
