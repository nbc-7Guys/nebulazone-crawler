# Nebulazone Crawler

![Java](https://img.shields.io/badge/java-21-blue.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg) ![Gradle](https://img.shields.io/badge/gradle-8.8-yellow.svg)

## 📝 프로젝트 개요

**Nebulazone Crawler**는 이커머스 플랫폼(현재 다나와)에서 판매되는 다양한 상품의 정보와 사용자 리뷰를 수집하기 위해 개발된 고성능 크롤러 애플리케이션입니다. Spring Boot를 기반으로 안정적인 실행 환경을 제공하며, Playwright를 통해 JavaScript로 동적 렌더링되는 웹 페이지의 콘텐츠까지 정확하게 수집합니다.

수집된 데이터는 **Nebulazone** 서비스의 핵심 자산으로 활용됩니다. 상품 데이터는 MySQL에 정형화된 형태로 저장되어 데이터 무결성을 보장하고, 전문(Full-text) 검색 및 복잡한 데이터 분석이 가능하도록 Elasticsearch에도 함께 저장됩니다. 이를 통해 사용자에게는 정교한 상품 검색 기능을, 내부적으로는 데이터 기반의 비즈니스 인사이트를 제공하는 것을 목표로 합니다.

## ✨ 주요 기능 및 특징

- **다나와 상품 및 리뷰 수집**: 특정 카테고리의 상품 목록, 상세 스펙, 가격 정보, 사용자 리뷰 등 방대한 데이터를 수집합니다.
- **동적 웹 페이지 크롤링**: Playwright를 사용하여 SPA(Single Page Application)나 비동기적으로 로드되는 콘텐츠도 놓치지 않고 안정적으로 처리합니다.
- **자동화된 스케줄링**: Spring의 `@Scheduled`를 활용하여 매일 자정(`0 0 0 * * *`)에 자동으로 최신 정보를 수집 및 동기화합니다.
- **안티 크롤링 탐지 우회**: 크롤러를 위장하는 `stealth` 스크립트를 적용하여 차단을 회피하고 데이터 수집의 연속성을 보장합니다.
- **이중 데이터 저장소 활용**:
    - **MySQL**: 상품의 핵심 정보(카탈로그)를 관계형 모델로 저장하여 데이터의 일관성과 정합성을 유지합니다.
    - **Elasticsearch**: 수집된 모든 텍스트 데이터를 인덱싱하여 빠르고 유연한 검색 및 집계, 분석 기능을 지원합니다.
- **시스템 모니터링**: Spring Boot Actuator와 Prometheus를 연동하여 애플리케이션의 상태, 성능 메트릭 등을 실시간으로 모니터링할 수 있는 환경을 제공합니다.

## 🛠️ 기술 스택

- **Backend**: `Java 21`, `Spring Boot 3.5.0`
- **Crawling**: `Playwright`, `Jsoup`
- **Database**: `MySQL`, `Elasticsearch`
- **Build Tool**: `Gradle`
- **Core Libraries**: `Spring Data JPA`, `Spring Data Elasticsearch`, `Lombok`

## 🏛️ 아키텍처

본 크롤러는 역할과 책임에 따라 명확하게 모듈화된 구조를 가집니다.

```
.
├── src
│   ├── main
│   │   ├── java/nbc/chillguys/nzcrawler
│   │   │   ├── config          # (1) 애플리케이션 및 스케줄러 설정
│   │   │   ├── product         # (2) 상품 정보 크롤링 모듈 (Jsoup 기반)
│   │   │   │   ├── crawler     # - Jsoup을 이용한 정적 Ajax 크롤러
│   │   │   │   ├── ...
│   │   │   └── review          # (3) 리뷰 정보 크롤링 모듈 (Playwright 기반)
│   │   │       ├── crawler     # - Playwright를 이용한 동적 상호작용 크롤러
│   │   │       ├── ...
│   │   └── resources
│   │       ├── application.yml # (4) 시스템 전역 설정 파일
│   │       └── elastic         # - Elasticsearch 인덱스 매핑 정보
│   └── test                    # 테스트 코드
├── build.gradle                # 프로젝트 의존성 및 빌드 설정
└── README.md                   # 프로젝트 문서
```

1.  **Config**: Spring 스케줄러와 같이 애플리케이션의 핵심 동작 방식을 정의합니다.
2.  **Product Module (Jsoup)**: 상품 정보 수집을 담당합니다. 다나와의 내부 Ajax API를 직접 호출하여 상품 목록을 가져오는 **정적 크롤링 방식**을 사용합니다. 이 방식은 JavaScript 렌더링이 필요 없어 가볍고 빠릅니다.
3.  **Review Module (Playwright)**: 상품 리뷰 수집을 전담합니다. 실제 브라우저(Chromium)를 제어하는 **동적 크롤링 방식**을 사용합니다. 페이지의 '리뷰 탭'을 클릭하고 '다음 페이지' 버튼을 누르는 등 사용자 상호작용을 시뮬레이션하여 JavaScript로 렌더링되는 리뷰 데이터를 정확히 수집합니다.
4.  **Resources**: `application.yml`에서는 데이터베이스 접속 정보, Playwright 동작 방식, 로깅 레벨 등 시스템의 모든 외부 설정을 관리합니다.