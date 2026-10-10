import re

file_path = "app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add AnimatedGradientView inner class right before the end of the file
inner_class = '''
    class AnimatedGradientView extends android.view.View {
        private android.graphics.Paint paint;
        private float offset = 0;
        private android.graphics.LinearGradient gradient;
        private android.graphics.Matrix matrix;

        public AnimatedGradientView(android.content.Context context) {
            super(context);
            paint = new android.graphics.Paint();
            matrix = new android.graphics.Matrix();
            int[] colors = {
                android.graphics.Color.parseColor("#ff007f"), 
                android.graphics.Color.parseColor("#7400b8"), 
                android.graphics.Color.parseColor("#00f5d4"), 
                android.graphics.Color.parseColor("#ff007f")  
            };
            gradient = new android.graphics.LinearGradient(0, 0, 1000, 1000, colors, null, android.graphics.Shader.TileMode.MIRROR);
            paint.setShader(gradient);

            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0, 2000);
            animator.setDuration(4000);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(anim -> {
                offset = (float) anim.getAnimatedValue();
                matrix.setTranslate(offset, offset);
                gradient.setLocalMatrix(matrix);
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }
}
'''
if not 'class AnimatedGradientView' in content:
    content = content.rstrip()
    if content.endswith('}'):
        content = content[:-1] + inner_class

# 2. Modify onCreateInputView
content = content.replace(
    'public View onCreateInputView() {\n        LinearLayout layout = new LinearLayout(this);',
    '''public View onCreateInputView() {
        android.widget.FrameLayout rootFrame = new android.widget.FrameLayout(this);
        AnimatedGradientView animatedBg = new AnimatedGradientView(this);
        rootFrame.addView(animatedBg, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout layout = new LinearLayout(this);
        rootFrame.addView(layout, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));'''
)
content = content.replace('layout.setBackgroundColor(Color.parseColor("#0B0D14"));', 'layout.setBackgroundColor(android.graphics.Color.TRANSPARENT);')

# Find the return layout; and replace it inside onCreateInputView
# It's right before onCreate() or onStartInput()
# A safe way is to replace `return layout;` but only the one inside onCreateInputView.
content = re.sub(
    r'(layout\.addView\(buttonRow\);\s*return )layout(;\s*\})',
    r'\1rootFrame\2',
    content
)

# 3. Modify applyTheme
apply_theme_old = '''    private void applyTheme(View rootLayout) {
        int[] theme = THEMES[currentThemeIndex];
        int bgColor = theme[0];
        int keyBgColor = theme[1];
        int textColor = theme[3];
        
        rootLayout.setBackgroundColor(bgColor);
        if (visualKeyboardContainer != null) {
            visualKeyboardContainer.setBackgroundColor(bgColor);
        }
        
        for (Button btn : keyButtons.values()) {
            btn.setBackgroundColor(keyBgColor);
            btn.setTextColor(textColor);
        }'''

apply_theme_new = '''    private void applyTheme(View rootLayout) {
        int[] theme = THEMES[currentThemeIndex];
        int textColor = theme[3];
        
        rootLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        if (visualKeyboardContainer != null) {
            visualKeyboardContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
        
        for (Button btn : keyButtons.values()) {
            btn.setBackgroundColor(android.graphics.Color.parseColor("#121212")); // Solid dark key
            btn.setTextColor(textColor);
        }'''

content = content.replace(apply_theme_old, apply_theme_new)

# 4. Modify animateKey
animate_key_old = '''        Button btn = keyButtons.get(upper);
        if (btn != null) {
            mainHandler.post(() -> {
                int[] theme = THEMES[currentThemeIndex];
                btn.setBackgroundColor(theme[2]); // Pressed color
                btn.setTextColor(theme[0]);
                mainHandler.postDelayed(() -> {
                    btn.setBackgroundColor(theme[1]); // Normal color
                    btn.setTextColor(theme[3]);
                }, 100);
            });
        }'''

animate_key_new = '''        Button btn = keyButtons.get(upper);
        if (btn != null) {
            mainHandler.post(() -> {
                int[] theme = THEMES[currentThemeIndex];
                btn.setBackgroundColor(android.graphics.Color.TRANSPARENT); // Show gradient!
                btn.setTextColor(theme[0]);
                mainHandler.postDelayed(() -> {
                    btn.setBackgroundColor(android.graphics.Color.parseColor("#121212")); // Back to dark
                    btn.setTextColor(theme[3]);
                }, 150); // Slightly longer for the visual effect
            });
        }'''
content = content.replace(animate_key_old, animate_key_new)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied successfully.")
