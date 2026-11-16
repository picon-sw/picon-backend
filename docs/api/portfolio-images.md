# 탐색 메인 화면 조회 API

> PIC:ON Backend API Specification — 2차 초안
> **상태: 구현 전 검토 필요**

## 1. 기본 정보

| 항목 | 내용 |
|---|---|
| 기능명 | 탐색 메인 화면 조회 |
| Method | GET |
| Endpoint | `/api/portfolio-images` |
| 그룹 | 탐색 |
| 인증 필요 여부 | 인증 필요 |
| HTTP Success | 200 OK |

### 기능 목적

PIC:ON 탐색 메인 화면에서 전문가들이 등록한 포트폴리오 이미지를 조회한다.

프론트엔드는 촬영 및 편집 탭을 제공하며, 사용자는 포트폴리오 이미지를 탐색하고 해당 전문가의 상세 페이지로 이동할 수 있다.

이미지 데이터는 `expert_image` 테이블을 기준으로 조회하며, 전문가 닉네임은 `member` 테이블을 통해 조회한다.

---

## 2. Request

### 2.1 Query Parameter

| Parameter | Type | 필수 | 설명 |
|---|---|---|---|
| `specialty` | String | 선택 | 작업 분야 (`SHOOTING`, `EDITING`) |
| `tag` | String | 선택 | 추천 태그 또는 사진 스타일 필터 |
| `page` | Integer | 선택 | 페이지 번호 (0부터 시작) |
| `size` | Integer | 선택 | 페이지당 조회할 이미지 수 |

### 2.2 Path Variable

없음.

### 2.3 Request Body

없음.

### 2.4 인증

`Authorization: Bearer {accessToken}`

JWT 인증이 필요한 API로 정의되어 있다.

### 2.5 요청 예시

`GET /api/portfolio-images?specialty=SHOOTING&page=0&size=10`

### 2.6 요청 처리 설명

- 전문가들이 등록한 포트폴리오 이미지를 조회한다.
- `specialty`를 이용하여 촬영 및 편집 이미지를 구분한다.
- `page`, `size`를 이용하여 페이지 단위로 조회한다.
- `tag`는 추천 태그 필터링을 위한 제안 파라미터이며, 실제 필터링 구현 방식은 미확정이다.
- 필터를 지정하지 않았을 때의 기본 노출 대상과 정렬 기준은 아직 확정되지 않았다.
- `page`, `size`의 기본값과 최대 허용값은 백엔드 협의가 필요하다.

---

## 3. Response

### 3.1 성공 Response Body 예시

```json
{
  "content": [
    {
      "imageId": 101,
      "expertId": 1,
      "expertNickname": "서하",
      "imageUrl": "https://example.com/portfolio1.jpg",
      "specialty": "SHOOTING"
    },
    {
      "imageId": 102,
      "expertId": 2,
      "expertNickname": "은슬",
      "imageUrl": "https://example.com/portfolio2.jpg",
      "specialty": "SHOOTING"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 24,
  "totalPages": 3,
  "last": false
}
```

위 데이터는 명세서의 예시이며, 실제 DB 데이터나 이미지 URL을 의미하지 않는다.

공통 성공 응답의 래핑 구조는 기존 프로젝트의 `ApiResponse` 구현을 확인한 후 적용한다.

### 3.2 Response 필드 설명

| 필드 | 타입 | 설명 |
|---|---|---|
| `content` | Array | 탐색 화면에 표시할 포트폴리오 이미지 목록 |
| `imageId` | Long | 전문가 포트폴리오 이미지 고유 ID |
| `expertId` | Long | 이미지 소유 전문가의 회원 ID |
| `expertNickname` | String | 전문가 닉네임 |
| `imageUrl` | String | 포트폴리오 이미지 조회 URL |
| `specialty` | String | 작업 분야 (`SHOOTING`, `EDITING`) |
| `page` | Integer | 현재 페이지 번호 |
| `size` | Integer | 페이지당 조회 수 |
| `totalElements` | Long | 전체 조회 대상 이미지 수 |
| `totalPages` | Integer | 전체 페이지 수 |
| `last` | Boolean | 마지막 페이지 여부 |

### 3.3 ERD 매핑

| API 필드 | ERD 근거 |
|---|---|
| `imageId` | `expert_image.expert_image_id` |
| `expertId` | `expert_image.member_id` |
| `expertNickname` | `member.nickname` |
| `imageUrl` | `expert_image.image_key` 기반 조회 URL |
| `specialty` | `expert_image.specialty` |

### 3.4 Response 관련 참고사항

- `portfolioId`는 현재 ERD에 없어 사용하지 않는다.
- `workType` 대신 실제 DB 컬럼에 대응하는 `specialty`를 사용한다.
- 이미지별 `tags`는 현재 ERD에 직접 대응하는 구조가 없어 Response에서 제외한다.
- `imageUrl`은 S3 객체 키를 조회 가능한 URL로 변환한다는 가정이며, 구체적인 URL 생성 방식은 미확정이다.
- 프론트엔드는 `expertId`를 이용하여 전문가 상세 화면으로 이동할 수 있다.
- 조회 결과가 없을 경우 `200 OK`와 빈 `content` 배열을 반환하는 방식으로 제안한다.
- 페이지네이션 응답 구조는 다른 목록 조회 API들과 통일한다는 제안이다.

