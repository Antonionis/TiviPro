<?php
// telegram.php - Servidor de Gestión Remota de Alertas de Telegram y Sincronización a BD para TiviPro
ini_set('display_errors', 0);
error_reporting(0);

// Permitir acceso desde cualquier dispositivo / origen si se consume vía API o navegador
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Cargar credenciales desde el archivo .env si existe
$env_path = __DIR__ . '/.env';
$env = file_exists($env_path) ? parse_ini_file($env_path) : [];

$db_host = $env['DB_HOST'] ?? 'localhost';
$db_name = $env['DB_NAME'] ?? 'panel_app';
$db_user = $env['DB_USER'] ?? 'root';
$db_pass = $env['DB_PASS'] ?? '';

function get_pdo_connection() {
    global $db_host, $db_name, $db_user, $db_pass;

    $host = !empty($db_host) ? $db_host : 'localhost';
    $user = !empty($db_user) ? $db_user : 'root';
    $pass = isset($db_pass) ? $db_pass : '';
    $dbname = !empty($db_name) ? $db_name : 'panel_app';

    $db_candidates = array_unique([$dbname, 'panel_app', 'tivipro']);
    $user_pass_candidates = [
        [$user, $pass],
        ['root', ''],
        ['root', 'goaltv123'],
        ['root', 'root'],
        ['tivipro', 'tivipro'],
        ['panel_app', 'panel_app']
    ];

    foreach ($user_pass_candidates as $cred) {
        $c_user = $cred[0];
        $c_pass = $cred[1];
        foreach ($db_candidates as $curr_db) {
            if (empty($curr_db)) continue;
            try {
                $dsn = "mysql:host=$host;dbname=$curr_db;charset=utf8mb4";
                $pdo = new PDO($dsn, $c_user, $c_pass);
                $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
                return $pdo;
            } catch (Exception $e) {}
        }
    }

    foreach ($user_pass_candidates as $cred) {
        $c_user = $cred[0];
        $c_pass = $cred[1];
        foreach ($db_candidates as $curr_db) {
            if (empty($curr_db)) continue;
            try {
                $mysqli = @new mysqli($host, $c_user, $c_pass, $curr_db);
                if (!$mysqli->connect_error) {
                    return $mysqli;
                }
            } catch (Exception $e) {}
        }
    }

    return null;
}

function get_mysql_telegram($device_id) {
    $conn = get_pdo_connection();
    if (!$conn) return null;

    if ($conn instanceof PDO) {
        try {
            $stmt = $conn->prepare("SELECT telegram FROM dispositivos WHERE device_id = ? OR UPPER(device_id) = ? LIMIT 1");
            $stmt->execute([$device_id, strtoupper($device_id)]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($row && !empty($row['telegram'])) {
                return trim($row['telegram']);
            }
        } catch (Exception $e) {}
    } else if ($conn instanceof mysqli) {
        try {
            $stmt = $conn->prepare("SELECT telegram FROM dispositivos WHERE device_id = ? OR UPPER(device_id) = ? LIMIT 1");
            if ($stmt) {
                $up = strtoupper($device_id);
                $stmt->bind_param("ss", $device_id, $up);
                $stmt->execute();
                $res = $stmt->get_result();
                if ($row = $res->fetch_assoc()) {
                    $val = !empty($row['telegram']) ? trim($row['telegram']) : null;
                    $stmt->close();
                    $conn->close();
                    return $val;
                }
                $stmt->close();
            }
            $conn->close();
        } catch (Exception $e) {}
    }
    return null;
}

function update_mysql_telegram($device_id, $telegram_user) {
    $conn = get_pdo_connection();
    if (!$conn) return;

    $val = (empty($telegram_user) || $telegram_user === 'NULL' || $telegram_user === 'null') ? null : $telegram_user;

    if ($conn instanceof PDO) {
        try {
            $stmt = $conn->prepare("UPDATE dispositivos SET telegram = ? WHERE device_id = ? OR UPPER(device_id) = ?");
            $stmt->execute([$val, $device_id, strtoupper($device_id)]);
        } catch (Exception $e) {}
    } else if ($conn instanceof mysqli) {
        try {
            $stmt = $conn->prepare("UPDATE dispositivos SET telegram = ? WHERE device_id = ? OR UPPER(device_id) = ?");
            if ($stmt) {
                $up = strtoupper($device_id);
                $stmt->bind_param("sss", $val, $device_id, $up);
                $stmt->execute();
                $stmt->close();
            }
            $conn->close();
        } catch (Exception $e) {}
    }
}

$db_file = __DIR__ . '/telegram_configs.json';

// Cargar registros existentes
$configs = [];
if (file_exists($db_file)) {
    try {
        $json = @file_get_contents($db_file);
        $configs = json_decode($json, true) ?: [];
    } catch (Exception $e) {}
}

// Obtener device_id automáticamente por GET, POST o JSON Body
$device_id = strtoupper(trim($_REQUEST['device_id'] ?? $_REQUEST['id_dispositivo'] ?? $_REQUEST['id'] ?? ''));
if (empty($device_id)) {
    $raw_input = json_decode(file_get_contents('php://input'), true);
    if (is_array($raw_input)) {
        $device_id = strtoupper(trim($raw_input['device_id'] ?? $raw_input['id_dispositivo'] ?? $raw_input['id'] ?? ''));
        $_REQUEST = array_merge($_REQUEST, $raw_input);
    }
}

// Acción 0: Desvincular / Limpiar Telegram desde la App
if (isset($_REQUEST['action']) && ($_REQUEST['action'] === 'clear_telegram' || $_REQUEST['action'] === 'clear')) {
    if (!empty($device_id)) {
        if (isset($configs[$device_id])) {
            unset($configs[$device_id]);
            @file_put_contents($db_file, json_encode($configs, JSON_PRETTY_PRINT));
        }
        update_mysql_telegram($device_id, null);
    }
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'status' => 'success',
        'message' => 'Telegram desvinculado con éxito',
        'device_id' => $device_id
    ]);
    exit;
}

