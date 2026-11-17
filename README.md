# picon-backend


### 1. 브랜치 전략

| 브랜치 | 역할 | 규칙 |
| --- | --- | --- |
| main | 시연·배포용 | 직접 push 금지, develop에서 PR로만 병합 |
| develop | 개발 통합 | 직접 push 금지, feat에서 PR로만 병합 |
| type/이슈번호-내용 | 기능·작업 단위 | develop에서 만들어 작업 후 develop으로 PR |
- type/이슈번호-내용
    - type: feat, fix, refactor, docs, chore
    - 이름 규칙: 영문 소문자와 하이픈, 이슈 번호 포함
        - 예시
            - feat/12-request-create-api
            - fix/21-list-paging
- main 브랜치 보호 규칙
    - 삭제 제한
    - 병합 전 PR 필수
        - 1명 이상의 승인 필요
    - 강제 Push 차단
        - git push --force 명령어를 통한 강제 Push를 차단

### 2. Issue 제목, PR 제목, 커밋 메시지 형식

| type | 용도 |
| --- | --- |
| feat | 새 기능 |
| fix | 버그 수정 |
| refactor | 동작 변화 없는 코드 정리 |
| docs | 문서 수정 |
| chore | 설정, 의존성, 빌드, 배포 설정. |
- **예시**
    - feat: 의뢰 등록 API 추가
    - fix: 의뢰 목록 조회 시 페이지 번호 오류 수정


### 로컬 가짜 이미지 URL

`local` 프로필에서는 개발용 가짜 이미지 URL을 사용할 수 있습니다. 다음 두 설정이 모두 필요합니다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:S3_FAKE_IMAGE_URL = "true"
./gradlew.bat bootRun
```

가짜 모드는 `local` 프로필만 단독으로 활성화한 경우에만 사용할 수 있습니다.
가짜 모드를 요청했을 때 활성 프로필이 없거나, 다른 프로필을 사용하거나, `local`과 다른 프로필을 함께 활성화하면 애플리케이션 시작이 실패합니다.
설정값이 없거나 false이면 `local` 프로필에서도 실제 S3 Provider를 사용합니다.
실제 S3 처리에 오류가 발생해도 가짜 URL로 자동 전환하지 않습니다.

가짜 모드에서는 S3 presigner를 생성하지 않으며 AWS 자격 증명, 버킷, 리전이 필요하지 않습니다.
`https://local.invalid/` URL은 개발용 가짜 주소이므로 실제 이미지를 다운로드할 수 없습니다.
기존 DB, JWT, Kakao 설정은 여전히 필요합니다.
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `KAKAO_CLIENT_ID`,
`KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URIS`.
Redis 기본 설정은 그대로 유지됩니다. 로컬 리소스를 사용하고 운영 환경의 자격 증명은 사용하지 마세요.

배포 설정에서는 `S3_FAKE_IMAGE_URL=false`를 유지하고 `local` 프로필을 활성화하지 않아야 합니다.
운영 환경이 잘못된 설정으로 자신을 로컬 환경으로 지정한 경우, 애플리케이션만으로는 실제 운영 환경임을 판별할 수 없습니다.
