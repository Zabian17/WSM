@echo off
pushd "%~dp0..\.."
echo ========================================================
echo   GUDANGKU - Git Branch Update & Release Automation
echo ========================================================

echo 1. Creating and switching to branch 'update-3.0'...
git checkout -b update-3.0 2>nul || git checkout update-3.0

echo 2. Removing untracked cached files specified in .gitignore...
git rm -r --cached . >nul 2>&1

echo 3. Staging all modified and new files...
git add .

echo 4. Committing changes with message 'First release v1.0.0'...
git commit -m "First release v1.0.0"

echo 5. Pushing branch 'update-3.0' to remote origin...
git push -u origin update-3.0

echo 6. Creating Git tag 'v1.0.0'...
git tag -a v1.0.0 -m "First Release v1.0.0"
git push origin v1.0.0

echo 7. Creating GitHub Release 'v1.0.0'...
gh release create v1.0.0 --title "First Release v1.0.0" --notes "First release v1.0.0 of GudangKu WMS JavaFX + Spring Boot application" --target update-3.0 2>nul || echo (Notice: If gh CLI is not installed, the GitHub Release can be created via GitHub UI using tag v1.0.0)

echo ========================================================
echo   SUCCESS! All steps for update-3.0 & v1.0.0 completed.
echo ========================================================
popd
pause
