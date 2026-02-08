# bidding-crawler

공공기관 입찰 공고(NURI) 데이터를 수집하여 구조화된 JSON 파일로 산출하는 백엔드 크롤러 서비스입니다.

---

## 1. 실행 방법

프로젝튼는 Docker를 통해 환경 의존성 없이 즉시 실행 가능합니다.

### 전제 조건

- Docker 및 Docker Compose 설치

### 실행 단계

1. 레포지토리 클론

```
git clone https://github.com/bowook/bidding-crawler.git
cd bidding-crawler
```

2. 컨테이너 빌드 및 실행

```
docker-compose up --build -d
```

3. 크롤링 과정 확인

```
docker logs -f bid-crawler
```

4. 크롤링 API 호출 방법

4.1. 특정 기간 지정 호출

```
curl -X GET "http://localhost:8080/api/crawl?targetSite=NURI&startDate=20240101&endDate=20240131"
```

4.2. 기본 범위(최근 한 달) 호출

```
curl -X GET "http://localhost:8080/api/crawl?targetSite=NURI"
```

5. 결과 확인
   프로젝트 루트의 `./output` 디렉토리에 `nuri_bids_YYYYMMDD_...json`파일이 생성됩니다.

---

## 2. 기술 스택 및 환경

- **Language**: Java 21 (Amazon Corretto)
- **Framework**: Spring Boot 3.x
- **Crawl Engine**: Microsoft Playwright (v1.50.0-jammy)
- **Build**: Gradle
- **Infra**: Docker, Docker Compose

---

## 3. 설계 및 주요 가정

**확장성을 고려한 설계**

단순한 크롤링을 넘어, 다양한 사이트와 환경 변화에 유연하게 대응할 수 있도록 설계했습니다.

- 다중 사이트 대응:
    - 서비스 로직이 구체적인 사이트 명칭이나 크롤링 규칙을 몰라도 처리가 가능하도록 추상화했습니다.
    - targetSite 파라미터에 따라 적절한 스캐너 로직이 매칭되는 구조를 취해, 새로운 사이트 추가 시 기존 코드 수정을 최소화합니다.

---

## 4. 한계 및 개선 아이디어

**현재 한계**

- **단일 스레드 수집**: 현재는 순차적으로 페이지를 읽어오기 때문에 대량의 데이터를 수집할 때 시간이 다소 소요될 수 있습니다.

**개선 아이디어**

- **비동기 크롤링**: 상세 페이지 수집을 병렬화함으로써 데이터 수집 속도를 개선할 수 있을 것 같습니다.

---

## 5. API 명세서 (Swagger) 확인

- Swagger UI 주소: http://localhost:8080/swagger-ui/index.html
- 주요 확인 사항:
    - `targetSite`: 필수 입력값 (예: NURI)
    - `startDate`, `endDate`: 선택 입력값 (YYYYMMDD 형식)
    - 각 파라미터의 상세 설명 및 응답 예시 확인 가능\
