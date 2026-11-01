# Lists devices on the PC's networks that answer on the ADB port (5555).
$mine = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' }

foreach ($a in $mine) {
    $base = $a.IPAddress.Substring(0, $a.IPAddress.LastIndexOf('.') + 1)
    $probes = @()
    foreach ($n in 1..254) {
        $ip = "$base$n"
        if ($ip -eq $a.IPAddress) { continue }
        $client = New-Object System.Net.Sockets.TcpClient
        $probes += [pscustomobject]@{ Ip = $ip; Client = $client; Task = $client.ConnectAsync($ip, 5555) }
    }
    Start-Sleep -Milliseconds 2500
    foreach ($p in $probes) {
        if ($p.Task.Status -eq 'RanToCompletion' -and $p.Client.Connected) { Write-Output $p.Ip }
        $p.Client.Dispose()
    }
}
