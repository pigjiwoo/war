@echo off
chcp 65001 >nul
echo ================================
echo 국가 전쟁 플러그인 빌드
echo ================================
echo.

REM 빌드 시작
echo [1/3] 이전 빌드 정리 중...
call gradlew clean

if %errorlevel% neq 0 (
    echo.
    echo ❌ 정리 실패!
    pause
    exit /b %errorlevel%
)

echo [2/3] 플러그인 빌드 중...
call gradlew shadowJar

if %errorlevel% neq 0 (
    echo.
    echo ❌ 빌드 실패!
    echo.
    echo 다음을 확인하세요:
    echo - Java 17 이상 설치되어 있나요? (java -version)
    echo - 인터넷 연결이 되어 있나요?
    pause
    exit /b %errorlevel%
)

echo [3/3] 빌드 완료 확인 중...
if exist "build\libs\NationWarPlugin-1.0.jar" (
    echo.
    echo ✅ 빌드 성공!
    echo.
    echo 📦 빌드된 파일: build\libs\NationWarPlugin-1.0.jar
    echo.
    echo 다음 단계:
    echo 1. build\libs\NationWarPlugin-1.0.jar 파일을
    echo 2. 마인크래프트 서버의 plugins 폴더에 복사
    echo 3. 서버 재시작
    echo.
) else (
    echo.
    echo ❌ JAR 파일을 찾을 수 없습니다!
)

echo ================================
pause
