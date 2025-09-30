# TutorialFinishBlock 1.3.0 (Java 11 / 1.16.5)

## 핵심 기능
- 첫 접속시 튜토리얼 워프로 이동 또는 커맨드 실행
- `/튜토리얼 종료블럭` 후 블럭 우클릭으로 종료블럭 등록
- 플레이어가 종료블럭(발밑 블럭) 밟으면 설정된 액션 실행
- `radius` 모드 지원 (블럭 중심으로 반경 N m)
- 완료 상태/종료블럭 `data.yml`/`config.yml`에 **영구 저장**
- `/튜토리얼 리로드`로 config/data 즉시 반영

## 빌드
```bash
mvn -q -DskipTests clean package
# target/TutorialFinishBlock-1.3.0.jar
```

## 설치 팁
- EssentialsX 사용 시 `tutorial.on-first-join.warp: tutorial` 로 설정
- 종료 액션은 `finish.actions` 목록에서 순서대로 실행됩니다.
