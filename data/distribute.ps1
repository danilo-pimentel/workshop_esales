# ============================================================================
# distribute.ps1 - Distribui os CSVs de dados do exercicio 04 para as pastas
#                  data/ das linguagens escolhidas
#
# Uso:
#   .\distribute.ps1                     (abre menu interativo)
#   .\distribute.ps1 all                 (copia para todas as 8 pastas)
#   .\distribute.ps1 manual              (copia para todas as pastas manual/)
#   .\distribute.ps1 ai                  (copia para todas as pastas ai/)
#   .\distribute.ps1 manual bun          (copia so para manual/bun)
#   .\distribute.ps1 ai kotlin           (copia so para ai/kotlin)
#
# O arquivo transactions_1m.csv.gz e descompactado automaticamente.
# ============================================================================

param(
    [string]$Arg1 = "",
    [string]$Arg2 = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

$CsvFiles = @(
    "transactions_1k.csv",
    "transactions_10k.csv",
    "transactions_50k.csv",
    "transactions_100k.csv",
    "transactions_500k.csv",
    "transactions_1m.csv"
)

# ── Descompactar o 1m se necessario ─────────────────────────────────────────
function Ensure-1mExtracted {
    $Csv1m = Join-Path $ScriptDir "transactions_1m.csv"
    $Csv1mGz = Join-Path $ScriptDir "transactions_1m.csv.gz"

    if (Test-Path $Csv1m) { return }

    if (-not (Test-Path $Csv1mGz)) {
        Write-Host "ERRO: transactions_1m.csv.gz nao encontrado em $ScriptDir" -ForegroundColor Red
        exit 1
    }

    Write-Host "Descompactando transactions_1m.csv.gz (~19MB -> ~53MB)..." -ForegroundColor Yellow

    $SevenZip = "C:\Program Files\7-Zip\7z.exe"
    if (Test-Path $SevenZip) {
        & $SevenZip x $Csv1mGz "-o$ScriptDir" -y 2>&1 | Out-Null
    } else {
        $inStream = [System.IO.File]::OpenRead($Csv1mGz)
        $outStream = [System.IO.File]::Create($Csv1m)
        $gzipStream = New-Object System.IO.Compression.GZipStream($inStream, [System.IO.Compression.CompressionMode]::Decompress)
        $buffer = New-Object byte[] 65536
        while (($n = $gzipStream.Read($buffer, 0, $buffer.Length)) -gt 0) {
            $outStream.Write($buffer, 0, $n)
        }
        $gzipStream.Close(); $outStream.Close(); $inStream.Close()
    }

    if (Test-Path $Csv1m) {
        Write-Host "Descompactado." -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host "ERRO: falha ao descompactar" -ForegroundColor Red
        exit 1
    }
}

# ── Copiar para um destino especifico ───────────────────────────────────────
function Copy-To {
    param([string]$Phase, [string]$Lang)

    $ExerciseDir = Join-Path $RepoRoot "04-batch-performance\$Phase\$Lang"
    if (-not (Test-Path $ExerciseDir)) {
        Write-Host "  " -NoNewline; Write-Host "X" -ForegroundColor Red -NoNewline
        Write-Host " $Phase/$Lang - pasta do exercicio nao encontrada"
        return
    }

    $Dest = Join-Path $ExerciseDir "data"
    if (-not (Test-Path $Dest)) {
        New-Item -ItemType Directory -Path $Dest -Force | Out-Null
    }

    foreach ($csv in $CsvFiles) {
        $src = Join-Path $ScriptDir $csv
        if (Test-Path $src) {
            Copy-Item -Path $src -Destination (Join-Path $Dest $csv) -Force
        }
    }
    Write-Host "  " -NoNewline; Write-Host "V" -ForegroundColor Green -NoNewline
    Write-Host " $Phase/$Lang"
}

# ── Copiar para todas as linguagens de uma fase ─────────────────────────────
function Copy-Phase {
    param([string]$Phase)
    Write-Host "Copiando para $Phase/..." -ForegroundColor Yellow
    foreach ($lang in @("bun", "java", "kotlin", "php")) {
        Copy-To -Phase $Phase -Lang $lang
    }
}

# ── Copiar para todos ────────────────────────────────────────────────────────
function Copy-All {
    Write-Host "Copiando para TODAS as 8 pastas..." -ForegroundColor Yellow
    foreach ($phase in @("manual", "ai")) {
        foreach ($lang in @("bun", "java", "kotlin", "php")) {
            Copy-To -Phase $phase -Lang $lang
        }
    }
}

# ── Menu interativo ─────────────────────────────────────────────────────────
function Show-Menu {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor White
    Write-Host "  Distribute - Datasets do Exercicio 04" -ForegroundColor White
    Write-Host "============================================================" -ForegroundColor White
    Write-Host ""
    Write-Host "  Fase Manual:" -ForegroundColor White
    Write-Host "    1)  " -ForegroundColor Cyan -NoNewline; Write-Host "manual/bun"
    Write-Host "    2)  " -ForegroundColor Cyan -NoNewline; Write-Host "manual/java"
    Write-Host "    3)  " -ForegroundColor Cyan -NoNewline; Write-Host "manual/kotlin"
    Write-Host "    4)  " -ForegroundColor Cyan -NoNewline; Write-Host "manual/php"
    Write-Host "    5)  " -ForegroundColor Cyan -NoNewline; Write-Host "TODAS as linguagens de manual/"
    Write-Host ""
    Write-Host "  Fase AI:" -ForegroundColor White
    Write-Host "    6)  " -ForegroundColor Cyan -NoNewline; Write-Host "ai/bun"
    Write-Host "    7)  " -ForegroundColor Cyan -NoNewline; Write-Host "ai/java"
    Write-Host "    8)  " -ForegroundColor Cyan -NoNewline; Write-Host "ai/kotlin"
    Write-Host "    9)  " -ForegroundColor Cyan -NoNewline; Write-Host "ai/php"
    Write-Host "    10) " -ForegroundColor Cyan -NoNewline; Write-Host "TODAS as linguagens de ai/"
    Write-Host ""
    Write-Host "  A)  " -ForegroundColor Yellow -NoNewline; Write-Host "TODAS as pastas (manual + ai, 8 destinos)"
    Write-Host ""
    Write-Host "  0)  " -ForegroundColor Red -NoNewline; Write-Host "Sair"
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor White
    Write-Host "  Escolha uma opcao: " -NoNewline
}

# ── Main ─────────────────────────────────────────────────────────────────────

# Modo nao-interativo
if ($Arg1) {
    Ensure-1mExtracted
    switch -Regex ($Arg1) {
        '^(all|tudo)$' {
            Copy-All
        }
        '^(manual|ai)$' {
            if ($Arg2) {
                if ($Arg2 -notmatch '^(bun|java|kotlin|php)$') {
                    Write-Host "Linguagem invalida: $Arg2 (use bun, java, kotlin ou php)" -ForegroundColor Red
                    exit 1
                }
                Write-Host "Copiando para $Arg1/$Arg2..." -ForegroundColor Yellow
                Copy-To -Phase $Arg1 -Lang $Arg2
            } else {
                Copy-Phase -Phase $Arg1
            }
        }
        default {
            Write-Host "ERRO: argumento invalido '$Arg1'" -ForegroundColor Red
            Write-Host "Use: all, manual, ai, 'manual <lang>', 'ai <lang>' ou sem argumentos para o menu"
            exit 1
        }
    }
    Write-Host ""
    Write-Host "Distribuicao concluida!" -ForegroundColor Green
    exit 0
}

# Modo interativo
Show-Menu
$Opcao = Read-Host

Ensure-1mExtracted

switch ($Opcao) {
    "1"  { Write-Host "Copiando para manual/bun..." -ForegroundColor Yellow;    Copy-To -Phase "manual" -Lang "bun" }
    "2"  { Write-Host "Copiando para manual/java..." -ForegroundColor Yellow;   Copy-To -Phase "manual" -Lang "java" }
    "3"  { Write-Host "Copiando para manual/kotlin..." -ForegroundColor Yellow; Copy-To -Phase "manual" -Lang "kotlin" }
    "4"  { Write-Host "Copiando para manual/php..." -ForegroundColor Yellow;    Copy-To -Phase "manual" -Lang "php" }
    "5"  { Copy-Phase -Phase "manual" }
    "6"  { Write-Host "Copiando para ai/bun..." -ForegroundColor Yellow;        Copy-To -Phase "ai" -Lang "bun" }
    "7"  { Write-Host "Copiando para ai/java..." -ForegroundColor Yellow;       Copy-To -Phase "ai" -Lang "java" }
    "8"  { Write-Host "Copiando para ai/kotlin..." -ForegroundColor Yellow;     Copy-To -Phase "ai" -Lang "kotlin" }
    "9"  { Write-Host "Copiando para ai/php..." -ForegroundColor Yellow;        Copy-To -Phase "ai" -Lang "php" }
    "10" { Copy-Phase -Phase "ai" }
    { $_ -in "a","A" } { Copy-All }
    "0"  { Write-Host "Saindo."; exit 0 }
    default { Write-Host "Opcao invalida." -ForegroundColor Red; exit 1 }
}

Write-Host ""
Write-Host "Distribuicao concluida!" -ForegroundColor Green
