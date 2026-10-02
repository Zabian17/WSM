Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  GUDANGKU - Git Branch Update & Release Automation" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Checkout new branch update-3.0
Write-Host "1. Switching to branch 'update-3.0'..." -ForegroundColor Yellow
git checkout -b update-3.0 2>$null
if ($LASTEXITCODE -ne 0) { git checkout update-3.0 }

# 2. Clear index cache for ignored files
Write-Host "2. Refreshing git index against .gitignore..." -ForegroundColor Yellow
git rm -r --cached . 2>$null

# 3. Stage all files
Write-Host "3. Staging files..." -ForegroundColor Yellow
git add .

# 4. Commit
Write-Host "4. Committing changes..." -ForegroundColor Yellow
git commit -m "First release v1.0.0"

# 5. Push branch
Write-Host "5. Pushing branch 'update-3.0' to remote origin..." -ForegroundColor Yellow
git push -u origin update-3.0

# 6. Tag & push tag
Write-Host "6. Creating tag 'v1.0.0'..." -ForegroundColor Yellow
git tag -a v1.0.0 -m "First Release v1.0.0"
git push origin v1.0.0

# 7. GitHub Release via gh CLI (if available)
Write-Host "7. Creating GitHub Release 'v1.0.0'..." -ForegroundColor Yellow
if (Get-Command gh -ErrorAction SilentlyContinue) {
    gh release create v1.0.0 --title "First Release v1.0.0" --notes "First release v1.0.0 of GudangKu WMS JavaFX + Spring Boot application" --target update-3.0
} else {
    Write-Host "Notice: gh CLI not detected. You can create the Release on GitHub web interface using tag 'v1.0.0'." -ForegroundColor Magenta
}

Write-Host "========================================================" -ForegroundColor Green
Write-Host "  SUCCESS! All steps for update-3.0 & v1.0.0 completed." -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
