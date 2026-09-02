$ModelUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
$ModelFile = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"
$AdbPath = "C:\Users\hansa\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $ModelFile)) {
    Write-Host "Downloading Llama-3.2-1B-Instruct-Q4_K_M.gguf (this might take a few minutes)..."
    Invoke-WebRequest -Uri $ModelUrl -OutFile $ModelFile
} else {
    Write-Host "Model already downloaded locally."
}

Write-Host "Pushing model to emulator..."
# Ensure the directory exists on the device
& $AdbPath -s emulator-5554 shell "mkdir -p /data/user/0/com.example.gemma/files/models/"
& $AdbPath -s emulator-5554 push $ModelFile /data/user/0/com.example.gemma/files/models/Llama-3.2-1B-Instruct-Q4_K_M.gguf
Write-Host "Model pushed successfully. You can now restart the app."
