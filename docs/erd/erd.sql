CREATE TABLE `request_image` (
	`image_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`request_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`image_key`	VARCHAR(500)	NOT NULL	COMMENT 'S3 객체 키(이미지 저장 경로)',
	`image_type`	VARCHAR(20)	NOT NULL	COMMENT 'REFERENCE, TO_EDIT',
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `favorite` (
	`favorite_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`expert_member_id`	BIGINT	NOT NULL	COMMENT 'FK, UNIQUE(member_id, expert_member_id) - 동일 전문가 중복 찜 방지',
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `request_edit_item` (
	`request_edit_item_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`request_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`name`	VARCHAR(20)	NOT NULL	COMMENT 'SKIN, COLOR, FACE_TYPE, BODY_TYPE, BACKGROUND, RESTORE',
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `proposal` (
	`proposal_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`request_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK(의뢰+전문가 UK)',
	`price`	INT	NOT NULL	COMMENT '전문가가 제안한 최종 견적 금액(원)',
	`available_time`	JSON	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `contest` (
	`contest_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`title`	VARCHAR(100)	NOT NULL	COMMENT '콘테스트 제목',
	`description`	TEXT	NOT NULL	COMMENT '콘테스트 주제 및 참여 안내사항',
	`start_date`	DATE	NOT NULL	COMMENT '콘테스트 접수 시작일',
	`end_date`	DATE	NOT NULL	COMMENT '콘테스트 접수 종료일',
	`thumbnail_key`	VARCHAR(500)	NULL	COMMENT '콘테스트 대표 이미지의 S3 객체 키',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'UPCOMING'	COMMENT 'UPCOMING(예정), OPEN(접수 중), CLOSED(종료)',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `expert_specialty` (
	`expert_specialty_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`name`	VARCHAR(20)	NOT NULL	COMMENT 'SHOOTING, EDITING',
	`base_price`	INT	NOT NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `expert` (
	`member_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`career_years`	SMALLINT	NOT NULL	DEFAULT 0,
	`avg_rating`	DECIMAL(2,1)	NOT NULL	DEFAULT 0.0,
	`review_count`	INT	NOT NULL	DEFAULT 0,
	`review_summary`	TEXT	NULL,
	`review_summary_count`	INT	NOT NULL	DEFAULT 0,
	`review_summary_updated_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `member_photo_style` (
	`member_photo_style_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`photo_style_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `expert_photo_style` (
	`expert_photo_style_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`photo_style_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `expert_image` (
	`expert_image_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`image_key`	VARCHAR(500)	NOT NULL	COMMENT 'S3 객체 키(포트폴리오 이미지 저장 경로), UNIQUE',
	`specialty`	VARCHAR(20)	NOT NULL	COMMENT 'SHOOTING, EDITING',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `region` (
	`region_code`	BIGINT	NOT NULL	COMMENT 'PK',
	`first_depth`	VARCHAR(20)	NOT NULL,
	`second_depth`	VARCHAR(20)	NULL,
	`third_depth`	VARCHAR(20)	NULL
);

CREATE TABLE `request` (
	`request_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`region_code`	BIGINT	NULL	COMMENT 'FK(촬영 지역)',
	`photo_style_id`	BIGINT	NULL	COMMENT 'FK(촬영 종류)',
	`service_type`	VARCHAR(20)	NOT NULL	COMMENT 'SHOOTING, SHOOTING_EDITING, EDITING',
	`title`	VARCHAR(100)	NOT NULL,
	`preferred_date`	DATE	NULL,
	`people_count`	SMALLINT	NULL	COMMENT '촬영 의뢰 시 인원이 필요하지만, 편집 의뢰 시 NULL 가능',
	`budget`	INT	NULL	COMMENT '고객이 아직 예산을 정하지 않았을 수도 있고, 전문가의 제안을 받은 뒤 결정할 수도 있음',
	`requirement`	TEXT	NULL	COMMENT '고객이 최종 작성한 요청사항(AI 생성 초안 수정 가능)',
	`edit_intensity`	VARCHAR(10)	NULL	COMMENT 'LOW, MEDIUM, HIGH',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'REQUESTED'	COMMENT 'REQUESTED, PROPOSED, RESERVED, CANCELLED',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `reservation` (
	`reservation_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`proposal_id`	BIGINT	NOT NULL	COMMENT 'FK, UNIQUE - 제안서당 예약 최대 1건',
	`reservation_date`	DATE	NOT NULL	COMMENT '고객이 최종 확정한 촬영 날짜',
	`reservation_time`	TIME	NOT NULL	COMMENT '고객이 최종 확정한 촬영 시간',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'CONFIRMED'	COMMENT 'CONFIRMED(확정), IN_PROGRESS(진행 중), COMPLETED(완료), CANCELLED(취소)',
	`cancel_reason`	VARCHAR(20)	NULL	COMMENT 'SCHEDULE_CHANGE(일정 변경), OTHER_EXPERT(다른 작가 선택), OTHER(기타)',
	`cancel_detail`	TEXT	NULL	COMMENT '사용자가 직접 작성한 예약 취소 상세 사유',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `review` (
	`review_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`reservation_id`	BIGINT	NOT NULL	COMMENT 'FK, UNIQUE - 예약당 후기 최대 1건',
	`rating`	TINYINT	NOT NULL	COMMENT '별점 1~5점 (CHECK: rating BETWEEN 1 AND 5)',
	`review_tags`	JSON	NULL	COMMENT '후기 평가 태그 목록(JSON 배열, 복수 선택 가능)',
	`review_content`	TEXT	NULL	COMMENT '고객이 작성한 후기 내용(선택 입력)',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `contest_entry` (
	`entry_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`contest_id`	BIGINT	NOT NULL	COMMENT 'FK - 소속 콘테스트 ID',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`category`	VARCHAR(20)	NOT NULL	COMMENT 'SHOOTING, EDITING',
	`title`	VARCHAR(100)	NOT NULL,
	`image_key`	VARCHAR(500)	NOT NULL	COMMENT '콘테스트 출품 사진의 S3 객체 키',
	`caption`	VARCHAR(500)	NULL	COMMENT '회원이 작성한 출품작 설명 (선택 입력)',
	`final_rank`	TINYINT	NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '출품작 등록 일시 (서버에서 자동 기록)',
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `photo_style` (
	`photo_style_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`name`	VARCHAR(20)	NOT NULL
);

CREATE TABLE `contest_vote` (
	`vote_id`	BIGINT	NOT NULL	COMMENT '콘테스트 투표 기록 고유 식별번호',
	`entry_id`	BIGINT	NOT NULL	COMMENT 'FK, 콘테스트 출품작 고유 식별번호',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK, UNIQUE(member_id, entry_id) - 동일 출품작 중복 투표 방지',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '회원이 투표한 일시 (서버에서 자동 기록)'
);

CREATE TABLE `expert_region` (
	`expert_region_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`region_code`	BIGINT	NOT NULL	COMMENT 'FK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `member` (
	`member_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`kakao_id`	BIGINT	NULL	COMMENT 'UNIQUE',
	`nickname`	VARCHAR(20)	NULL,
	`birth`	DATE	NULL,
	`gender`	VARCHAR(10)	NULL	COMMENT 'MALE(남성), FEMALE(여성), 미입력 시 NULL',
	`phone_number`	VARCHAR(20)	NULL,
	`profile_image`	VARCHAR(500)	NULL,
	`service_type`	VARCHAR(20)	NULL	COMMENT 'SHOOTING, SHOOTING_EDITING, EDITING',
	`introduction`	VARCHAR(200)	NULL	COMMENT '작가 소개 겸용',
	`terms_agreed_at`	DATETIME	NULL	COMMENT '필수',
	`privacy_agreed_at`	DATETIME	NULL,
	`onboarded_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `request_recipient` (
	`request_recipient_id`	BIGINT	NOT NULL	COMMENT 'PK',
	`request_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`member_id`	BIGINT	NOT NULL	COMMENT 'FK',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'PENDING'	COMMENT 'PENDING(대기), RESPONDED(제안 완료), DECLINED(거절)',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

ALTER TABLE `request_image` ADD CONSTRAINT `PK_REQUEST_IMAGE` PRIMARY KEY (
	`image_id`
);

ALTER TABLE `favorite` ADD CONSTRAINT `PK_FAVORITE` PRIMARY KEY (
	`favorite_id`
);

ALTER TABLE `request_edit_item` ADD CONSTRAINT `PK_REQUEST_EDIT_ITEM` PRIMARY KEY (
	`request_edit_item_id`
);

ALTER TABLE `proposal` ADD CONSTRAINT `PK_PROPOSAL` PRIMARY KEY (
	`proposal_id`
);

ALTER TABLE `contest` ADD CONSTRAINT `PK_CONTEST` PRIMARY KEY (
	`contest_id`
);

ALTER TABLE `expert_specialty` ADD CONSTRAINT `PK_EXPERT_SPECIALTY` PRIMARY KEY (
	`expert_specialty_id`
);

ALTER TABLE `expert` ADD CONSTRAINT `PK_EXPERT` PRIMARY KEY (
	`member_id`
);

ALTER TABLE `member_photo_style` ADD CONSTRAINT `PK_MEMBER_PHOTO_STYLE` PRIMARY KEY (
	`member_photo_style_id`
);

ALTER TABLE `expert_photo_style` ADD CONSTRAINT `PK_EXPERT_PHOTO_STYLE` PRIMARY KEY (
	`expert_photo_style_id`
);

ALTER TABLE `expert_image` ADD CONSTRAINT `PK_EXPERT_IMAGE` PRIMARY KEY (
	`expert_image_id`
);

ALTER TABLE `region` ADD CONSTRAINT `PK_REGION` PRIMARY KEY (
	`region_code`
);

ALTER TABLE `request` ADD CONSTRAINT `PK_REQUEST` PRIMARY KEY (
	`request_id`
);

ALTER TABLE `reservation` ADD CONSTRAINT `PK_RESERVATION` PRIMARY KEY (
	`reservation_id`
);

ALTER TABLE `review` ADD CONSTRAINT `PK_REVIEW` PRIMARY KEY (
	`review_id`
);

ALTER TABLE `contest_entry` ADD CONSTRAINT `PK_CONTEST_ENTRY` PRIMARY KEY (
	`entry_id`
);

ALTER TABLE `photo_style` ADD CONSTRAINT `PK_PHOTO_STYLE` PRIMARY KEY (
	`photo_style_id`
);

ALTER TABLE `contest_vote` ADD CONSTRAINT `PK_CONTEST_VOTE` PRIMARY KEY (
	`vote_id`
);

ALTER TABLE `expert_region` ADD CONSTRAINT `PK_EXPERT_REGION` PRIMARY KEY (
	`expert_region_id`
);

ALTER TABLE `member` ADD CONSTRAINT `PK_MEMBER` PRIMARY KEY (
	`member_id`
);

ALTER TABLE `request_recipient` ADD CONSTRAINT `PK_REQUEST_RECIPIENT` PRIMARY KEY (
	`request_recipient_id`
);

ALTER TABLE `expert` ADD CONSTRAINT `FK_member_TO_expert_1` FOREIGN KEY (
	`member_id`
)
REFERENCES `member` (
	`member_id`
);