---

## 4. ERD 관련 정보

### 4.1 expert_image 테이블

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `expert_image_id` | BIGINT | 전문가 포트폴리오 이미지 ID |
| `member_id` | BIGINT | 이미지 소유 전문가의 회원 ID |
| `image_key` | VARCHAR(500) | S3 객체 키 |
| `specialty` | VARCHAR(20) | `SHOOTING`, `EDITING` |
| `created_at` | DATETIME | 등록 일시 |
| `updated_at` | DATETIME | 수정 일시 |

### 4.2 추가 테이블 참조

- `member`: 전문가 닉네임 조회
- `photo_style`: 사진 스타일 정보 관리
- `expert_photo_style`: 전문가와 사진 스타일 연결

단, `expert_photo_style`은 전문가와 사진 스타일의 관계를 나타내며, 개별 `expert_image`에 직접 태그를 부여하는 테이블이 아니다.

따라서 이미지 단위 태그 필터링을 구현할 수 있는지 별도 검토가 필요하다.

**주의:** 이 문서의 ERD 정보는 제공된 API 명세서를 기준으로 정리한 것이다. 실제 구현 전 전체 ERD SQL과 기존 Entity를 확인해야 한다.

---

## 5. Status

| HTTP Status | Code | 설명 |
|---|---|---|
| 200 | 성공 | 탐색 메인 이미지 목록 조회 성공 |
| 400 | `INVALID_REQUEST` | 조회 조건 또는 페이지 정보가 유효하지 않음 |
| 401 | `UNAUTHORIZED` | 인증 정보가 없거나 유효하지 않음 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

오류 코드 명칭 및 반환 조건은 현재 명세서의 제안안이며, 프로젝트의 실제 `ErrorCode` 정의와 비교 후 확정한다.

### 5.1 실패 Response 예시

```json
{
  "isSuccess": false,
  "code": "INVALID_REQUEST",
  "message": "유효하지 않은 탐색 조회 조건입니다."
}
```

공통 실패 응답은 `isSuccess`, `code`, `message`를 사용하는 형태로 작성되어 있다.

실제 구현에서는 기존 공통 응답 및 예외 처리 구조를 우선 적용한다.

---

## 6. 미확정 사항 및 백엔드 검토 항목

1. **추천 태그 필터링**
    - `tag` 파라미터를 어떤 테이블과 연결할지 확인
    - 전문가 단위 스타일 필터인지 이미지 단위 태그 필터인지 확인
    - 현재 ERD에 없는 이미지별 태그 구조를 임의로 추가하지 않음

2. **이미지 노출 순서**
    - 최신순 또는 별도 정렬 기준 확정 필요
    - 필터 미지정 시 기본 노출 범위 확인

3. **페이지네이션**
    - 기본 페이지 크기 및 최대 크기 확인
    - 기존 프로젝트의 페이지네이션 패턴 재사용

4. **Response 데이터 범위**
    - 이미지 URL, 전문가 ID, 닉네임 등 반환 필드 확인
    - 프론트엔드에서 추가로 필요한 데이터가 있는지 확인

5. **S3 이미지 URL 변환**
    - `image_key`를 URL로 변환하는 방식 확인
    - 기존 프로젝트에 이미지 URL 생성 기능이 있는지 확인

6. **검색 및 필터 API 분리 여부**
    - 탐색 메인 API에서 검색과 필터를 모두 처리할지 확인
    - 별도 전문가 검색 API와의 역할 구분 필요

7. **공통 응답 및 Status**
    - `ApiResponse`의 성공 응답 구조 확인
    - `SuccessCode`, `ErrorCode` 및 예외 처리 규칙 확인

---

## 7. Codex 구현 시 주의사항

이 문서는 **2차 API 명세 초안**이며 최종 확정된 구현 계약이 아니다.

Codex는 다음 원칙에 따라 작업한다.

- 기존 프로젝트의 Entity, Repository, Service, Controller 구조를 먼저 분석한다.
- 기존 `ApiResponse`, 예외 처리 및 Security 설정을 재사용한다.
- 이 문서의 필드와 실제 ERD·Entity 사이에 차이가 있다면 먼저 보고한다.
- 명세서의 미확정 사항을 임의로 결정하지 않는다.
- 기존 팀원이 작성한 코드를 불필요하게 변경하지 않는다.
- 실제 S3 및 JWT 연동 상태를 확인한다.
- `README.md`의 브랜치 전략과 협업 규칙을 준수한다.
- 사용자 승인 전 코드 수정, 브랜치 생성, Commit, Push, PR 생성을 하지 않는다.

### 최초 분석 요청

현재 프로젝트의 코드를 먼저 분석하고 다음 결과를 보고한다.

1. 해당 API 구현에 재사용할 수 있는 코드
2. 새로 작성해야 하는 파일과 역할
3. ERD 및 API 명세와 기존 코드의 불일치 사항
4. 인증 및 이미지 URL 처리 방식
5. 구현 순서
6. 실제 구현 전에 결정해야 할 정책

**분석 결과에 대한 검토가 끝난 뒤 구현을 진행한다.**