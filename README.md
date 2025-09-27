# Tuto (Java 8 / 1.16.5) — Location-based triggers

- First join -> Essentials warp
- Finish by stepping on **configured coordinates** (exact block match by default)
- Optional radius per trigger
- One-time / cooldown / sneak requirement
- Per-trigger or global command list

## Build
```
cd tuto
mvn -q -DskipTests clean package
```


## New Admin Commands
- `/tuto settrigger [radius]` : 현재 위치(발 블럭)를 트리거로 추가합니다. 기본 radius=0.0
- `/tuto cleartriggers` : 모든 트리거 삭제
- `/tuto listtriggers` : 등록된 트리거 목록 표시


## 변경 사항 (v1.0.6)
- 서버 종료 시 `finish.triggers`를 자동 저장하도록 수정 → 재부팅 후에도 `/tuto settrigger`로 추가한 위치가 유지됩니다.
