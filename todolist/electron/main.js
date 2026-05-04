const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

let mainWindow;
let backendProcess;

// 获取 JAR 包路径
function getJarPath() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, 'resources', 'demo-0.0.1-SNAPSHOT.jar');
  }
  return path.join(__dirname, '../../demo/target/demo-0.0.1-SNAPSHOT.jar');
}

// 获取 Java 路径
function getJavaPath() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, 'resources', 'jre', 'bin', 'java.exe');
  }
  return 'java';
}

// 启动后端服务
function startBackend() {
  const jarPath = getJarPath();
  const javaPath = getJavaPath();

  // 工作目录：打包后指向 exe 所在目录，开发时指向 demo 目录
  const workDir = app.isPackaged
    ? path.dirname(app.getPath('exe'))
    : path.join(__dirname, '../../demo');

  console.log(`[Main] 启动后端...`);
  console.log(`[Main] Java: ${javaPath}`);
  console.log(`[Main] Jar: ${jarPath}`);
  console.log(`[Main] 工作目录: ${workDir}`);

  backendProcess = spawn(javaPath, ['-jar', jarPath, '--server.port=8080'], {
    stdio: 'pipe',
    cwd: workDir
  });

  backendProcess.stdout.on('data', (data) => {
    console.log(`[Backend] ${data}`);
  });

  backendProcess.stderr.on('data', (data) => {
    console.error(`[Backend] ${data}`);
  });

  backendProcess.on('close', (code) => {
    console.log(`[Backend] 进程退出，退出码: ${code}`);
  });

  backendProcess.on('error', (err) => {
    console.error(`[Backend] 启动失败: ${err.message}`);
  });
}

// 创建窗口
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  if (!app.isPackaged) {
    mainWindow.loadURL('http://localhost:5173');
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }
}

// 应用启动
app.whenReady().then(() => {
  startBackend();
  // 给后端 3 秒启动时间
  setTimeout(createWindow, 3000);
});

// 退出时关闭后端
app.on('before-quit', () => {
  if (backendProcess && !backendProcess.killed) {
    console.log('[Main] 正在关闭后端...');
    backendProcess.kill();
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  }
});