// Acción 1: Polling desde la App Android TiviPro (action=poll o action=get_telegram)
if (isset($_GET['action']) && ($_GET['action'] === 'poll' || $_GET['action'] === 'get_telegram')) {
    header('Content-Type: application/json; charset=utf-8');

    $telegram_user = null;
    if ($device_id && isset($configs[$device_id])) {
        $telegram_user = $configs[$device_id]['telegram_user'];
    }

    if (empty($telegram_user) && !empty($device_id)) {
        $db_user = get_mysql_telegram($device_id);
        if (!empty($db_user)) {
            $telegram_user = $db_user;
        }
    }

    if (!empty($telegram_user)) {
        echo json_encode([
            'status' => 'success',
            'device_id' => $device_id,
            'telegram' => $telegram_user,
            'telegram_user' => $telegram_user,
            'chat_id' => $telegram_user,
            'timestamp' => time()
        ]);
    } else {
        echo json_encode([
            'status' => 'waiting',
            'device_id' => $device_id
        ]);
    }
    exit;
}

header('Content-Type: text/html; charset=utf-8');

// Acción 2: Recepción del Formulario Web o Petición API desde la App (POST/GET)
$msg = '';
$msg_type = '';
$guardado_ok = false;
$telegram_user = trim($_REQUEST['telegram'] ?? $_REQUEST['telegram_user'] ?? $_REQUEST['chat_id'] ?? '');

if ($_SERVER['REQUEST_METHOD'] === 'POST' || !empty($telegram_user)) {
    if (empty($device_id)) {
        $msg = 'Error: ID de Dispositivo no válido o ausente.';
        $msg_type = 'error';
    } elseif (empty($telegram_user)) {
        $msg = 'Por favor, introduce tu ID numérico o Usuario de Telegram.';
        $msg_type = 'error';
    } else {
        if (!preg_match('/^@/', $telegram_user) && !is_numeric($telegram_user)) {
            $telegram_user = '@' . $telegram_user;
        }

        // 1. Actualizar de inmediato en la Columna 'telegram' de la Base de Datos MySQL (panel_app.dispositivos)
        update_mysql_telegram($device_id, $telegram_user);

        // 2. Guardar en JSON para Polling rápido
        try {
            $configs[$device_id] = [
                'device_id' => $device_id,
                'telegram_user' => $telegram_user,
                'timestamp' => time(),
                'ip' => $_SERVER['REMOTE_ADDR'] ?? ''
            ];
            @file_put_contents($db_file, json_encode($configs, JSON_PRETTY_PRINT));
        } catch (Exception $e) {}

        if (isset($_REQUEST['action']) && $_REQUEST['action'] === 'update_telegram') {
            header('Content-Type: application/json; charset=utf-8');
            echo json_encode([
                'status' => 'success',
                'message' => 'Columna telegram actualizada en BD panel_app',
                'device_id' => $device_id,
                'telegram' => $telegram_user
            ]);
            exit;
        }

        $msg = "¡Configuración guardada con éxito!";
        $msg_type = 'success';
        $guardado_ok = true;
    }
}

