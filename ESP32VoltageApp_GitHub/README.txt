ESP32 전압 측정기 Android 앱

기능
- 휴대폰에 이미 페어링된 Bluetooth Classic 기기 목록 표시
- 선택한 ESP32에 Classic Bluetooth SPP로 연결
- ESP32가 "12.65\n"처럼 한 줄씩 전송하면 "12.65 V"로 실시간 표시

ESP32 조건
- Bluetooth Classic SPP 서버로 동작해야 합니다.
- 전송 예: SerialBT.println("12.65");

사용 방법
1. Android 휴대폰 Bluetooth 설정에서 ESP32와 먼저 페어링합니다.
2. Android Studio에서 이 프로젝트를 엽니다.
3. 휴대폰에 앱을 설치합니다.
4. 앱에서 ESP32를 선택하고 "Bluetooth 연결"을 누릅니다.
5. 화면에 전압이 표시됩니다.

참고
- Android 12 이상에서는 Bluetooth 권한을 앱 최초 실행 시 요청합니다.
- BLE가 아니라 Bluetooth Classic SPP용입니다.
- 그래프/저장/로그인/알림/배터리 표시 등은 넣지 않았습니다.
