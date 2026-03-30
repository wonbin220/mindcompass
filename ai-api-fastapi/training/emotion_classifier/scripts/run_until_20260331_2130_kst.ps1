# KcELECTRA HAPPY/CALM 단일-knob 실험을 2026-03-31 21:30 KST까지 자동 점검하고 이어서 수행하는 래퍼 스크립트입니다.
[CmdletBinding()]
param()

$BaseScriptPath = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "run_until_2130_kst.ps1"
if (-not (Test-Path -LiteralPath $BaseScriptPath)) {
    throw "base script not found: $BaseScriptPath"
}

& $BaseScriptPath -CutoffLocalIso "2026-03-31T21:30:00"
