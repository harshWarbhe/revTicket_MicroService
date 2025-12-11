#!/usr/bin/env node

const { spawn, execSync } = require('child_process');
const http = require('http');
const path = require('path');
const fs = require('fs');

const processes = [];
const backendDir = path.join(__dirname, 'Microservices-Backend');
const logsDir = path.join(backendDir, 'logs');
const isWindows = process.platform === 'win32';

if (!fs.existsSync(logsDir)) {
    fs.mkdirSync(logsDir, { recursive: true });
}

function log(msg) {
    console.log(`[${new Date().toLocaleTimeString()}] ${msg}`);
}

function killExistingProcesses() {
    log('Cleaning up existing processes...');
    const ports = [8761, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090, 8091];

    ports.forEach(port => {
        try {
            if (isWindows) {
                execSync(`for /f "tokens=5" %a in ('netstat -aon ^| find ":${port}" ^| find "LISTENING"') do taskkill /F /PID %a`,
                    { stdio: 'ignore' });
            } else {
                execSync(`lsof -ti:${port} | xargs kill -9 2>/dev/null || true`, { stdio: 'ignore' });
            }
        } catch (e) {
            // Ignore errors if no process is using the port
        }
    });

    log('✓ Cleanup complete');
}

function checkHealth(port, path = '/actuator/health') {
    return new Promise((resolve) => {
        const req = http.get(`http://localhost:${port}${path}`, { timeout: 3000 }, (res) => {
            resolve(res.statusCode === 200);
        });
        req.on('error', () => resolve(false));
        req.on('timeout', () => { req.destroy(); resolve(false); });
    });
}

async function waitForService(port, name, maxWait = 60, healthPath = '/actuator/health') {
    const start = Date.now();
    let dots = '';

    while ((Date.now() - start) / 1000 < maxWait) {
        if (await checkHealth(port, healthPath)) {
            process.stdout.write('\r' + ' '.repeat(80) + '\r');
            log(`✓ ${name} is UP (http://localhost:${port})`);
            return true;
        }
        dots = dots.length >= 3 ? '' : dots + '.';
        process.stdout.write(`\r[${new Date().toLocaleTimeString()}] Waiting for ${name}${dots}`);
        await new Promise(r => setTimeout(r, 1000));
    }

    process.stdout.write('\r' + ' '.repeat(80) + '\r');
    log(`⚠ ${name} timeout - check logs/${name.split('-')[0]}.log`);
    return false;
}

function startService(serviceName, port) {
    const jarPath = path.join(backendDir, serviceName, 'target', `${serviceName}-1.0.0.jar`);

    if (!fs.existsSync(jarPath)) {
        log(`Building ${serviceName}...`);
        const mvn = isWindows ? 'mvn.cmd' : 'mvn';
        const build = spawn(mvn, ['clean', 'package', '-DskipTests'], {
            cwd: path.join(backendDir, serviceName),
            stdio: 'inherit'
        });

        return new Promise((resolve) => {
            build.on('close', (code) => {
                if (code === 0) {
                    log(`✓ ${serviceName} built`);
                    runJar(serviceName, jarPath, port);
                    resolve(true);
                } else {
                    log(`✗ ${serviceName} build failed`);
                    resolve(false);
                }
            });
        });
    } else {
        runJar(serviceName, jarPath, port);
        return Promise.resolve(true);
    }
}

function runJar(serviceName, jarPath, port) {
    log(`Starting ${serviceName} on port ${port}...`);

    const logFile = path.join(logsDir, `${serviceName.split('-')[0]}.log`);
    const logStream = fs.createWriteStream(logFile, { flags: 'a' });

    const proc = spawn('java', ['-jar', jarPath], {
        cwd: path.dirname(jarPath),
        stdio: ['ignore', 'pipe', 'pipe'],
        env: { ...process.env, SERVER_PORT: port.toString() }
    });

    proc.stdout.pipe(logStream);
    proc.stderr.pipe(logStream);

    proc.on('error', (err) => {
        log(`✗ ${serviceName} error: ${err.message}`);
    });

    proc.on('exit', (code) => {
        if (code !== 0 && code !== null) {
            log(`✗ ${serviceName} exited with code ${code}`);
        }
    });

    processes.push({ proc, name: serviceName, port });
}

