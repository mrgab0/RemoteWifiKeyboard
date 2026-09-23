using System;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Threading;
using System.Windows.Forms;
using Microsoft.Win32;

namespace RemoteKeyboardInstaller
{
    public class SetupForm : Form
    {
        private Panel headerPanel;
        private Label titleLabel;
        private Label subtitleLabel;
        private Label descLabel;
        private Label pathLabel;
        private TextBox pathBox;
        private Button browseBtn;
        private CheckBox chkAutoStart;
        private CheckBox chkDesktopShortcut;
        private CheckBox chkStartMenu;
        private CheckBox chkLaunchNow;
        private ProgressBar progressBar;
        private Label statusLabel;
        private Button installBtn;
        private Button cancelBtn;

        public SetupForm()
        {
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            this.Text = "Instalador de Remote WiFi Keyboard";
            this.Size = new Size(540, 480);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.BackColor = Color.FromArgb(15, 17, 26);
            this.ForeColor = Color.White;
            this.Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);

            // Header
            headerPanel = new Panel
            {
                Dock = DockStyle.Top,
                Height = 85,
                BackColor = Color.FromArgb(24, 28, 44)
            };

            titleLabel = new Label
            {
                Text = "⌨️ Remote WiFi Keyboard para Windows",
                Font = new Font("Segoe UI", 13f, FontStyle.Bold),
                ForeColor = Color.FromArgb(240, 244, 255),
                Location = new Point(20, 16),
                AutoSize = true
            };

            subtitleLabel = new Label
            {
                Text = "Asistente de Instalación y Configuración del Sistema",
                Font = new Font("Segoe UI", 9f),
                ForeColor = Color.FromArgb(160, 174, 192),
                Location = new Point(24, 46),
                AutoSize = true
            };

            headerPanel.Controls.Add(titleLabel);
            headerPanel.Controls.Add(subtitleLabel);
            this.Controls.Add(headerPanel);

            // Path Selection
            pathLabel = new Label
            {
                Text = "Carpeta de instalación:",
                Location = new Point(24, 105),
                AutoSize = true,
                ForeColor = Color.FromArgb(203, 213, 225)
            };
            this.Controls.Add(pathLabel);

            string defaultPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "RemoteWiFiKeyboard");
            pathBox = new TextBox
            {
                Text = defaultPath,
                Location = new Point(24, 130),
                Width = 370,
                BackColor = Color.FromArgb(30, 35, 55),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.FixedSingle,
                Font = new Font("Segoe UI", 9.5f)
            };
            this.Controls.Add(pathBox);

            browseBtn = new Button
            {
                Text = "Examinar...",
                Location = new Point(405, 128),
                Width = 95,
                Height = 28,
                BackColor = Color.FromArgb(45, 52, 78),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            browseBtn.FlatAppearance.BorderSize = 0;
            browseBtn.Click += (s, e) =>
            {
                using (var fbd = new FolderBrowserDialog())
                {
                    fbd.SelectedPath = pathBox.Text;
                    if (fbd.ShowDialog() == DialogResult.OK)
                    {
                        pathBox.Text = fbd.SelectedPath;
                    }
                }
            };
            this.Controls.Add(browseBtn);

            // Options Checkboxes
            chkAutoStart = new CheckBox
            {
                Text = "Iniciar automáticamente al encender Windows (en la bandeja del reloj)",
                Checked = true,
                Location = new Point(24, 175),
                Width = 480,
                AutoSize = true,
                ForeColor = Color.FromArgb(226, 232, 240)
            };
            this.Controls.Add(chkAutoStart);

            chkDesktopShortcut = new CheckBox
            {
                Text = "Crear acceso directo en el Escritorio",
                Checked = true,
                Location = new Point(24, 205),
                Width = 480,
                AutoSize = true,
                ForeColor = Color.FromArgb(226, 232, 240)
            };
            this.Controls.Add(chkDesktopShortcut);

            chkStartMenu = new CheckBox
            {
                Text = "Crear acceso directo en el Menú Inicio",
                Checked = true,
                Location = new Point(24, 235),
                Width = 480,
                AutoSize = true,
                ForeColor = Color.FromArgb(226, 232, 240)
            };
            this.Controls.Add(chkStartMenu);

            chkLaunchNow = new CheckBox
            {
                Text = "Iniciar Remote WiFi Keyboard al finalizar la instalación",
                Checked = true,
                Location = new Point(24, 265),
                Width = 480,
                AutoSize = true,
                ForeColor = Color.FromArgb(226, 232, 240)
            };
            this.Controls.Add(chkLaunchNow);

            // Progress Bar & Status
            progressBar = new ProgressBar
            {
                Location = new Point(24, 310),
                Width = 475,
                Height = 16,
                Style = ProgressBarStyle.Continuous,
                Visible = false
            };
            this.Controls.Add(progressBar);

            statusLabel = new Label
            {
                Text = "Listo para instalar.",
                Location = new Point(24, 335),
                Width = 475,
                ForeColor = Color.FromArgb(148, 163, 184),
                Font = new Font("Segoe UI", 9f)
            };
            this.Controls.Add(statusLabel);

            // Bottom Buttons
            installBtn = new Button
            {
                Text = "Instalar Ahora 🚀",
                Location = new Point(275, 380),
                Width = 135,
                Height = 38,
                BackColor = Color.FromArgb(99, 102, 241),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10f, FontStyle.Bold),
                Cursor = Cursors.Hand
            };
            installBtn.FlatAppearance.BorderSize = 0;
            installBtn.Click += StartInstallation;
            this.Controls.Add(installBtn);

