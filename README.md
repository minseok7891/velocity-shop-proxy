# Velocity Shop System - Proxy

이 플러그인은 **Velocity Shop System**의 프록시 부분으로, Velocity 프록시 서버에서 실행됩니다.

## 주요 기능
- **메시지 중계**: 여러 백엔드 서버 간의 가격 동기화 메시지(`PRICE_UPDATE`)를 중계합니다.
- **채널 관리**: `shopsystem:sync` 채널을 통해 서버 간 통신을 관리합니다.

## 설치 방법
1. `VelocityShopSystem-1.0.0.jar` 파일을 Velocity 서버의 `plugins` 폴더에 넣습니다.
2. Velocity 서버를 재시작합니다.

## 설정
별도의 설정 파일(`config.yml`)이 필요하지 않습니다.
