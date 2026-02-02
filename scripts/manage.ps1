# Script de gestión de NaranjaX
# Uso: .\scripts\manage.ps1 [comando]

param(
    [Parameter(Mandatory=$false)]
    [string]$Command = "help"
)

function Show-Help {
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║     NaranjaX - Virtual Wallet Management      ║" -ForegroundColor Cyan
    Write-Host "║          (Spring Cloud Edition 2.0)           ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Comandos disponibles:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  up             " -NoNewline; Write-Host "- Levantar todo el ecosistema (Docker Compose)" -ForegroundColor Gray
    Write-Host "  down           " -NoNewline; Write-Host "- Detener infraestructura" -ForegroundColor Gray
    Write-Host "  build          " -NoNewline; Write-Host "- Compilar servicios (Preferiblemente via Docker)" -ForegroundColor Gray
    Write-Host "  rebuild        " -NoNewline; Write-Host "- Forzar reconstrucción de imágenes Docker" -ForegroundColor Gray
    Write-Host "  logs           " -NoNewline; Write-Host "- Ver logs de todos los servicios" -ForegroundColor Gray
    Write-Host "  status         " -NoNewline; Write-Host "- Ver estado de los servicios en Eureka y Docker" -ForegroundColor Gray
    Write-Host "  clean          " -NoNewline; Write-Host "- Limpiar contenedores y volúmenes (¡CUIDADO!)" -ForegroundColor Gray
    Write-Host "  help           " -NoNewline; Write-Host "- Mostrar esta ayuda" -ForegroundColor Gray
    Write-Host ""
}

function Build-Services {
    $mvnExists = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnExists) {
        Write-Host "🔨 Compilando servicios con Maven local..." -ForegroundColor Green
        mvn clean install -DskipTests
    } else {
        Write-Host "🐳 Maven local no detectado. Usando Docker para compilar..." -ForegroundColor Yellow
        docker-compose build
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Build exitoso!" -ForegroundColor Green
    } else {
        Write-Host "❌ Error en el build" -ForegroundColor Red
    }
}

function Start-Infrastructure {
    Write-Host "🚀 Levantando infraestructura Spring Cloud..." -ForegroundColor Green
    docker-compose up -d
    Write-Host ""
    Write-Host "✅ Servicios iniciados!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🌐 URLs Principales de Acceso:" -ForegroundColor Yellow
    Write-Host "  API Gateway (Entrada):    http://localhost:8080" -ForegroundColor Cyan
    Write-Host "  Discovery Dashboard:      http://localhost:8761" -ForegroundColor Cyan
    Write-Host "  Config Server API:        http://localhost:8888" -ForegroundColor Cyan
    Write-Host "  Kafka Control Center:     http://localhost:9021" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Nota: Los servicios individuales (8081, 8082, etc.) son accesibles pero se recomienda usar el Gateway (8080)." -ForegroundColor Gray
}

function Stop-Infrastructure {
    Write-Host "🛑 Deteniendo infraestructura..." -ForegroundColor Yellow
    docker-compose down
    Write-Host "✅ Servicios detenidos!" -ForegroundColor Green
}

function Show-Logs {
    Write-Host "📋 Mostrando logs..." -ForegroundColor Cyan
    docker-compose logs -f
}

function Show-Status {
    Write-Host "📊 Estado de los contenedores Docker:" -ForegroundColor Cyan
    docker-compose ps
    Write-Host ""
    Write-Host "💡 Tip: Revisa http://localhost:8761 para ver si los servicios se registraron correctamente en Eureka." -ForegroundColor Gray
}

function Clean-All {
    Write-Host "⚠️  ADVERTENCIA: Esta operación eliminará todos los contenedores y volúmenes" -ForegroundColor Red
    $confirm = Read-Host "¿Estás seguro? (yes/no)"
    if ($confirm -eq "yes") {
        Write-Host "🧹 Limpiando..." -ForegroundColor Yellow
        docker-compose down -v
        Write-Host "✅ Limpieza completada!" -ForegroundColor Green
    } else {
        Write-Host "❌ Operación cancelada" -ForegroundColor Yellow
    }
}

function Rebuild-Images {
    Write-Host "🔨 Reconstruyendo imágenes Docker (con build interno)..." -ForegroundColor Green
    docker-compose up -d --build
    Write-Host "✅ Servicios reconstruidos e iniciados!" -ForegroundColor Green
}

# Main
switch ($Command.ToLower()) {
    "build"   { Build-Services }
    "up"      { Start-Infrastructure }
    "down"    { Stop-Infrastructure }
    "logs"    { Show-Logs }
    "status"  { Show-Status }
    "clean"   { Clean-All }
    "rebuild" { Rebuild-Images }
    "help"    { Show-Help }
    default   { 
        Write-Host "❌ Comando desconocido: $Command" -ForegroundColor Red
        Write-Host ""
        Show-Help
    }
}
