# TutorialWarp (fixed)
- 블럭을 `sethere`로 등록하면 즉시 `config.yml`에 저장되고, 서버 재시작 후에도 유지됩니다.
- `trigger.mode` 가 `block`이면 해당 좌표의 블럭을 밟을 때 발동, `radius`이면 반경(블럭 단위) 안에 들어올 때 발동합니다.
- `trigger.require-sneak` 가 true면 웅크린 상태에서만 발동합니다.
- `on-trigger.actions` 에서 `title:`, `subtitle:`, `sound:`, `console:`, `player:`, `op:` 을 지원합니다.
- 쿨다운 2초(40틱)로 중복 발동 방지.

## 명령어
`/tutorial sethere|remove|list|clear|reload|test`

- `sethere`: 현재 위치의 발밑 블럭 좌표(world,x,y,z)를 트리거로 등록 (즉시 저장)
- `remove`: 가장 가까운 트리거 블럭 1개 삭제 (즉시 저장)
- `list`: 등록된 블럭 목록 출력
- `clear`: 모두 삭제 (즉시 저장)
- `reload`: 설정 리로드
- `test`: 현재 플레이어에게 `on-trigger.actions` 실행

## 퍼미션
- `tutorial.admin` (OP 기본)
- `tutorial.bypass` (OP 기본, 발동 무시)

## 빌드
Java 8, Spigot/Paper 1.16.5 API 기준.
`mvn -B -DskipTests clean package`

생성물: `target/TutorialWarp-1.0.2.jar`
