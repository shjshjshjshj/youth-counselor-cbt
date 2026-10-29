# 청소년상담사 CBT Android

Java와 Android 기본 UI로 만든 네이티브 앱입니다. WebView를 사용하지 않습니다.

- Android 8.0 이상에서 실행됩니다.
- 2017·2018·2019·2020·2021·2022·2023·2024·2025년 기출 1,575문항을 포함합니다.
- 과목별 풀이, 선택과목을 포함한 150문항 모의고사, 채점 및 문항별 답안 확인을 지원합니다.
- 기출 문제는 인터넷 연결 없이 풀 수 있습니다.

## 빌드

Android Studio에서 이 `android` 폴더를 엽니다. JDK 17 이상, Android SDK 35가 필요합니다.

```powershell
.\gradlew.bat assembleDebug lintDebug
```

산출물: `app/build/outputs/apk/debug/app-debug.apk`. 디버그 서명 APK이므로 직접 설치용으로 사용합니다.

## 검증

`assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`로 빌드와 정적 검사를 수행합니다. `connectedDebugAndroidTest`는 연결된 Android 기기 또는 실행 중인 에뮬레이터가 필요합니다. 계측 테스트는 기출 정답 정규화, 문항 선택, 결과 화면, 150문항 구성을 확인합니다.

## 데이터

문항과 정답은 `app/src/main/assets/exams.json`에 들어 있습니다. 2021년은 3급 A형만 사용합니다. 원문 이미지는 문항에 포함된 주소에서 불러옵니다.

1.2.2: 채점 결과의 정답 보기에 연두색 배경, 초록색 테두리, 굵은 검정 글씨와 앞쪽 정답 표시를 적용했습니다.
