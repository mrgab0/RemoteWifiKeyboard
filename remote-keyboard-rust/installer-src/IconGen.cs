using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

class IconGen
{
    static void Main()
    {
        string baseDir = @"C:\Users\gabo\Documents\codigo\codespaces\remote-keyboard-rust\msix-package\Assets";
        Directory.CreateDirectory(baseDir);

        CreateIcon(Path.Combine(baseDir, "StoreLogo.png"), 50, 50);
        CreateIcon(Path.Combine(baseDir, "Square44x44Logo.png"), 44, 44);
        CreateIcon(Path.Combine(baseDir, "Square150x150Logo.png"), 150, 150);
        CreateIcon(Path.Combine(baseDir, "Wide310x150Logo.png"), 310, 150);
    }

    static void CreateIcon(string path, int w, int h)
    {
        using (Bitmap bmp = new Bitmap(w, h))
        using (Graphics g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            using (LinearGradientBrush brush = new LinearGradientBrush(
                new Point(0, 0),
                new Point(w, h),
                Color.FromArgb(99, 102, 241),
                Color.FromArgb(139, 92, 246)))
            {
                g.FillRectangle(brush, 0, 0, w, h);
            }

            float fontSize = Math.Max(8f, (float)h / 3f);
            using (Font font = new Font("Segoe UI", fontSize, FontStyle.Bold))
            using (StringFormat sf = new StringFormat
            {
                Alignment = StringAlignment.Center,
                LineAlignment = StringAlignment.Center
            })
            {
                g.DrawString("KB", font, Brushes.White, new RectangleF(0, 0, w, h), sf);
            }

            bmp.Save(path, ImageFormat.Png);
        }
    }
}
