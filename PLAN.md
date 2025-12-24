# Compose 기반 스트리밍 플레이어 아키텍처 설계 계획 (plan.md)

## 1. 프로젝트 개요

본 프로젝트는 **스트리밍 기반 음악 플레이어의 재생 구조와 UI 상태 관리**에 집중한  
Android 포트폴리오용 데모 애플리케이션이다.

실제 서비스 수준의 콘텐츠 제공이나 사용자 인증을 구현하는 대신,  
**미디어 재생 안정성, 상태 일관성, 확장 가능한 아키텍처 설계**를 목표로 한다.

UI는 **Jetpack Compose**를 사용하며,  
재생 로직과 UI 상태를 명확히 분리하기 위해 **Clean Architecture + MVI 패턴**을 적용한다.

## 2. 아키텍처 선택 이유

### 2.1 Clean Architecture

본 프로젝트는 다음과 같은 이유로 Clean Architecture를 채택한다.

- 재생 로직, UI, 외부 데이터 소스를 명확히 분리
- 스트리밍 소스 변경(Audius → 사내 백엔드 등)에 대한 확장성 확보
- 미디어 중심 애플리케이션에서 중요한 **상태 안정성**과 **테스트 용이성** 보장

도메인 계층은 **순수 Kotlin 모듈**로 유지하며,  
Android 프레임워크에 대한 의존성을 제거한다.

### 2.2 MVI 패턴

본 프로젝트의 UI 상태 관리는 **단방향 데이터 흐름(Unidirectional Data Flow)** 을 따르는  
MVI(Model–View–Intent) 패턴을 기반으로 설계한다.

- UI는 Intent만 발행
- ViewModel은 Intent를 처리하여 State를 생성
- State는 immutable data class로 관리
- 네비게이션, 재생 명령 등은 SideEffect로 분리

이를 통해 **재생 상태와 UI 상태의 불일치 문제를 원천적으로 방지**한다.

## 3. 모듈 구성

프로젝트는 다음과 같은 모듈 구조를 따른다.

```
app
domain
core:data
core:ui
core:designsystem
feature:home
feature:album
feature:player
build-logic
```

## 4. 각 모듈의 역할 정의

### 4.1 app 모듈

- 애플리케이션 진입점
- Hilt 엔트리포인트 정의
- 전역 Navigation 그래프 관리 (Navigation3)
- feature 모듈 바인딩

app 모듈은 **비즈니스 로직을 포함하지 않는다.**

### 4.2 feature 모듈

각 feature 모듈은 **화면 및 사용자 플로우 단위**로 분리된다.  
모든 feature 모듈은 다음 요소를 포함한다.

- Compose UI
- ViewModel (MVI 기반)
- Navigation Route 정의

#### feature:home
- 앨범(또는 재생 단위) 목록 표시
- 사용자가 탐색하는 시작 화면

#### feature:album
- 하나의 앨범(또는 플레이리스트) 구성 정보 표시
- 트랙 목록 제공
- 특정 트랙 선택 시 재생 요청 전달

#### feature:player (핵심 모듈)
- 애플리케이션의 **중앙 재생 시스템**
- 재생 상태 단일 소스(Single Source of Truth)
- ExoPlayer 상태와 UI 상태 동기화
- 트랙 전환, 재생/일시정지 제어

> player feature는 단순 화면이 아니라  
> **재생 상태를 책임지는 시스템 중심 모듈**로 설계한다.

### 4.3 domain 모듈

- 순수 Kotlin 모듈
- 비즈니스 모델 및 Repository 인터페이스 정의
- Android 프레임워크 의존성 없음

#### 포함 요소
- Domain Model (Album, Track 등)
- Repository 인터페이스
- 비즈니스 규칙

### 4.4 core:data 모듈

- 외부 데이터 소스 접근 책임
- Retrofit2 + OkHttp 기반 네트워크 통신
- Repository 구현체 제공

본 모듈은 **스트리밍 소스 교체를 전제로 설계**되며,  
domain 계층과 feature 계층에는 영향을 주지 않는다.

### 4.5 core:ui 모듈

- 공용 UI 컴포넌트 정의
- 버튼, 리스트 아이템, 로딩 UI 등 재사용 가능한 Composable 포함

### 4.6 core:designsystem 모듈

- 색상, 타이포그래피, spacing 등 디자인 토큰 정의
- feature 모듈에서는 디자인 값을 직접 정의하지 않는다.

## 5. 스트리밍 전략

본 프로젝트는 **Audius Public API**를 스트리밍 소스로 사용한다.

- 별도의 서버 구현 없이 실제 스트리밍 환경을 시뮬레이션
- 재생 구조 및 UI 상태 관리에 집중
- 콘텐츠 배포가 아닌 **플레이어 아키텍처 검증**이 목적

> 모든 음원은 Audius 플랫폼을 통해 제공되며,  
> 저작권은 원 저작자에게 귀속된다.

## 6. 네비게이션 설계

- Navigation3 사용
- 네비게이션 상태를 Compose State로 관리
- MVI 패턴과의 정합성을 고려하여 **상태 기반 네비게이션** 적용

## 7. 의존성 관리

- 모든 모듈은 **build-logic 기반 커스텀 컨벤션 플러그인**을 사용하여 설정
- 모듈별 의존성 중복 제거
- Gradle 설정의 일관성 유지

## 8. 테스트 전략

본 프로젝트의 테스트는 **핵심 로직 안정성 검증**에 초점을 둔다.

- Domain 레이어: JUnit5 기반 단위 테스트
- ViewModel: Repository를 Fake 구현체로 교체한 뒤, Intent를 발행했을 때 기대하는 UI State가 생성되었는지 검증
- UI 테스트 및 Instrumentation 테스트는 범위에서 제외

## 9. Out of Scope (의도적 제외 범위)

본 프로젝트에서는 다음 항목을 구현하지 않는다.

- 사용자 인증 / 로그인
- 콘텐츠 소유권 검증
- 오프라인 다운로드
- DRM 구현
- Analytics / Logging 인프라
- 서버 사이드 개발

