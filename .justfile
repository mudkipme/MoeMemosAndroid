# Justfile for MoeMemosAndroid
# https://github.com/casey/just

# List available commands
default:
    @just --list

# Build the project
build:
    ./gradlew build

# Build without running lint checks
build-no-lint:
    ./gradlew build -x lint

# Run the app on a connected device or emulator
dev:
    ./gradlew installDebug

# Run lint checks
lint:
    ./gradlew lint

# Clean the project
clean:
    ./gradlew clean

# Run tests
test:
    ./gradlew test

# Build and run the app
run: build-no-lint dev
    @echo "App built and installed"

# Create a release build
release:
    ./gradlew assembleRelease

# Generate APK
apk:
    ./gradlew assembleDebug
    @echo "APK generated at app/build/outputs/apk/debug/app-debug.apk"
