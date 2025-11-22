# TutorialWarp 1.21.1 (once-per-player + reset + test)
- 블럭 등록은 즉시 `config.yml`에 저장 → 재시작 후에도 유지
- **1회만 발동**(once-per-player) 지원 + **리셋/상태/강제완료** 명령어 추가
- 트리거 모드: `block`(좌표 일치) / `radius`(반경)
- 웅크리기 필요 옵션, 2초 쿨다운, 다양한 액션 파서(title/subtitle/sound/console/player/op)

## 설정
`tutorial.once-per-player: true` 면 1회만 발동하며 완료자는 다시 밟아도 작동하지 않습니다. 완료 상태는 `plugins/TutorialWarp/data.yml`에 저장됩니다.

## 명령어
`/tutorial sethere|remove|list|clear|reload|test|reset <player|*>|status <player>|complete <player>`

- `sethere` : 현재 블럭을 트리거로 등록(즉시 저장)
- `remove`  : 가장 가까운 트리거 1개 삭제
- `list`    : 목록 보기
- `clear`   : 전부 삭제
- `reload`  : 설정 리로드
- `test`    : **본인에게** 액션을 강제로 실행(once-per-player 무시)
- `reset <player>` : 해당 플레이어 완료 상태 초기화
- `reset *` : **전체 초기화**
- `status <player>` : 완료/미완료 상태 확인
- `complete <player>` : 대상 플레이어에게 완료 액션 강제 실행

## 빌드
Java 11 + Spigot/Paper 1.21.1.  
`mvn -B -DskipTests clean package` → `target/TutorialWarp-1.21.1.jar`