            cancelBtn = new Button
            {
                Text = "Cancelar",
                Location = new Point(420, 380),
                Width = 80,
                Height = 38,
                BackColor = Color.FromArgb(30, 35, 55),
                ForeColor = Color.FromArgb(203, 213, 225),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9.5f)
            };
            cancelBtn.FlatAppearance.BorderSize = 0;
            cancelBtn.Click += (s, e) => this.Close();
            this.Controls.Add(cancelBtn);
        }

        private void StartInstallation(object sender, EventArgs e)
        {
            installBtn.Enabled = false;
            cancelBtn.Enabled = false;
            browseBtn.Enabled = false;
            pathBox.Enabled = false;
            progressBar.Visible = true;
            progressBar.Value = 10;

            string targetDir = pathBox.Text.Trim();
            string exePath = Path.Combine(targetDir, "RemoteWiFiKeyboard.exe");
            bool autoStart = chkAutoStart.Checked;
            bool desktopShortcut = chkDesktopShortcut.Checked;
            bool startMenu = chkStartMenu.Checked;
            bool launchNow = chkLaunchNow.Checked;

            new Thread(() =>
            {
                try
                {
                    UpdateStatus("Creando directorio de destino...", 25);
                    if (!Directory.Exists(targetDir))
                    {
                        Directory.CreateDirectory(targetDir);
                    }

                    UpdateStatus("Extrayendo archivos de Remote WiFi Keyboard...", 50);
                    // Extract payload from embedded resource
                    var assembly = Assembly.GetExecutingAssembly();
                    using (var stream = assembly.GetManifestResourceStream("AppPayload.exe"))
                    {
                        if (stream != null)
                        {
                            using (var fileStream = new FileStream(exePath, FileMode.Create, FileAccess.Write))
                            {
                                stream.CopyTo(fileStream);
                            }
                        }
                    }

                    UpdateStatus("Configurando accesos directos y registro...", 75);

                    // Registry Auto-Start
                    if (autoStart)
                    {
                        using (var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", true))
                        {
                            if (key != null)
                            {
                                key.SetValue("RemoteWiFiKeyboard", "\"" + exePath + "\"");
                            }
                        }
                    }

                    // Windows Uninstall Registry Entry
                    using (var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\RemoteWiFiKeyboard"))
                    {
                        if (key != null)
                        {
                            key.SetValue("DisplayName", "Remote WiFi Keyboard");
                            key.SetValue("DisplayIcon", exePath);
                            key.SetValue("DisplayVersion", "1.0.0");
                            key.SetValue("Publisher", "Gabo Dev");
                            key.SetValue("InstallLocation", targetDir);
                            key.SetValue("UninstallString", "\"" + Path.Combine(targetDir, "uninstall.bat") + "\"");
                        }
                    }

                    // Create uninstaller script in target dir
                    string uninstallerContent = "@echo off\r\ntaskkill /F /IM RemoteWiFiKeyboard.exe >nul 2>&1\r\nreg delete \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v RemoteWiFiKeyboard /f >nul 2>&1\r\nreg delete \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\RemoteWiFiKeyboard\" /f >nul 2>&1\r\npowershell -Command \"Remove-Item '$([Environment]::GetFolderPath('Desktop'))\\Remote WiFi Keyboard.lnk' -ErrorAction SilentlyContinue; Remove-Item '$([Environment]::GetFolderPath('Programs'))\\Remote WiFi Keyboard.lnk' -ErrorAction SilentlyContinue\"\r\nrmdir /S /Q \"" + targetDir + "\" >nul 2>&1\r\nexit\r\n";
                    File.WriteAllText(Path.Combine(targetDir, "uninstall.bat"), uninstallerContent);

                    // Desktop Shortcut
                    if (desktopShortcut)
                    {
                        string desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
                        CreateShortcut(Path.Combine(desktopPath, "Remote WiFi Keyboard.lnk"), exePath, targetDir);
                    }

                    // Start Menu Shortcut
                    if (startMenu)
                    {
                        string programsPath = Environment.GetFolderPath(Environment.SpecialFolder.Programs);
                        CreateShortcut(Path.Combine(programsPath, "Remote WiFi Keyboard.lnk"), exePath, targetDir);
                    }

                    UpdateStatus("¡Instalación completada con éxito!", 100);

                    if (launchNow)
                    {
                        System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                        {
                            FileName = exePath,
                            WorkingDirectory = targetDir
                        });
                    }

                    this.Invoke((MethodInvoker)delegate
                    {
                        MessageBox.Show(this, "¡Remote WiFi Keyboard ha sido instalado correctamente!\nLa aplicación ya está activa en la bandeja del sistema (System Tray).", "Instalación Exitosa", MessageBoxButtons.OK, MessageBoxIcon.Information);
                        this.Close();
                    });
                }
                catch (Exception ex)
                {
                    this.Invoke((MethodInvoker)delegate
                    {
                        MessageBox.Show(this, "Error durante la instalación: " + ex.Message, "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        installBtn.Enabled = true;
                        cancelBtn.Enabled = true;
                    });
                }
            }).Start();
        }

        private void UpdateStatus(string message, int progress)
        {
            if (this.InvokeRequired)
            {
                this.Invoke((MethodInvoker)delegate { UpdateStatus(message, progress); });
                return;
            }
            statusLabel.Text = message;
            progressBar.Value = progress;
        }

        private void CreateShortcut(string shortcutPath, string targetPath, string workingDir)
        {
            Type shellType = Type.GetTypeFromProgID("WScript.Shell");
            if (shellType != null)
            {
                dynamic shell = Activator.CreateInstance(shellType);
                dynamic shortcut = shell.CreateShortcut(shortcutPath);
                shortcut.TargetPath = targetPath;
                shortcut.WorkingDirectory = workingDir;
                shortcut.Description = "Remote WiFi Keyboard para Windows";
                shortcut.Save();
            }
        }

        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new SetupForm());
        }
    }
}