function startFrontend() {
    log('Starting Frontend with proxy...');
    const npm = isWindows ? 'npm.cmd' : 'npm';

    const proc = spawn(npm, ['run', 'dev'], {
        cwd: path.join(__dirname, 'Frontend'),
        stdio: 'inherit'
    });

    processes.push({ proc, name: 'frontend' });
}

function cleanup() {
    log('\n\nStopping services...');
    processes.forEach(({ proc, name }) => {
        try {
            proc.kill();
            log(`✓ Stopped ${name}`);
        } catch (e) { }
    });

    setTimeout(() => {
        try {
            if (!isWindows) {
                execSync('pkill -9 -f "spring-boot" 2>/dev/null || true', { stdio: 'ignore' });
            }
        } catch (e) { }
        process.exit(0);
    }, 1000);
}

async function checkServiceHealth() {
    console.log('\n📊 Service Health Status:\n');

    const services = [
        { name: 'Eureka Server', port: 8761 },
        { name: 'API Gateway', port: 8080 },
        { name: 'User Service', port: 8082 },
        { name: 'Movie Service', port: 8081 },
        { name: 'Theater Service', port: 8083 },
        { name: 'Payment Service', port: 8084 },
        { name: 'Booking Service', port: 8085 },
        { name: 'Showtime Service', port: 8086 },
        { name: 'Review Service', port: 8087 },
        { name: 'Search Service', port: 8088 },
        { name: 'Settings Service', port: 8089 },
        { name: 'Notification Service', port: 8090 },
        { name: 'Dashboard Service', port: 8091 }
    ];

    for (const service of services) {
        const isUp = await checkHealth(service.port);
        const status = isUp ? '✅ UP  ' : '❌ DOWN';
        console.log(`  ${status}  ${service.name.padEnd(20)} (http://localhost:${service.port})`);
    }

    const frontendUp = await checkHealth(4200, '/');
    const frontendStatus = frontendUp ? '✅ UP  ' : '❌ DOWN';
    console.log(`  ${frontendStatus}  ${'Frontend'.padEnd(20)} (http://localhost:4200)`);
}

async function main() {
    console.log('🚀 RevTicket Microservices Launcher\n');

    killExistingProcesses();
    await new Promise(r => setTimeout(r, 2000));

    const services = [
        { name: 'eureka-server', port: 8761, wait: 15 },
        { name: 'api-gateway', port: 8080, wait: 15 },
        { name: 'user-service', port: 8082, wait: 15 },
        { name: 'settings-service', port: 8089, wait: 15 },
        { name: 'movie-service', port: 8081, wait: 15 },
        { name: 'theater-service', port: 8083, wait: 15 },
        { name: 'showtime-service', port: 8086, wait: 15 },
        { name: 'booking-service', port: 8085, wait: 15 },
        { name: 'payment-service', port: 8084, wait: 15 },
        { name: 'review-service', port: 8087, wait: 15 },
        { name: 'search-service', port: 8088, wait: 15 },
        { name: 'notification-service', port: 8090, wait: 15 },
        { name: 'dashboard-service', port: 8091, wait: 15 }
    ];

    for (const service of services) {
        await startService(service.name, service.port);
        await waitForService(service.port, service.name, service.wait);
        await new Promise(r => setTimeout(r, 500));
    }

    await checkServiceHealth();

    log('Waiting for all services to stabilize...');
    await new Promise(r => setTimeout(r, 3000));

    startFrontend();
    await new Promise(r => setTimeout(r, 8000));

    console.log('\n📱 Quick Access:');
    console.log('  Frontend:       http://localhost:4200');
    console.log('  API Gateway:    http://localhost:8080');
    console.log('  Eureka Console: http://localhost:8761');
    console.log('\n📂 Logs: ' + logsDir);
    console.log('\n✅ API URL: http://localhost:8080/api');
    console.log('\n⌨️  Press Ctrl+C to stop all services\n');
}

process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);

main().catch((err) => {
    console.error('❌ Error:', err);
    cleanup();
});
