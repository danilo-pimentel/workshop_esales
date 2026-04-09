# ============================================================================
# distribute.ps1 - Distribui os CSVs de dados do exercicio 04 para todas as
#                  pastas data/ necessarias (4 linguagens x 2 fases)
#
# Uso:  .\distribute.ps1
#
# O arquivo transactions_1m.csv.gz e descompactado automaticamente.
# ============================================================================

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

$Destinations = @(
    "04-batch-performance\manual\bun\data",
    "04-batch-performance\manual\java\data",
    "04-batch-performance\manual\kotlin\data",
    "04-batch-performance\manual\php\data",
    "04-batch-performance\ai\bun\data",
    "04-batch-performance\ai\java\data",
    "04-batch-performance\ai\kotlin\data",
    "04-batch-performance\ai\php\data"
)

$CsvFiles = @(
    "transactions_1k.csv",
    "transactions_10k.csv",
    "transactions_50k.csv",
    "transactions_100k.csv",
    "transactions_500k.csv"
)

Write-Host "Distribuindo datasets para o exercicio 04..." -ForegroundColor Cyan
Write-Host ""

# Descompactar o 1m se ainda nao estiver descompactado
$Csv1m = Join-Path $ScriptDir "transactions_1m.csv"
$Csv1mGz = Join-Path $ScriptDir "transactions_1m.csv.gz"

if (-not (Test-Path $Csv1m)) {
    if (Test-Path $Csv1mGz) {
        Write-Host "Descompactando transactions_1m.csv.gz (~19MB -> ~53MB)..." -ForegroundColor Yellow

        # Tentar usar 7-Zip primeiro (mais rapido), depois .NET GZipStream
        $SevenZip = "C:\Program Files\7-Zip\7z.exe"
        if (Test-Path $SevenZip) {
            & $SevenZip x $Csv1mGz "-o$ScriptDir" -y 2>&1 | Out-Null
        } else {
            # Fallback: usar .NET GZipStream
            $input = [System.IO.File]::OpenRead($Csv1mGz)
            $output = [System.IO.File]::Create($Csv1m)
            $gzipStream = New-Object System.IO.Compression.GZipStream($input, [System.IO.Compression.CompressionMode]::Decompress)
            $buffer = New-Object byte[] 65536
            while (($n = $gzipStream.Read($buffer, 0, $buffer.Length)) -gt 0) {
                $output.Write($buffer, 0, $n)
            }
            $gzipStream.Close()
            $output.Close()
            $input.Close()
        }

        if (Test-Path $Csv1m) {
            Write-Host "Descompactado." -ForegroundColor Green
            Write-Host ""
        } else {
            Write-Host "ERRO: falha ao descompactar" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "ERRO: transactions_1m.csv.gz nao encontrado em $ScriptDir" -ForegroundColor Red
        exit 1
    }
}

$CsvFiles += "transactions_1m.csv"

# Copiar para todos os destinos
foreach ($dest in $Destinations) {
    $FullDest = Join-Path $RepoRoot $dest
    if (-not (Test-Path $FullDest)) {
        Write-Host "Criando $dest" -ForegroundColor Yellow
        New-Item -ItemType Directory -Path $FullDest -Force | Out-Null
    }

    foreach ($csv in $CsvFiles) {
        $src = Join-Path $ScriptDir $csv
        if (Test-Path $src) {
            Copy-Item -Path $src -Destination (Join-Path $FullDest $csv) -Force
        }
    }
    Write-Host "  " -NoNewline; Write-Host "V" -ForegroundColor Green -NoNewline; Write-Host " $dest"
}

Write-Host ""
Write-Host "Distribuicao concluida!" -ForegroundColor Green
Write-Host "Arquivos copiados: $($CsvFiles.Count) CSVs x $($Destinations.Count) pastas" -ForegroundColor Cyan
