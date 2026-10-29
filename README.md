# 청소년상담사 3급 CBT

청소년상담사 3급 기출문제를 과목 단위로 풀고 채점할 수 있는 정적 웹 앱입니다. 설치나 로그인 없이 브라우저에서 바로 실행되며, 같은 문제를 Android 네이티브 앱으로도 풀 수 있습니다.

## 기능

- 2017년부터 2025년까지 18개 교시, 1,575문항 수록
- 연도 · 교시 · 과목별 25문항 단위 풀이
- 필수 5과목과 선택 1과목을 합친 150문항 모의고사
- 문항표(OMR)에서 답안 위치 확인 및 이동
- 제출 후 점수, 문항별 정오답, 정답 보기, 해설 확인
- 데스크톱과 모바일 브라우저 지원

## 실행

정적 파일을 로컬 서버로 실행합니다.

```powershell
python -m http.server 8000
```

브라우저에서 `http://localhost:8000`을 엽니다. 배포용 파일을 만들려면 다음을 실행합니다.

```powershell
node scripts/build.mjs
```

`dist` 폴더가 배포 대상입니다. Netlify를 사용하면 `netlify.toml`에 정의된 빌드 설정으로 바로 배포할 수 있습니다.

## 구조

| 경로 | 설명 |
| --- | --- |
| `index.html` | 앱 진입점 |
| `app.js` | 화면 전환, 풀이, 채점 로직 |
| `styles.css` | 레이아웃과 스타일 |
| `data/catalog.js` | 시험 회차와 교시 목록 |
| `data/exams/` | 회차별 문항, 정답, 해설 JSON |
| `android/` | Android 네이티브 앱 |
| `scripts/build.mjs` | `dist` 생성 스크립트 |

문항 데이터는 다음 형태입니다.

```json
{
  "number": 1,
  "question": "문제 지문",
  "options": ["보기 1", "보기 2", "보기 3", "보기 4", "보기 5"],
  "answers": [2],
  "explanation": "해설",
  "image": "문항 이미지 주소 또는 빈 문자열"
}
```

`answers`는 1부터 5까지의 정답 번호 배열이며, 복수 정답 문항은 여러 값을 가집니다. 일부 문항 이미지는 외부 이미지 호스팅에서 불러옵니다.

## Android 앱

`android` 폴더를 Android Studio에서 열고 빌드합니다. JDK 17 이상과 Android SDK 35가 필요합니다.

```powershell
cd android
.\gradlew.bat assembleDebug lintDebug
```

산출물은 `android/app/build/outputs/apk/debug/app-debug.apk`입니다. 기출 문제는 인터넷 연결 없이 풀 수 있습니다.

## 데이터 범위

2017~2025년 3급 기출을 수록했으며, 2021년은 A형만 포함합니다. 원문에 그림이 필요한 문항은 이미지로 함께 제공하고, 이미지가 없는 문항은 지문과 보기만으로 풀 수 있게 정리했습니다.

## 라이선스

코드는 자유롭게 참고할 수 있습니다. 기출 문제와 정답, 해설의 권리는 원저작자에게 있으며 이 저장소는 개인 학습용 정리 자료입니다.
