# Docker WSL Manager - Release Guide

## 🎉 Successfully Configured for Release!

Your project is now fully configured to create standalone, distribution-ready JAR files.

## 📦 What Was Done

### 1. **POM.xml Configuration**
- ✅ All dependency versions moved to properties for easy management
- ✅ Maven Shade Plugin configured for creating uber-JAR
- ✅ Service resource transformers added for proper merging
- ✅ Proper MANIFEST with main class and metadata
- ✅ Release profile added with additional optimizations
- ✅ Java target version fixed to 22 (matching your Maven JDK)

### 2. **Build Scripts Created**
- ✅ `build-release.ps1` - Automated release build script
- ✅ `run.bat` - Windows launcher for end users
- ✅ `RELEASE_BUILD.md` - Detailed build documentation

### 3. **Fixed Issues**
- ✅ Fixed "invalid target release: 23" error by changing to Java 22
- ✅ Fixed log viewing for running containers (timeout added)
- ✅ Fixed console attach command construction for Windows

## 🚀 Quick Start - Creating a Release

### Option 1: Using PowerShell Script (Recommended)
```powershell
.\build-release.ps1
```

This will:
1. Clean previous builds
2. Compile and package with release profile
3. Create `release/` folder with all files
4. Generate `docker-wsl-manager-1.0.0.zip` ready for distribution

### Option 2: Using Maven Directly
```bash
# Standard build
mvn clean package

# Release build (with optimizations)
mvn clean package -P release
```

## 📂 Release Package Contents

After running the build script, you'll have:

```
release/
├── docker-wsl-manager.jar      # Standalone executable JAR (42.54 MB)
├── run.bat                      # Windows launcher
├── README.md                    # User documentation
├── LICENSE                      # License file
└── RELEASE_NOTES.txt           # Auto-generated release notes
```

Plus a ZIP file: `docker-wsl-manager-1.0.0.zip` (37.92 MB)

## 📋 Distribution Checklist

Before releasing to users:

- [ ] Test the JAR file: `java -jar release\docker-wsl-manager.jar`
- [ ] Test the launcher: `release\run.bat`
- [ ] Verify Docker connection works
- [ ] Test container management features
- [ ] Check log viewing functionality
- [ ] Test console attach feature
- [ ] Update version number in `pom.xml` if needed
- [ ] Update `RELEASE_NOTES.txt` with specific release information
- [ ] Create GitHub release with the ZIP file

## 🔧 Version Management

To release a new version:

1. **Update version in `pom.xml`:**
   ```xml
   <version>1.1.0</version>
   ```

2. **Run release build:**
   ```powershell
   .\build-release.ps1 -Version "1.1.0"
   ```

3. **The script automatically:**
   - Updates all version references
   - Creates properly named artifacts
   - Generates release notes with new version

## 📦 Dependency Versions (Centralized)

All versions are now in `pom.xml` properties:
- Java: 22
- JavaFX: 25.0.1
- Docker Java: 3.7.0
- SLF4J: 2.0.17
- Logback: 1.5.22
- Maven Plugins: 3.14.1, 3.6.1, etc.

To update a dependency, just change the version in the `<properties>` section!

## 🐛 Troubleshooting

### Build fails with "invalid target release"
- Ensure you're using Java 22 or higher
- Check Maven is using correct JDK: `mvn -version`

### JAR file is too large
- Current size (42.54 MB) includes all dependencies
- This is expected for a standalone JavaFX application
- Cannot be significantly reduced without removing features

### Application won't start
- Verify Java 22+ is installed: `java -version`
- Run from command line to see error messages
- Check Docker daemon is accessible

## 📚 Additional Documentation

- `RELEASE_BUILD.md` - Detailed build instructions
- `README.md` - User documentation
- `pom.xml` - Full build configuration

## ✨ Features Summary

Your release package includes:
- ✅ Standalone JAR (no external dependencies needed)
- ✅ Windows launcher script with Java version check
- ✅ All Docker management features
- ✅ Container log viewing (fixed for running containers)
- ✅ Console attach via cmd window
- ✅ Image, volume, and network management
- ✅ Auto-discovery of Docker in WSL

## 🎯 Next Steps

1. **Test thoroughly** in a clean environment
2. **Create GitHub release** with the ZIP file
3. **Document any known issues**
4. **Share with users!**

---

**Happy Releasing! 🚀**