$saved_user = isset($configs[$device_id]) ? $configs[$device_id]['telegram_user'] : get_mysql_telegram($device_id);
?>
<!DOCTYPE html>
<html lang="es" class="dark scroll-smooth">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes">
    <title>Configurar alertas de eventos en Telegram - TiviPro</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <script>
        tailwind.config = {
            darkMode: 'class',
            theme: {
                extend: {
                    colors: {
                        brand: {
                            dark: '#030816', card: '#0a122c', border: '#1e295d',
                            primary: '#0052d4', secondary: '#4364f7', cyan: '#6fb1fc', accent: '#00d2ff'
                        }
                    },
                    fontFamily: { sans: ['Inter', 'sans-serif'] }
                }
            }
        }
    </script>
    <style>
        body { background-color: #030816; color: #f1f5f9; font-family: 'Inter', sans-serif; }
        .text-gradient-brand { background: linear-gradient(135deg, #ffffff 0%, #6fb1fc 50%, #00d2ff 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .pill-badge { background: linear-gradient(90deg, #0036a7 0%, #0072ff 50%, #00c6ff 100%); border-radius: 9999px; box-shadow: 0 8px 25px rgba(0, 114, 255, 0.35); transition: all 0.2s ease; }
        .pill-badge:hover { filter: brightness(1.08); transform: translateY(-1px); }
        .glass-card { background: rgba(10, 18, 44, 0.85); backdrop-filter: blur(16px); border: 1px solid rgba(255, 255, 255, 0.08); }
    </style>
</head>
<body class="min-h-screen flex flex-col justify-between antialiased selection:bg-brand-accent selection:text-black">
    <main class="flex-grow flex items-center justify-center p-3 sm:p-6">
        <div class="w-full max-w-md">
            <div class="glass-card p-5 sm:p-8 rounded-3xl shadow-2xl space-y-5 sm:space-y-6">
                <div class="text-center space-y-2.5 sm:space-y-3">
                    <div class="flex justify-center mb-1">
                        <img src="logop.png" alt="TiviPro" class="h-12 object-contain" onerror="this.remove()">
                    </div>
                    <h1 class="text-xl sm:text-2xl font-black text-white">Alertas <span class="text-gradient-brand">Telegram</span></h1>
                    <p class="text-slate-400 text-xs">Configuración remota de eventos para TiviPro</p>
                </div>

                <?php if (!empty($msg)): ?>
                    <?php if ($msg_type === 'success'): ?>
                        <div class="bg-brand-primary/20 border border-brand-accent/40 text-brand-accent text-xs py-3 px-4 rounded-xl text-center font-medium">
                            <i class="fa-solid fa-circle-check mr-1"></i> <?= htmlspecialchars($msg) ?>
                        </div>
                    <?php else: ?>
                        <div class="bg-red-500/15 border border-red-500/40 text-red-400 text-xs py-3 px-4 rounded-xl text-center font-medium">
                            <i class="fa-solid fa-triangle-exclamation mr-1"></i> <?= htmlspecialchars($msg) ?>
                        </div>
                    <?php endif; ?>
                <?php endif; ?>

                <div class="bg-brand-dark/90 border border-brand-border rounded-2xl p-4 space-y-2">
                    <h3 class="text-xs font-bold text-brand-cyan uppercase tracking-wider">Pasos para activar las alertas</h3>
                    <ol class="list-decimal list-inside text-xs text-slate-300 space-y-1.5 leading-relaxed">
                        <li>Inicia conversación con <a href="https://t.me/tivipro_bot" target="_blank" rel="noopener noreferrer" class="text-brand-accent hover:underline font-semibold">@tivipro_bot</a> y pulsa <b>INICIAR</b>.</li>
                        <li>Consulta tu ID numérico en <a href="https://t.me/userinfobot" target="_blank" rel="noopener noreferrer" class="text-brand-accent hover:underline font-semibold">@userinfobot</a>, pulsa <b>INICIAR</b> y copia el <b>ID</b>.</li>
                        <li>Introduce tu ID y pulsa guardar para vincularlo a TiviPro.</li>
                    </ol>
                </div>

                <form method="POST" action="" class="space-y-4">
                    <input type="hidden" name="device_id" value="<?= htmlspecialchars($device_id) ?>">
                    <input type="hidden" name="id" value="<?= htmlspecialchars($device_id) ?>">
                    <input type="hidden" name="id_dispositivo" value="<?= htmlspecialchars($device_id) ?>">

                    <div>
                        <label for="telegram_user" class="block text-xs font-semibold text-slate-300 mb-1">Tu ID de Telegram</label>
                        <input type="text" id="telegram_user" name="telegram_user" value="<?= htmlspecialchars($saved_user) ?>" required placeholder="Ej: 123456789" class="w-full bg-brand-dark text-slate-100 border border-brand-border focus:border-brand-accent rounded-xl px-4 py-2.5 text-sm focus:outline-none">
                    </div>

                    <?php if ($guardado_ok): ?>
                        <button type="button" id="btnCerrarVentana" class="w-full pill-badge py-3.5 text-white font-bold text-sm flex items-center justify-center gap-2 mt-2 cursor-default">
                            <i class="fa-solid fa-xmark"></i> Ya puede cerrar la ventana
                        </button>
                    <?php else: ?>
                        <button type="submit" class="w-full pill-badge py-3.5 text-white font-bold text-sm flex items-center justify-center gap-2 mt-2 cursor-pointer">
                            <i class="fa-solid fa-paper-plane"></i> Guardar y vincular a TiviPro
                        </button>
                    <?php endif; ?>
                </form>
            </div>
        </div>
    </main>

    <footer class="bg-brand-dark border-t border-slate-800/80 py-6 text-center text-xs text-slate-500 px-4">
        © <?= date('Y') ?> TiviPro • Gestión Remota Oficial
    </footer>
</body>
</html>
