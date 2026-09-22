$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$names = @('APP_BOOTSTRAP_ENABLED','AUX_USERNAME','AUX_EMAIL','AUX_PASSWORD')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $env:AUX_USERNAME = Read-Host 'Usuario del auxiliar'
    $env:AUX_EMAIL = Read-Host 'Correo del auxiliar'
    $securePassword = Read-Host 'Contraseña del auxiliar' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { $env:AUX_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    $env:APP_BOOTSTRAP_ENABLED = 'true'
    & .\mvnw.cmd spring-boot:run
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    if ($securePassword) { $securePassword.Dispose() }
}
